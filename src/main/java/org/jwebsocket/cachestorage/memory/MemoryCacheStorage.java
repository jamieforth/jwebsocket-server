//    ---------------------------------------------------------------------------
//    jWebSocket - MemoryCacheStorage
//  Copyright (c) 2010 jwebsocket.org
//    ---------------------------------------------------------------------------
//    This program is free software; you can redistribute it and/or modify it
//    under the terms of the GNU Lesser General Public License as published by the
//    Free Software Foundation; either version 3 of the License, or (at your
//    option) any later version.
//    This program is distributed in the hope that it will be useful, but WITHOUT
//    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//    FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
//    more details.
//    You should have received a copy of the GNU Lesser General Public License along
//    with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//    ---------------------------------------------------------------------------
package org.jwebsocket.cachestorage.memory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.jwebsocket.api.IBasicCacheStorage;

/**
 *
 * @author kyberneees
 */
public class MemoryCacheStorage<K, V> implements IBasicCacheStorage<K, V> {

    class Element<V> {

        private int mExpTime;
        private Long mInsertionTime;
        private V mValue;

        public Element(V value, Long insertionTime, int expTime) {
            this.mExpTime = expTime;
            this.mValue = value;
            this.mInsertionTime = insertionTime;
        }

        public int getExpTime() {
            return mExpTime;
        }

        public void setExpTime(int expTime) {
            this.mExpTime = expTime;
        }

        public V getValue() {
            return mValue;
        }

        public void setValue(V value) {
            this.mValue = value;
        }

        public Long getInsertionTime() {
            return mInsertionTime;
        }

        public void setInsertionTime(Long insertionTime) {
            this.mInsertionTime = insertionTime;
        }
    }
    private static FastMap<String, FastMap> mContainer = new FastMap<String, FastMap>();
    private String mName;
    private FastMap<K, Element<V>> mMap;

    /**
     * Create a new MemoryStorage instance
     * @param aName The name of the storage container
     * */
    public MemoryCacheStorage(String aName) {
        this.mName = aName;

    }

    public static FastMap<String, FastMap> getContainer() {
        return mContainer;
    }

    @Override
    public synchronized String getName() {
        return mName;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public synchronized void setName(String aNewName) throws Exception {
        if (getContainer().containsKey(mName)) {
            FastMap value = getContainer().remove(mName);
            if (mMap != null) {
                getContainer().put(aNewName, mMap);
            } else {
                getContainer().put(aNewName, value);
            }
        }

        this.mName = aNewName;
    }

    /**
     * {@inheritDoc
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
     */
    @Override
    public void initialize() throws Exception {
        if (!getContainer().containsKey(mName) || null == getContainer().get(mName)) {
            getContainer().put(mName, new FastMap<K, Element<V>>());
        }

        mMap = getContainer().get(mName);
    }

    /**
     * {@inheritDoc
     */
    @Override
    public void shutdown() throws Exception {
    }

    /**
     * {@inheritDoc
     */
    @Override
    public int size() {
        return mMap.size();
    }

    /**
     * {@inheritDoc
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
     */
    @Override
    public boolean containsKey(Object aKey) {
        if (mMap.containsKey((String) aKey)) {
            if (isValid((K)aKey, mMap.get(aKey))){
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param r The Element object
     * @return TRUE if the element is not expired, FALSE otherwise
     */
    private boolean isValid(K aKey, Element<V> aElement) {
        Integer lExpTime = aElement.getExpTime();
        if (lExpTime < 1) {
            return true;
        }

        if (aElement.getInsertionTime() + lExpTime >= System.currentTimeMillis() / 1000) {
            return true;
        }
        //Useful to keep the collection up to date with only non-expired values
        mMap.remove(aKey);

        return false;
    }
    
    /**
     * {@inheritDoc
     */
    @Override
    public boolean containsValue(Object aValue) {
        Iterator<K> lKeys = mMap.keySet().iterator();
        while (lKeys.hasNext()){
            K lKey = lKeys.next();
            Element<V> lElement = mMap.get(lKey);
            if (lElement.getValue().equals(aValue) && isValid((K)lKey, lElement)){
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public V get(Object aKey) {
        Element<V> lElement = mMap.get(aKey);
        if (lElement != null && isValid((K)aKey, lElement)){
            return lElement.getValue();
        }
        
        return null;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public V put(K aKey, V aValue) {
        return put(aKey, aValue, 0);
    }

    /**
     * {@inheritDoc
     */
    @Override
    public V put(K aKey, V aValue, int aExpTime) {
        Element<V> lElement = new Element<V>(aValue, (Long) (System.currentTimeMillis() / 1000), aExpTime);
        mMap.put(aKey, lElement);
        
        return lElement.getValue();
    }

    /**
     * {@inheritDoc
     * 
     * @param key
     * @return  
     */
    @Override
    public V remove(Object aKey) {
        return (V) mMap.remove(aKey).getValue();
    }

    /**
     * {@inheritDoc
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> aMap) {
        Iterator keys = aMap.keySet().iterator();
        while (keys.hasNext()){
            K key = (K)keys.next();
            put(key, aMap.get(key));
        }
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
     */
    @Override
    public Set<K> keySet() {
        Set<K> lKeyset = new FastSet<K>();
        Iterator<K> lKeys = mMap.keySet().iterator();
        while (lKeys.hasNext()){
            K lKey = lKeys.next();
            Element<V> lElement = mMap.get(lKey);
            if (isValid(lKey, lElement)){
                lKeyset.add(lKey);
            }
        }
        
        return lKeyset;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Collection<V> values() {
        Set<V> lValues = new FastSet<V>();
        Set<K> lKeys = keySet();
        
        if (!lKeys.isEmpty())
        for (K k: lKeys){
            lValues.add(mMap.get(k).getValue());
        }
        
        return lValues;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return getAll(keySet()).entrySet();
    }
}
