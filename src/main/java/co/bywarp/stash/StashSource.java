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

import co.bywarp.stash.element.ElementExpiryPolicy;
import co.bywarp.stash.memory.MemoryStash;
import co.bywarp.stash.redis.RedisConnection;
import co.bywarp.stash.redis.RedisKeyspace;
import co.bywarp.stash.redis.RedisStash;
import co.bywarp.stash.redis.RedisTypeAdapter;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PROTECTED)
@SuppressWarnings("rawtypes")
public enum StashSource {

    MEMORY(MemoryStash.class, ElementExpiryPolicy.class),
    REDIS(RedisStash.class, ElementExpiryPolicy.class, RedisConnection.class, RedisKeyspace.class, RedisTypeAdapter.class, RedisTypeAdapter.class);

    private final Class<? extends StashProvider> provider;
    private final Class<?>[] constructorTypes;

    StashSource(Class<? extends StashProvider> provider, Class<?>... constructorTypes) {
        this.provider = provider;
        this.constructorTypes = constructorTypes;
    }

}
