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

package co.bywarp.stash;

import co.bywarp.lightkit.util.CollectionUtils;
import co.bywarp.lightkit.util.logger.Logger;
import co.bywarp.stash.element.ElementExpiryPolicy;
import co.bywarp.stash.memory.MemoryStash;
import co.bywarp.stash.redis.RedisConnection;
import co.bywarp.stash.redis.RedisKeyspace;
import co.bywarp.stash.redis.RedisStash;
import co.bywarp.stash.redis.RedisTypeAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
@SuppressWarnings({"SimplifyStreamApiCallChains", "rawtypes", "unchecked"})
public class StashFactory<K, T> {

    private Logger logger;
    private StashSource source;
    private ElementExpiryPolicy expiryPolicy;
    private RedisConnection redisConnection;
    private RedisKeyspace<K> redisKeyspace;
    private RedisTypeAdapter<String, T> resultSerializer;
    private RedisTypeAdapter<T, String> typeSerializer;

    public StashFactory(StashSource source) {
        this.source = source;
        this.logger = new Logger("Stash Factory");
    }

    protected StashFactory(StashSource source,
                           ElementExpiryPolicy expiryPolicy,
                           RedisConnection connection,
                           RedisKeyspace<K> keyspace,
                           RedisTypeAdapter<String, T> resultSerializer,
                           RedisTypeAdapter<T, String> typeSerializer) {
        this(source);
        this.expiryPolicy = expiryPolicy;
        this.redisConnection = connection;
        this.redisKeyspace = keyspace;
        this.resultSerializer = resultSerializer;
        this.typeSerializer = typeSerializer;
    }

    /**
     * Creates a factory instance.
     *
     * @param source the stash provider source
     * @param expiryPolicy the expiry policy for cache elements
     * @param <T> the element type
     * @param <K> the key type
     *
     * @return a partially configured {@link StashFactory}
     */
    public static <K, T> StashFactory<K, T> of(StashSource source, ElementExpiryPolicy expiryPolicy) {
        return new StashFactory<>(source, expiryPolicy, null,
                null, null, null);
    }

    /**
     * Creates a factory instance for a {@link co.bywarp.stash.memory.MemoryStash}
     * @param <T> the element type
     * @param <K> the key type
     * @param expiryPolicy the expiry policy for cache elements
     * @return a fully configured {@link StashFactory}
     */
    public static <K, T> MemoryStash<K, T> memoryStash(ElementExpiryPolicy expiryPolicy) {
        StashFactory<K, T> factory = new StashFactory<>(StashSource.MEMORY, expiryPolicy,
                null, null, null, null);
        return (MemoryStash<K, T>) factory.build();
    }

    /**
     * Creates a factory instance for a {@link co.bywarp.stash.redis.RedisStash}.
     *
     * @param <T> the element type
     * @param <K> the key type
     *
     * @param expiryPolicy the expiry policy for cache elements
     * @param connection the connection data for this stash
     * @param keyspace the keyspace configuration for this stash
     * @param resultSerializer the result serializer for this stash
     * @param typeSerializer the type serializer for this stash
     *
     * @return a fully configured {@link StashFactory}
     */
    public static <K, T> RedisStash<K, T> redisStash(ElementExpiryPolicy expiryPolicy,
                                                     RedisConnection connection,
                                                     RedisKeyspace<K> keyspace,
                                                     RedisTypeAdapter<String, T> resultSerializer,
                                                     RedisTypeAdapter<T, String> typeSerializer) {
        StashFactory<K, T> factory = new StashFactory<>(StashSource.REDIS,
                expiryPolicy,
                connection,
                keyspace,
                resultSerializer,
                typeSerializer);
        return (RedisStash<K, T>) factory.build();
    }

    /**
     * Assigns the element expiry policy for a {@link StashProvider}.
     * @param expiryPolicy the expiry policy to follow
     * @return this factory instance
     */
    public StashFactory<K, T> withExpiryPolicy(ElementExpiryPolicy expiryPolicy) {
        this.expiryPolicy = expiryPolicy;
        return this;
    }

    /**
     * Assigns the connection data for a {@link co.bywarp.stash.redis.RedisStash}.
     * @param connection the connection properties
     * @throws UnsupportedOperationException thrown if current builder is not a Redis builder.
     * @return this factory instance
     */
    public StashFactory<K, T> withRedisConnection(RedisConnection connection) {
        if (source != StashSource.REDIS) {
            throw new UnsupportedOperationException("Cannot assign type serializer to Non-Redis stash");
        }

        this.redisConnection = connection;
        return this;
    }

    /**
     * Assigns the keyspace configuration for a {@link co.bywarp.stash.redis.RedisStash}.
     * @param keyspace the Redis keyspace configuration
     * @throws UnsupportedOperationException thrown if current builder is not a Redis builder.
     * @return this factory instance
     */
    public StashFactory<K, T> withKeyspace(RedisKeyspace<K> keyspace) {
        if (source != StashSource.REDIS) {
            throw new UnsupportedOperationException("Cannot assign type serializer to Non-Redis stash");
        }

        this.redisKeyspace = keyspace;
        return this;
    }

    /**
     * Assigns the result serializer for a {@link co.bywarp.stash.redis.RedisStash}.
     * @param resultSerializer the result serializer
     * @throws UnsupportedOperationException thrown if current builder is not a Redis builder.
     * @return this factory instance
     */
    public StashFactory<K, T> withResultSerializer(RedisTypeAdapter<String, T> resultSerializer) {
        if (source != StashSource.REDIS) {
            throw new UnsupportedOperationException("Cannot assign type serializer to Non-Redis stash");
        }

        this.resultSerializer = resultSerializer;
        return this;
    }

    /**
     * Assigns the type serializer for a {@link co.bywarp.stash.redis.RedisStash}.
     * @param typeSerializer the type serializer
     * @throws UnsupportedOperationException thrown if current builder is not a Redis builder.
     * @return this factory instance
     */
    public StashFactory<K, T> withTypeSerializer(RedisTypeAdapter<T, String> typeSerializer) {
        if (source != StashSource.REDIS) {
            throw new UnsupportedOperationException("Cannot assign type serializer to Non-Redis stash");
        }

        this.typeSerializer = typeSerializer;
        return this;
    }

    /**
     * Builds a {@link StashProvider} from all of the provided data sources, serializers, and options.
     * @return the completed {@link StashProvider}
     */
    public StashProvider<K, T> build() {
        return this.reflectivelyInitialize(source);
    }

    /**
     * Reflectively initializes a {@link StashProvider} from a {@link StashSource}.
     * @param source the provided {@link StashSource}
     * @return the {@link StashProvider}
     */
    private StashProvider<K, T> reflectivelyInitialize(StashSource source) {
        Class<? extends StashProvider> genericProvider = source.getProvider();
        try {
            Object[] constructorValues = getConstructorValues();
            return genericProvider
                    .getConstructor(source.getConstructorTypes())
                    .newInstance(constructorValues);
        } catch (InstantiationException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | InvocationTargetException e) {
            logger.except(e, "Failed to initialize StashProvider");
            return null;
        }
    }

    /**
     * Retrieves constructor values for the current {@link StashSource}.
     * @return a list of constructor values
     */
    private Object[] getConstructorValues() {
        List<Object> objects = CollectionUtils.collect(expiryPolicy);
        if (source == StashSource.MEMORY) {
            return quickConvertList(objects);
        }

        List<Object> redisComponents = CollectionUtils.collect(
                redisConnection,
                redisKeyspace,
                resultSerializer,
                typeSerializer);
        objects.addAll(redisComponents);
        return quickConvertList(objects);
    }

    private Object[] quickConvertList(List<?> list) {
        return list.stream().toArray(Object[]::new);
    }

}
