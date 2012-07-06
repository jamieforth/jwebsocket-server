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
package org.jwebsocket.storage.mongodb;

import com.mongodb.Mongo;
import org.jwebsocket.api.IBasicStorage;
import org.jwebsocket.api.IStorageProvider;

/**
 *
 * @author kyberneees, aschulze
 */
public class MongoDBStorageProvider extends MongoDBStorageBuilder implements IStorageProvider {

    /**
     *
     */
    public MongoDBStorageProvider() {
        super();
    }

    /**
     * {@inheritDoc
     */
    @Override
    public IBasicStorage<String, Object> getStorage(String aName) throws Exception {
        return this.getStorage(MongoDBStorageBuilder.V2, aName);
    }
    
    @Override
    public void removeStorage(String aName) throws Exception {
        this.getStorage(aName).clear();
    }

    /**
     *
     * @param aCon
     * @param aDBName
     * @param aStorageName
     * @param aCollectionName
     * @return
     * @throws Exception
     */
    public static IBasicStorage getInstance(Mongo aCon, String aDBName,
            String aCollectionName, String aStorageName) throws Exception {
        MongoDBStorageBuilder lBuilder = new MongoDBStorageBuilder();
        lBuilder.setCon(aCon);
        lBuilder.setDatabaseName(aDBName);
        lBuilder.setCollectionName(aCollectionName);
        return lBuilder.getStorage(MongoDBStorageBuilder.V2, aStorageName);
    }
}
