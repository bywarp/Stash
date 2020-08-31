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

import co.bywarp.lightkit.util.Closable;

import java.util.Map;
import java.util.function.BiPredicate;

public interface StashProvider<K, T> extends Closable {

    /**
     * Retrieves a cached item of type T
     * using the provided key, of type K.
     *
     * @param key the key mapped to the cached object.
     * @return the cached object
     */
    T retrieve(K key);

    /**
     * Retrieves a cached item of type T using the provided key, of type K.
     * If the retrieved item could not be found, an alternative item, also of
     * type T, will be returned in it's place.
     *
     * @param key the key mapped to the cached object.
     * @param orElse the value to return if the cached element does not exist.
     * @return the cached object, or if it does not exist, an alternate object.
     */
    T retrieveOrElse(K key, T orElse);

    /**
     * Retrieves a cached item of type T using the provided key, of type K.
     * If the retrieved item could not be found, the second parameter, also of type T
     * will be assigned to the specified key instead.
     *
     * @param key the key mapped to the cached object.
     * @param newValue the value to store if the element is not present.
     * @return the cached object, or if it does not exist, the alternate object.
     */
    T retrieveOrSet(K key, T newValue);

    /**
     * Retrieves all cached elements.
     * @return all cached elements
     */
    Map<K, T> retrieveAll();

    /**
     * Stores (and updates if found) an object of type T
     * using the provided key of type K.
     *
     * @param key the key used to retrieve the object
     * @param element the object to cache
     * @return the cached object
     */
    T store(K key, T element);

    /**
     * Updates a cached value (if found) in the Stash.
     *
     * @param key the key to update
     * @param element the new value to cache under the key
     * @throws NullPointerException thrown if no element exists for the provided key
     * @return the cached object
     */
    T update(K key, T element) throws NullPointerException;

    /**
     * Evicts a cached item by it's key.
     * @param key the key to evict
     * @return if the element was successfully evicted
     */
    boolean evict(K key);

    /**
     * Evicts a cached item by it's key, and expected value.
     *
     * @apiNote when implementing {@link StashProvider#evict(K key, T element)}
     * ensure that the retrieved element matches the expected element
     * which is supplied as a parameter to this method.
     *
     * @param key the key to evict
     * @param element the item that is expected to be there
     * @return if the element was successfully evicted
     */
    boolean evict(K key, T element);

    /**
     * Evicts all elements that match the specified predicate.
     * @param predicate the condition to meet for eviction
     */
    void evictIf(BiPredicate<K, T> predicate);

    /**
     * Evicts all cached elements.
     */
    void evictAll();

    /**
     * Returns whether or not the specified key refers to an element that is present in the cache.
     * @param key the provided key
     * @return if an element with such key is present in the cache
     */
    boolean contains(K key);

}
