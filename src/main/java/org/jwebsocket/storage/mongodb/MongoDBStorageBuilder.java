//  ---------------------------------------------------------------------------
//  jWebSocket - MongoDBStorageBuilder
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
package org.jwebsocket.storage.mongodb;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import org.jwebsocket.api.IBasicStorage;

/**
 * Create MongoDBStorage instances
 *
 * @author kyberneees
 */
public class MongoDBStorageBuilder {

    private Mongo mCon;
    private String mDatabaseName;
    private String mCollectionName;
    /**
     *
     */
    public static final String V1 = "v1";
    /**
     *
     */
    public static final String V2 = "v2";
    private DBCollection mCollection = null;
    private DB mDatabase = null;

    /**
     *
     * @return The Mongo database connection
     */
    public Mongo getCon() {
        return mCon;
    }

    /**
     *
     * @param aCon The Mongo database connection to set
     */
    public void setCon(Mongo aCon) {
        this.mCon = aCon;
    }

    /**
     *
     * @param aVersion
     * @param aName The storage name
     * @return The MongoDB storage ready to use.
     * @throws Exception
     */
    public IBasicStorage<String, Object> getStorage(String aVersion, String aName) throws Exception {
        IBasicStorage<String, Object> lStorage = null;
        if (aVersion.equals(V1)) {
            lStorage = new MongoDBStorageV1<String, Object>(aName, mDatabase);
            lStorage.initialize();
        } else if (aVersion.equals(V2)) {
            lStorage = new MongoDBStorageV2<String, Object>(aName, mCollection);
            lStorage.initialize();
        }

        return lStorage;
    }

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
        return mDatabaseName;
    }

    /**
     * @param aDatabaseName the databaseName to set
     */
    public void setDatabaseName(String aDatabaseName) {
        this.mDatabaseName = aDatabaseName;

        //Getting the temporal database instance to improve performance
        mDatabase = mCon.getDB(aDatabaseName);
    }

    /**
     * @return The database collection name for storages of version 2
     */
    public String getCollectionName() {
        return mCollectionName;
    }

    /**
     * @param aCollectionName The database collection name for storages of
     * version 2
     */
    public void setCollectionName(String aCollectionName) {
        this.mCollectionName = aCollectionName;

        //Getting the temporal collection instance to improve performance
        mCollection = mCon.getDB(mDatabaseName).getCollection(aCollectionName);
    }
}