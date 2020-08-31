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

import java.util.function.Function;

import lombok.Getter;

@Getter
public class RedisKeyspace<K> {

    private String prefix;
    private String delimiter;
    private RedisConnection connection;
    private Function<K, String> deserializer;
    private Function<String, K> serializer;

    public RedisKeyspace(String prefix, RedisConnection connection, Function<K, String> deserializer, Function<String, K> serializer) {
        this.prefix = prefix;
        this.delimiter = connection.getDelimiter();
        this.connection = connection;
        this.deserializer = deserializer;
        this.serializer = serializer;
    }

    /**
     * Creates a keyspace path for the provided key
     * @param key the key
     * @return the keyspace path
     */
    public String construct(K key) {
        return prefix + connection.getDelimiter() + deserializer.apply(key);
    }

    /**
     * Creates a key object of type K from a remote Redis key.
     * @param head the provided key string
     * @return the object key
     */
    public K fromRemote(String head) {
        return serializer
                .apply(head
                .split(prefix + connection.getDelimiter())[1]);
    }

    /**
     * Creates a string for use in {@link redis.clients.jedis.Jedis#keys(String)} to select all keys in this keyspace.
     * @return the wildcard selector string
     */
    public String selectAll() {
        return prefix + connection.getDelimiter() + "*";
    }

}
