//    ---------------------------------------------------------------------------
//    jWebSocket - Copyright (c) 2011 jwebsocket.org
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
package org.jwebsocket.cachestorage.mongodb;

import org.jwebsocket.api.IBasicCacheStorage;
import org.jwebsocket.api.ICacheStorageProvider;
import org.jwebsocket.storage.mongodb.MongoDBStorageBuilder;

/**
 *
 * @author kyberneees, aschulze
 */
public class MongoDBCacheStorageProvider extends MongoDBCacheStorageBuilder implements ICacheStorageProvider {

    public MongoDBCacheStorageProvider() {
    super();
    }

    /**
     * {@inheritDoc 
     */
    @Override
    public IBasicCacheStorage getCacheStorage(String aName) throws Exception {
    return this.getCacheStorage(MongoDBStorageBuilder.V2, aName);
    }

    @Override
    public void removeCacheStorage(String aName) throws Exception {
    this.getCacheStorage(aName).clear();
    }
}
