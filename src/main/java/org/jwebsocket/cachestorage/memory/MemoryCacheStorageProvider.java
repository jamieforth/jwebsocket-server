//  ---------------------------------------------------------------------------
//  jWebSocket - MemoryStorageProvider
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
package org.jwebsocket.cachestorage.memory;

import org.jwebsocket.api.IBasicCacheStorage;
import org.jwebsocket.api.ICacheStorageProvider;

/**
 *
 * @author kyberneees, aschulze
 */
public class MemoryCacheStorageProvider implements ICacheStorageProvider {

    @Override
    public IBasicCacheStorage<String, Object> getCacheStorage(String aName) throws Exception {
        MemoryCacheStorage<String, Object> lStorage = new MemoryCacheStorage<String, Object>(aName);
        lStorage.initialize();

        return lStorage;
    }

    @Override
    public void removeCacheStorage(String aName) throws Exception {
        MemoryCacheStorage.getContainer().remove(aName);
    }
}
