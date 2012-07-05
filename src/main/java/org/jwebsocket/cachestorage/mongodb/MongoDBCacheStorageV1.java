//  ---------------------------------------------------------------------------
//  jWebSocket - MongoDBCacheStorageV1
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

import com.mongodb.*;
import java.util.Set;
import javolution.util.FastSet;
import org.jwebsocket.api.IBasicCacheStorage;
import org.jwebsocket.storage.BaseStorage;

/**
 * This class uses MongoDB servers to persist the information. 
 * <br>
 * Each cache storage represents a database collection. Please see for 
 * MongoDB collections number limit.
 *
 * @author kyberneees
 */
public class MongoDBCacheStorageV1<K, V> extends BaseStorage<K, V> implements IBasicCacheStorage<K, V> {

        private DB mDatabase;
        private String mName;
        private DBCollection mCollection;

        public MongoDBCacheStorageV1(String aName, DB aDatabase) {
                this.mDatabase = aDatabase;
                this.mName = aName;
                mCollection = aDatabase.getCollection(aName);
        }

        /**
         * {@inheritDoc
         */
        @Override
        public V put(K aKey, V aValue, int expTime) {
                mCollection.insert(new BasicDBObject()
                                .append("k", aKey)
                                .append("v", aValue)
                                .append("it", (Long)(System.currentTimeMillis() / 1000))
                                .append("et", expTime));

                return aValue;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public V put(K aKey, V aValue) {
                return put(aKey, aValue, 0);
        }

        /**
         * {@inheritDoc
         */
        @Override
        public String getName() {
                return mName;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public void setName(String aNewName) throws Exception {
                mDatabase.createCollection(aNewName, null);
                DBCollection newCollection = mDatabase.getCollection(aNewName);
                
                DBCursor lRecords = mCollection.find();
                while (lRecords.hasNext()){
                        newCollection.insert(lRecords.next());
                }
                
                mCollection.drop();
                mCollection = newCollection;
                mName = aNewName;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public void clear() {
                mCollection.drop();
        }

        /**
         * {@inheritDoc
         */
        @Override
        public boolean containsKey(Object aObj) {
                DBObject lRecord = mCollection.findOne(new BasicDBObject().append("k", aObj));

                if (lRecord != null && isValid(lRecord)) {
                        return true;
                }
                
                return false;
        }

        /**
         * 
         * @param r The DBObject record
         * @return TRUE if the record is not expired, FALSE otherwise
         */
        private boolean isValid(DBObject aRecord) {
                Integer lExpTime = (Integer)aRecord.get("et");
                if (lExpTime < 1){
                        return true;
                }
                
                if (((Long)aRecord.get("it")) + lExpTime >= System.currentTimeMillis() / 1000) {
                        return true;
                }
                //Useful to keep the collection up to date with only non-expired values
                mCollection.remove(aRecord);
                
                return false;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public boolean containsValue(Object aValue) {
                DBObject lRecord = mCollection.findOne(new BasicDBObject().append("v", aValue));

                if (lRecord != null && isValid(lRecord)) {
                        return true;
                }
                
                return false;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public V get(Object aKey) {
                DBObject lRecord = mCollection.findOne(new BasicDBObject().append("k", aKey));

                if (lRecord != null) {
                        if (isValid(lRecord)){
                                return (V)lRecord.get("v");
                        }
                }
                
                return null;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public Set<K> keySet() {
                Set<K> lKeySet = new FastSet<K>();
                DBCursor lCursor = mCollection.find();
                DBObject lRecord = null;
                
                while (lCursor.hasNext()) {
                        lRecord = lCursor.next();
                        if (isValid(lRecord)){
                                lKeySet.add((K) lRecord.get("k"));
                        }
                }
                
                return lKeySet;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public V remove(Object aKey) {
                DBObject lRecord = mCollection.findOne(new BasicDBObject().append("k", aKey));
                mCollection.remove(lRecord);
                
                if (lRecord != null && isValid(lRecord)){
                        return (V)lRecord.get("v");
                }
                
                return null;
        }

        /**
         * {@inheritDoc
         */
        @Override
        public void initialize() throws Exception {
                mCollection.ensureIndex(new BasicDBObject().append("k", 1),
                                new BasicDBObject().append("unique", true));
        }
}
