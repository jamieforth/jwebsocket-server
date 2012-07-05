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
package org.jwebsocket.storage.memory;

import org.jwebsocket.api.IBasicStorage;
import org.jwebsocket.api.IStorageProvider;

/**
 *
 * @author kyberneees, aschulze
 */
public class MemoryStorageProvider implements IStorageProvider {

        @Override
        public IBasicStorage<String, Object> getStorage(String aName) throws Exception {
                MemoryStorage<String, Object> lStorage = new MemoryStorage<String, Object>(aName);
                lStorage.initialize();
                
                return lStorage;
        }

    @Override
    public void removeStorage(String aName) throws Exception {
        MemoryStorage.getContainer().remove(aName);
    }
    
}
