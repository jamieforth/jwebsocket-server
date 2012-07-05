//  ---------------------------------------------------------------------------
//  jWebSocket - MemcachedStorage
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
package org.jwebsocket.storage.memcached;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javolution.util.FastSet;
import net.spy.memcached.MemcachedClient;
import org.jwebsocket.storage.BaseStorage;

/**
 * 
 * @param <K> 
 * @param <V> 
 * @author kyberneees
 */
public class MemcachedStorage<K, V> extends BaseStorage<K, V> {

        private MemcachedClient mMemcachedClient;
        private String mName;
        private final static String KEYS_LOCATION = ".KEYS::1234567890";
        private final static String KEY_SEPARATOR = "::-::";
        private final static int NOT_EXPIRE = 0;

        /**
         * 
         * @param aName
         * @param aMemcachedClient
         */
        public MemcachedStorage(String aName, MemcachedClient aMemcachedClient) {
                this.mName = aName;
                this.mMemcachedClient = aMemcachedClient;
        }

        /**
         * 
         * {@inheritDoc }
         */
        @Override
        public void initialize() throws Exception {
                //Key index support
                if (null == get(mName + KEYS_LOCATION)) {
                        mMemcachedClient.set(mName + KEYS_LOCATION, NOT_EXPIRE, "");
                }
        }

        /**
         * 
         * {@inheritDoc }
         */
        @SuppressWarnings("unchecked")
        @Override
        public void clear() {
                super.clear();
                
                //Removing the index
                mMemcachedClient.set(mName + KEYS_LOCATION, NOT_EXPIRE, "");
        }

        /**
         * 
         * {@inheritDoc }
         */
        @SuppressWarnings("unchecked")
        @Override
        public Set<K> keySet() {
                String lIndex = (String) get(mName + KEYS_LOCATION);
                if (lIndex.length() == 0) {
                        return new FastSet<K>();
                } else {
                        String[] lKeys = lIndex.split(KEY_SEPARATOR);
                        FastSet lKeySet = new FastSet();
                        lKeySet.addAll(Arrays.asList(lKeys));

                        return lKeySet;
                }
        }

        /**
         * 
         * {@inheritDoc }
         */
        @SuppressWarnings("unchecked")
        @Override
        public V get(Object lKey) {
                V lValue;
                lValue = (V) mMemcachedClient.get(lKey.toString());

                return lValue;
        }

        /**
         * 
         * {@inheritDoc }
         */
        @Override
        public V remove(Object lKey) {
                V lValue = get(lKey);
                mMemcachedClient.delete(lKey.toString());

                //Key index update
                String lIndex = (String) get(mName + KEYS_LOCATION);
                lIndex = lIndex.replace(lKey.toString() + KEY_SEPARATOR, "");
                mMemcachedClient.set(mName + KEYS_LOCATION, NOT_EXPIRE, lIndex);

                return lValue;
        }

        /**
         * 
         * {@inheritDoc }
         */
        @Override
        public V put(K aKey, V aValue) {
                mMemcachedClient.set(aKey.toString(), NOT_EXPIRE, aValue);

                //Key index update
                if (!keySet().contains(aKey)) {
                        String lIndex = (String) get(mName + KEYS_LOCATION);
                        lIndex = lIndex + aKey.toString() + KEY_SEPARATOR;
                        mMemcachedClient.set(mName + KEYS_LOCATION, NOT_EXPIRE, lIndex);
                }

                return aValue;
        }

        /**
         * 
         * @return
         */
        public MemcachedClient getMemcachedClient() {
                return mMemcachedClient;
        }

        /**
         * 
         * @param aMemcachedClient
         */
        public void setMemcachedClient(MemcachedClient aMemcachedClient) {
                this.mMemcachedClient = aMemcachedClient;
        }

        /**
         * 
         * {@inheritDoc }
         */
        @Override
        public String getName() {
                return mName;
        }

        /**
         * 
         * {@inheritDoc }
         */
        @Override
        public void setName(String aName) throws Exception {
                if (aName.length() == 0) {
                        throw new InvalidParameterException();
                }
                Map<K, V> lMap = getAll(keySet());
                clear();

                this.mName = aName;
                initialize();
                for (K key : lMap.keySet()) {
                        put(key, lMap.get(key));
                }
        }
}
