//  ---------------------------------------------------------------------------
//  jWebSocket - BaseStorage
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
package org.jwebsocket.storage;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javolution.util.FastMap;
import org.jwebsocket.api.IBasicStorage;

/**
 * Abstract base storage implementation.
 *
 * @param <K>
 * @param <V>
 * @author kyberneees
 */
public abstract class BaseStorage<K, V> implements IBasicStorage<K, V> {
        
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
        public boolean containsKey(Object o) {
                return keySet().contains(o);
        }

        /**
         * {@inheritDoc
         *
         * @throws Exception
         */
        @Override
        public boolean containsValue(Object o) {
                return values().contains(o);
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
                return keySet().size();
        }

        /**
         * {@inheritDoc
         *
         * @return
         */
        @Override
        public boolean isEmpty() {
                return keySet().isEmpty();
        }

        /**
         * {@inheritDoc
         *
         * @param aMap
         */
        @Override
        public void putAll(Map<? extends K, ? extends V> aMap) {
                for (K lkey : aMap.keySet()) {
                        put(lkey, aMap.get(lkey));
                }
        }

        /**
         * {@inheritDoc
         */
        @Override
        public void clear() {
                for (K lKey : keySet()) {
                        remove(lKey);
                }
        }

        /**
         * {@inheritDoc
         *
         * @return
         */
        @Override
        public Set<Entry<K, V>> entrySet() {
                return getAll(keySet()).entrySet();
        }
        
        /**
         * 
         * {@inheritDoc }
         */
        @Override
        public Collection<V> values() {
                return getAll(keySet()).values();
        }
}