/*
 * Copyright (c) 2020 Warp Studios
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.bywarp.stash.redis;

import co.bywarp.lightkit.util.logger.Logger;
import co.bywarp.stash.StashProvider;
import co.bywarp.stash.element.ElementExpiryPolicy;
import co.bywarp.stash.redis.pool.RedisPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

@Getter
public class RedisStash<K, T> implements StashProvider<K, T> {

    private Logger logger;
    private ElementExpiryPolicy expiryPolicy;
    private RedisConnection connection;
    private RedisKeyspace<K> keyspace;
    private RedisTypeAdapter<String, T> resultSerializer;
    private RedisTypeAdapter<T, String> typeSerializer;
    private RedisPool<K, T> pool;

    public RedisStash(ElementExpiryPolicy expiryPolicy,
                      RedisConnection connection,
                      RedisKeyspace<K> keyspace,
                      RedisTypeAdapter<String, T> resultSerializer,
                      RedisTypeAdapter<T, String> typeSerializer) {
        this.logger = new Logger("Redis Stash " + UUID.randomUUID().toString().substring(0, 7));
        this.expiryPolicy = expiryPolicy;
        this.connection = connection;
        this.keyspace = keyspace;
        this.resultSerializer = resultSerializer;
        this.typeSerializer = typeSerializer;
        this.pool = new RedisPool<>(this);
    }

    @Override
    public T retrieve(K key) throws JedisException {
        Jedis resource = pool.borrow();
        String result = resource.get(keyspace.construct(key));

        resource.close();

        if (result == null || result.isEmpty()) {
            return null;
        }

        return resultSerializer.adapt(result);
    }

    @Override
    public T retrieveOrElse(K key, T orElse) throws JedisException {
        T result = retrieve(key);
        if (result == null) {
            return orElse;
        }

        return result;
    }

    @Override
    public T retrieveOrSet(K key, T newValue) throws JedisException {
        Jedis resource = pool.borrow();
        String result = resource.get(keyspace.construct(key));
        if (result == null || result.isEmpty()) {
            String fullKey = keyspace.construct(key);
            resource.set(fullKey, typeSerializer.adapt(newValue));
            resource.close();
            return newValue;
        }

        return resultSerializer.adapt(result);
    }

    @Override
    public Map<K, T> retrieveAll() throws JedisException {
        Jedis resource = pool.borrow();
        String selector = keyspace.selectAll();
        Set<String> keys = resource.keys(selector);
        if (keys == null || keys.isEmpty()) {
            resource.close();
            return new HashMap<>();
        }

        Map<K, T> ret = CompletableFuture
                .supplyAsync(() -> {
                    Map<K, T> results = new HashMap<>();
                    keys.forEach(key -> {
                        String result = resource.get(key);
                        if (result == null || result.isEmpty()) {
                            return;
                        }

                        results.put(keyspace.fromRemote(key),
                                resultSerializer.adapt(result));
                    });

                    return results;
                })
                .exceptionally(throwable -> {
                    logger.except(new Exception(throwable), "Exception retrieving records");
                    return new HashMap<>();
                })
                .join();

        resource.close();
        return ret;
    }

    @Override
    public T store(K key, T element) throws JedisException {
        Jedis resource = pool.borrow();
        String head = keyspace.construct(key);
        resource.set(head, typeSerializer.adapt(element));
        resource.expire(head, expiryPolicy.toSeconds());
        resource.close();

        return element;
    }

    @Override
    public T update(K key, T element) throws JedisException, NullPointerException {
        Jedis resource = pool.borrow();
        String head = keyspace.construct(key);
        if (!resource.exists(head)) {
            throw new NullPointerException("No element for key \"" + head + "\"");
        }

        resource.set(head, typeSerializer.adapt(element));
        resource.expire(head, expiryPolicy.toSeconds());
        resource.close();
        return element;
    }

    @Override
    public boolean evict(K key) throws JedisException {
        Jedis resource = pool.borrow();
        String head = keyspace.construct(key);
        if (!resource.exists(head)) {
            resource.close();
            return false;
        }

        long response = resource.del(head);
        resource.close();
        return response > 0;
    }

    @Override
    public boolean evict(K key, T element) throws JedisException {
        Jedis resource = pool.borrow();
        String head = keyspace.construct(key);
        if (!resource.exists(head)) {
            resource.close();
            return false;
        }

        String result = resource.get(head);
        if (result == null || result.isEmpty()) {
            resource.close();
            return false;
        }

        T remote = resultSerializer.adapt(result);
        if (remote != element) {
            resource.close();
            return false;
        }

        resource.del(head);
        resource.close();
        return true;
    }

    @Override
    public void evictIf(BiPredicate<K, T> predicate) throws JedisException {
        Map<K, T> all = retrieveAll();
        if (all.isEmpty()) {
            return;
        }

        List<Map.Entry<K, T>> evict = all
                .entrySet()
                .stream()
                .filter(ent -> predicate.test(
                            ent.getKey(),
                            ent.getValue()))
                .collect(Collectors.toList());
        if (evict.isEmpty()) {
            return;
        }

        Jedis resource = pool.borrow();
        Pipeline pipeline = resource.pipelined();
        evict.forEach(element -> pipeline.del(keyspace.construct(element.getKey())));
        pipeline.sync();
        resource.close();
    }

    @Override
    public void evictAll() throws JedisException {
        this.evictIf((k, t) -> true);
    }

    @Override
    public boolean contains(K key) throws JedisException {
        Jedis resource = pool.borrow();
        String head = keyspace.construct(key);
        boolean exists = resource.exists(head);

        resource.close();
        return exists;
    }

    @Override
    public void close() {
        this.pool.close();
    }

}
