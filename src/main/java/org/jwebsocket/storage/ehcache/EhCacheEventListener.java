//  ---------------------------------------------------------------------------
//  jWebSocket - EhCacheEventListener
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
package org.jwebsocket.storage.ehcache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;

/**
 *
 * @author aschulze
 */
public class EhCacheEventListener implements CacheManagerEventListener {

        /**
         * 
         * @throws CacheException
         */
        @Override
        public void init() throws CacheException {
        }

        /**
         * 
         * @param aCacheName
         */
        @Override
        public void notifyCacheAdded(String aCacheName) {
        }

        /**
         * 
         * @param aCacheName
         */
        @Override
        public void notifyCacheRemoved(String aCacheName) {
        }

        /**
         * 
         * @return
         */
        @Override
        public Status getStatus() {
                return null;
        }

        /**
         * 
         */
        @Override
        public void dispose() {
        }
}
