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

package co.bywarp.stash.memory;

import co.bywarp.lightkit.util.CollectionUtils;
import co.bywarp.stash.StashProvider;
import co.bywarp.stash.element.ElementExpiryPolicy;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class MemoryStash<K, T> implements StashProvider<K, T> {

    private Map<K, T> cache;

    public MemoryStash(ElementExpiryPolicy expiryPolicy) {
        this.cache = Collections.synchronizedMap(new PassiveExpiringMap<>(
                expiryPolicy.getAmount(), expiryPolicy.getUnit()
        ));
    }

    @Override
    public T retrieve(K key) {
        return cache.get(key);
    }

    @Override
    public T retrieveOrElse(K key, T orElse) {
        return cache.getOrDefault(key, orElse);
    }

    @Override
    public T retrieveOrSet(K key, T newValue) {
        T local = cache.get(key);
        if (local == null) {
            return cache.put(key, newValue);
        }

        return local;
    }

    @Override
    public Map<K, T> retrieveAll() {
        return new HashMap<>(cache);
    }

    @Override
    public T store(K key, T element) {
        return cache.put(key, element);
    }

    @Override
    public T update(K key, T element) throws NullPointerException {
        if (!cache.containsKey(key)) {
            throw new NullPointerException("No element for key \"" + key.toString() + "\"");
        }

        return cache.replace(key, element);
    }

    @Override
    public boolean evict(K key) {
        return cache.remove(key) != null;
    }

    @Override
    public boolean evict(K key, T element) {
        return cache.remove(key, element);
    }

    @Override
    public void evictIf(BiPredicate<K, T> predicate) {
        CollectionUtils.removeIf(cache, predicate);
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    @Override
    public boolean contains(K key) {
        return cache.containsKey(key);
    }

    @Override
    public void close() {
        this.cache.clear();
        this.cache = null;
    }

}
