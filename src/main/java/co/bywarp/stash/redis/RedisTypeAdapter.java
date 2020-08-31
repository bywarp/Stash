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

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RedisTypeAdapter<Provided, Required> {

    private Function<Provided, Required> converter;

    public Required adapt(Provided provided) {
        return converter.apply(provided);
    }

    public static <P, R> R adapt(P provided, Function<P, R> converter) {
        return converter.apply(provided);
    }

}
