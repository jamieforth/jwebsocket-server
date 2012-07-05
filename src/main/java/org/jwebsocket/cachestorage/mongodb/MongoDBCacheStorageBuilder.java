//  ---------------------------------------------------------------------------
//  jWebSocket - MongoDBCacheStorageBuilder
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
package org.jwebsocket.cachestorage.mongodb;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import org.jwebsocket.api.IBasicCacheStorage;

/**
 * Create MongoDBCacheStorage instances
 *
 * @author kyberneees
 */
public class MongoDBCacheStorageBuilder {

        private Mongo mCon;
        private String mDatabaseName;
        private String mCollectionName;
        public static final String V1 = "v1";
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
         * @param aName The cache storage name to build
         * @return The cache storage ready to use.
         */
        public IBasicCacheStorage<String, Object> getCacheStorage(String aVersion, String aName) throws Exception {
                IBasicCacheStorage<String, Object> lCache = null;
                if (aVersion.equals(V1)) {
                        lCache = new MongoDBCacheStorageV1<String, Object>(aName, mDatabase);
                        lCache.initialize();
                } else if (aVersion.equals(V2)) {
                        lCache = new MongoDBCacheStorageV2<String, Object>(aName, mCollection);
                        lCache.initialize();
                }

                return lCache;
        }

        /**
         * @return the databaseName
         */
        public String getDatabaseName() {
                return mDatabaseName;
        }

        /**
         * @param databaseName the databaseName to set
         */
        public void setDatabaseName(String aDatabaseName) {
                this.mDatabaseName = aDatabaseName;

                //Getting the temporal database instance to improve performance
                mDatabase = mCon.getDB(aDatabaseName);
        }

        /**
         * @return The database collection name for cache storages of version 2
         */
        public String getCollectionName() {
                return mCollectionName;
        }

        /**
         * @param aCollectionName The database collection name for cache storages of version 2
         */
        public void setCollectionName(String aCollectionName) {
                this.mCollectionName = aCollectionName;

                //Getting the temporal collection instance to improve performance
                mCollection = mCon.getDB(mDatabaseName).getCollection(aCollectionName);
        }
}