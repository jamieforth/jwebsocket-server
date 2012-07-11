//  ---------------------------------------------------------------------------
//  jWebSocket - EhCacheManager
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.jwebsocket.config.JWebSocketConfig;

/**
 *
 * @author aschulze
 */
public class EhCacheManager {

    private volatile static CacheManager mInstance = null;

    /**
     * Default constructor, cannot be called from outside this class.
     */
    private EhCacheManager() {
    }

    /**
     * Static method, returns the one and only instance
     */
    public static CacheManager getInstance() {
        if (mInstance == null) {
            ClassLoader lClassLoader = Thread.currentThread().getContextClassLoader();
            mInstance = new CacheManager(JWebSocketConfig.getConfigFolder("ehcache.xml", lClassLoader));
        }
        return mInstance;
    }

    /**
     * Static method, returns the one and only instance
     */
    public static Cache getCache(String aName) {
        return getInstance().getCache(aName);
    }
}