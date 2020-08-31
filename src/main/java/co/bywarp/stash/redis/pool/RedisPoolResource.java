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

import lombok.AllArgsConstructor;
import lombok.Getter;
import redis.clients.jedis.Jedis;

@Getter
@AllArgsConstructor
public class RedisPoolResource {

    private Jedis resource;
    private long timeout;

    public static RedisPoolResource of(Jedis resource, long timeout) {
        return new RedisPoolResource(resource, System.currentTimeMillis() + timeout);
    }

    /**
     * Returns whether or not this {@link RedisPoolResource} is dead.
     *
     * A dead resource either has a null {@link Jedis} resource field,
     * or a closed {@link Jedis} resource, which results in {@link Jedis#isConnected()} being false.
     *
     * @return if this resource is dead.
     */
    public boolean isDead() {
        return resource == null || !resource.isConnected();
    }

}
