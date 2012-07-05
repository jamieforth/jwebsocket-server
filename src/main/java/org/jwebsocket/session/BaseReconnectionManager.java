//        ---------------------------------------------------------------------------
//        jWebSocket - Copyright (c) 2011 jwebsocket.org
//        ---------------------------------------------------------------------------
//        This program is free software; you can redistribute it and/or modify it
//        under the terms of the GNU Lesser General Public License as published by the
//        Free Software Foundation; either version 3 of the License, or (at your
//        option) any later version.
//        This program is distributed in the hope that it will be useful, but WITHOUT
//        ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//        FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
//        more details.
//        You should have received a copy of the GNU Lesser General Public License along
//        with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//        ---------------------------------------------------------------------------
package org.jwebsocket.session;

import org.jwebsocket.api.*;
import org.jwebsocket.util.Tools;

/**
 *
 * @author kyberneees, aschulze
 */
public abstract class BaseReconnectionManager implements ISessionReconnectionManager, IInitializable {

        private IBasicCacheStorage<String, Object> mReconnectionIndex;
        private String mCacheStorageName;
        private Integer mSessionExpirationTime = 60; //One minute by default
        private IBasicStorage<String, Object> mSessionIdsTrash;
        private String mTrashStorageName;
        private IStorageProvider mStorageProvider;
        private ICacheStorageProvider mCacheStorageProvider;
        private Integer mGCProcessTime = 300000; //Five minutes by default

        public ICacheStorageProvider getCacheStorageProvider() {
                return mCacheStorageProvider;
        }

        public void setCacheStorageProvider(ICacheStorageProvider aCacheStorageProvider) {
                this.mCacheStorageProvider = aCacheStorageProvider;
        }

        public String getCacheStorageName() {
                return mCacheStorageName;
        }

        public void setCacheStorageName(String aCacheStorageName) {
                this.mCacheStorageName = aCacheStorageName;
        }

        @Override
        public IBasicCacheStorage<String, Object> getReconnectionIndex() {
                return mReconnectionIndex;
        }

        public void setReconnectionIndex(IBasicCacheStorage<String, Object> aReconnectionIndex) {
                this.mReconnectionIndex = aReconnectionIndex;
        }

        @Override
        public Integer getSessionExpirationTime() {
                return mSessionExpirationTime;
        }

        public void setSessionExpirationTime(Integer aSessionExpirationTime) {
                this.mSessionExpirationTime = aSessionExpirationTime;
        }

        @Override
        public IBasicStorage<String, Object> getSessionIdsTrash() {
                return mSessionIdsTrash;
        }

        public void setSessionIdsTrash(IBasicStorage<String, Object> aSessionIdsTrash) {
                this.mSessionIdsTrash = aSessionIdsTrash;
        }

        public String getTrashStorageName() {
                return mTrashStorageName;
        }

        public void setTrashStorageName(String aTrashStorageName) {
                this.mTrashStorageName = aTrashStorageName;
        }

        @Override
        public void putInReconnectionMode(String aSessionId) {
                getReconnectionIndex().put(aSessionId, true, getSessionExpirationTime());

                //Used by a deamon to release expired sessions resources
                getSessionIdsTrash().put(aSessionId, System.currentTimeMillis() + (getSessionExpirationTime() * 1000));
        }

        @Override
        public IStorageProvider getStorageProvider() {
                return mStorageProvider;
        }

        public void setStorageProvider(IStorageProvider mStorageProvider) {
                this.mStorageProvider = mStorageProvider;
        }

        @Override
        public void initialize() throws Exception {
                Tools.getTimer().scheduleAtFixedRate(
                                new CleanExpiredSessionsTask(
                                getSessionIdsTrash(), getStorageProvider()), 0, getGCProcessTime());
        }

        @Override
        public void shutdown() throws Exception {
        }

        @Override
        public boolean isExpired(String aSessionId) {
                if (getReconnectionIndex().containsKey(aSessionId)) {
                        return false;
                }

                return true;
        }

        @Override
        public Integer getGCProcessTime() {
                return mGCProcessTime;
        }

        public void setGCProcessTime(Integer aGCProcessTime) {
                this.mGCProcessTime = aGCProcessTime;
        }
}
