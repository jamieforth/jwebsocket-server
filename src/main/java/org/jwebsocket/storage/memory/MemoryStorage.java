//  ---------------------------------------------------------------------------
//  jWebSocket - MemoryStorage
//  Copyright (c) 2011 Innotrade GmbH, jWebSocket.org
//  ---------------------------------------------------------------------------
//  This program is free software; you can redistribute it and/or modify it
//  under the terms of the GNU Lesser General Public License as published by the
//  Free Software Foundation; either version 3 of the License, or (at your
//  option) any later version.
//  This program is distributed in the hope that it will be useful, but WITHOUT
//  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
//  more details.
//  You should have received a copy of the GNU Lesser General Public License along
//  with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//  ---------------------------------------------------------------------------
package org.jwebsocket.storage.memory;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javolution.util.FastMap;
import org.jwebsocket.api.IBasicStorage;

/**
 *
 * @param <K> 
 * @param <V> 
 * @author rbetancourt
 */
public class MemoryStorage<K, V> implements IBasicStorage<K, V> {

    private static FastMap<String, FastMap> mContainer = new FastMap<String, FastMap>();
    private String mName;
    private FastMap mMap;

    /**
     * Create a new MemoryStorage instance
     * @param aName The name of the storage container
     * */
    public MemoryStorage(String aName) {
        this.mName = aName;
    }

    /**
     * 
     * @return
     */
    public static FastMap<String, FastMap> getContainer() {
        return mContainer;
    }

    /**
     * 
     * @return
     */
    @Override
    public synchronized String getName() {
        return mName;
    }

    /**
     * {@inheritDoc
     * 
     * @param aNewName 
     * @throws Exception 
     */
    @Override
    public synchronized void setName(String aNewName) throws Exception {
        if (getContainer().containsKey(mName)) {
            FastMap lValue = getContainer().remove(mName);
            if (mMap != null) {
                getContainer().put(aNewName, mMap);
            } else {
                getContainer().put(aNewName, lValue);
            }
        }

        this.mName = aNewName;
    }

    /**
     * {@inheritDoc
     * 
     * @param aKeys
     * @return  
     */
    @Override
    public Map<K, V> getAll(Collection<K> aKeys) {
        FastMap<K, V> lMap = new FastMap<K, V>();
        for (K lKey : aKeys) {
            lMap.put((K) lKey, get((K) lKey));
        }

        return lMap;
    }

    /**
     * {@inheritDoc
     * 
     * @throws Exception 
     */
    @Override
    public void initialize() throws Exception {
        if (!getContainer().containsKey(mName) || null == getContainer().get(mName)) {
            getContainer().put(mName, new FastMap<K, V>());
        }

        mMap = getContainer().get(mName);
    }

    /**
     * {@inheritDoc
     * 
     * @throws Exception 
     */
    @Override
    public void shutdown() throws Exception {
    }

    /**
     * {@inheritDoc
     * 
     * @return 
     */
    @Override
    public int size() {
        return mMap.size();
    }

    /**
     * {@inheritDoc
     * 
     * @return 
     */
    @Override
    public boolean isEmpty() {
        if (mMap.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc
     * 
     * @param key 
     * @return 
     */
    @Override
    public boolean containsKey(Object aKey) {
        if (mMap.containsKey((String) aKey)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc
     * 
     * @param aValue 
     * @return 
     */
    @Override
    public boolean containsValue(Object aValue) {
        if (mMap.containsValue(aValue)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc
     * 
     * @param lKey 
     * @return 
     */
    @Override
    public V get(Object lKey) {
        return (V) mMap.get(lKey);
    }

    /**
     * {@inheritDoc
     * 
     * @param lKey 
     * @param lValue 
     * @return 
     */
    @Override
    public V put(K lKey, V lValue) {
        return (V) mMap.put(lKey, lValue);
    }

    /**
     * {@inheritDoc
     * 
     * @param aKey 
     * @return 
     */
    @Override
    public V remove(Object aKey) {
        return (V) mMap.remove(aKey);
    }

    /**
     * {@inheritDoc
     * 
     * @param aMap 
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> aMap) {
        mMap.putAll(aMap);
    }

    /**
     * {@inheritDoc
     */
    @Override
    public void clear() {
        mMap.clear();
    }

    /**
     * {@inheritDoc
     * 
     * @return 
     */
    @Override
    public Set<K> keySet() {
        return mMap.keySet();
    }

    /**
     * {@inheritDoc
     * 
     * @return 
     */
    @Override
    public Collection<V> values() {
        return mMap.values();
    }

    /**
     * {@inheritDoc
     * 
     * @return 
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return mMap.entrySet();
    }
}