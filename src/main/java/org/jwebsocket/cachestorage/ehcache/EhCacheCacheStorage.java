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
package org.jwebsocket.cachestorage.ehcache;

import net.sf.ehcache.Element;
import org.jwebsocket.api.IBasicCacheStorage;
import org.jwebsocket.storage.ehcache.EhCacheStorage;

/**
 * a named storage (a map of key/value pairs) in EhCache. Please consider that
 * each storage is maintained in its own file on the hard disk.
 *
 * @author aschulze
 */
public class EhCacheCacheStorage<K, V> extends EhCacheStorage<K, V> implements IBasicCacheStorage<K, V> {

    /**
     *
     * @param aName
     */
    public EhCacheCacheStorage(String aName) {
        super(aName);
    }

    @Override
    public V put(K aKey, V aData, int expTime) {
        Element lElement = new Element(aKey, aData);
        lElement.setTimeToLive(expTime);
        getCache().put(lElement);

        return (V)aData;
    }
}
