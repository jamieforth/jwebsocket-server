//  ---------------------------------------------------------------------------
//  jWebSocket - EhCacheStorage 
//  Copyright (c) 2010 Innotrade GmbH, jWebSocket.org
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
package org.jwebsocket.storage.ehcache;

import java.util.Set;
import javolution.util.FastSet;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.jwebsocket.storage.BaseStorage;

/**
 * a named storage (a map of key/value pairs) in EhCache. Please consider 
 * that each storage is maintained in its own file on the hard disk.
 * @author aschulze
 */
public class EhCacheStorage<K, V> extends BaseStorage<K, V> {

    private String mName = null;
    private static CacheManager mCacheManager = null;
    private Cache mCache = null;

        public Cache getCache() {
                return mCache;
        }

        public void setCache(Cache aCache) {
                this.mCache = aCache;
        }
        
    /**
     * 
     * @param aName
     */
    public EhCacheStorage(String aName) {
        mName = aName;
        initialize();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void setName(String aName) throws Exception {
        mName = aName;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public Set keySet() {
        Set lKeys = new FastSet();
        lKeys.addAll(mCache.getKeys());
        return lKeys;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public int size() {
        return mCache.getSize();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public V get(Object aKey) {
        Element lElement = mCache.get(aKey);
        return (lElement != null ? (V)lElement.getObjectValue() : null);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public V remove(Object aKey) {
        // TODO: The interface specs that a previous object is supposed to be returned
        // this may not be desired and reduce performance, provide second message
        V lRes = (V)mCache.get(aKey);
        mCache.remove(aKey);
        return lRes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        mCache.removeAll();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public Object put(Object aKey, Object aData) {
        Element lElement = new Element(aKey, aData);
        mCache.put(lElement);
                
        return aData;
    }

    /**
         *
         * {@inheritDoc }
         */
    @Override
    public boolean containsKey(Object aKey) {
        return mCache.get(aKey) != null;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void initialize() {
        mCacheManager = EhCacheManager.getInstance();
        if (mCacheManager != null) {
            // TODO: think about how to configure or pass settings to this cache.
            if (!mCacheManager.cacheExists(mName)) {
                mCacheManager.addCache(mName);
            }
            mCache = mCacheManager.getCache(mName);
        }
    }
}
