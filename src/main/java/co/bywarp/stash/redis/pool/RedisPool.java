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

package co.bywarp.stash.redis.pool;

import co.bywarp.lightkit.util.Closable;
import co.bywarp.stash.redis.RedisConnection;
import co.bywarp.stash.redis.RedisKeyspace;
import co.bywarp.stash.redis.RedisStash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

public class RedisPool<K, T> implements Closable {

    private RedisStash<K, T> host;
    private RedisConnection connection;
    private RedisKeyspace<K> keyspace;
    private JedisPool pool;

    private List<RedisPoolResource> resources;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> resourceAuditor;

    public RedisPool(RedisStash<K, T> host) {
        this.host = host;
        this.connection = host.getConnection();
        this.keyspace = host.getKeyspace();
        this.resources = Collections.synchronizedList(new ArrayList<>());

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(connection.getPoolSize());
        config.setMaxWaitMillis(connection.getTimeoutMillis());
        config.setTestOnBorrow(connection.isTestOnBorrow());
        config.setBlockWhenExhausted(connection.isBlockWhenExhausted());

        this.pool = new JedisPool(config,
                connection.getHost(),
                connection.getPort(),
                connection.getConnectTimeout(),
                connection.isAuth() ? connection.getPassword() : null);

        this.executorService = Executors.newScheduledThreadPool(4);
        this.resourceAuditor = executorService.scheduleAtFixedRate(() -> {
            // Remove all dead resources
            resources.removeIf(RedisPoolResource::isDead);

            // Find all resources that have not been closed for 20+ seconds
            List<RedisPoolResource> stale = resources
                    .stream()
                    .filter(resource -> System.currentTimeMillis() > resource.getTimeout())
                    .collect(Collectors.toList());

            // Return all stale resources to pool, remove them from managed list, and clear stale list
            stale.forEach(resource -> resource.getResource().close());
            stale.forEach(resources::remove);
            stale.clear();
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    /**
     * Borrows a timed Jedis resource from the pool.
     *
     * @throws JedisException thrown if any of the following
     * conditions are met:
     * <ul>
     *     <li>The {@link JedisPool} is dead</li>
     *     <li>The {@link JedisPool} has exhausted all of it's allotted resources</li>
     *     <li>The returned {@link Jedis} resource from the pool is null</li>
     *     <li>The returned {@link Jedis} resource from the pool is dead (not connected)</li>
     * </ul>
     *
     * @return the borrowed {@link Jedis} object.
     */
    public Jedis borrow() throws JedisException {
        if (pool.isClosed()) {
            throw new JedisException("Dead Jedis Pool");
        }

        if (pool.getNumActive() > connection.getPoolSize()) {
            throw new JedisException("Pool has no available resources at this time");
        }

        Jedis jedis = pool.getResource();
        if (jedis == null) {
            throw new JedisException("Failed to get resource from pool (Is it exhausted?)");
        }

        if (!jedis.isConnected()) {
            jedis.close();
            throw new JedisException("Pool resource is not connected.");
        }

        resources.add(RedisPoolResource.of(jedis, connection.getResourceTimeout()));
        return jedis;
    }

    @Override
    public void close() {
        this.resourceAuditor.cancel(true);
        this.executorService.shutdown();
        this.resources
                .stream()
                .filter(resource -> !resource.isDead())
                .map(RedisPoolResource::getResource)
                .forEach(Jedis::close);

        this.pool.close();
    }

}
