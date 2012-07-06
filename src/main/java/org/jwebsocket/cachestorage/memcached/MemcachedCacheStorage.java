//  ---------------------------------------------------------------------------
//  jWebSocket - EventsPlugIn
//  Copyright (c) 2012 Innotrade GmbH, jWebSocket.org
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
package org.jwebsocket.cachestorage.memcached;

import net.spy.memcached.MemcachedClient;
import org.jwebsocket.api.IBasicCacheStorage;
import org.jwebsocket.storage.memcached.MemcachedStorage;

/**
 *
 * @param <K>
 * @param <V>
 * @author kyberneees
 */
public class MemcachedCacheStorage<K extends String, V> extends MemcachedStorage<K, V> implements IBasicCacheStorage<K, V> {

    /**
     *
     * @param aName
     * @param aMemcachedClient
     */
    public MemcachedCacheStorage(String aName, MemcachedClient aMemcachedClient) {
        super(aName, aMemcachedClient);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public V put(K aKey, V aValue, int aExpTime) {
        getMemcachedClient().add(aKey, aExpTime, aValue);

        return aValue;
    }
}
