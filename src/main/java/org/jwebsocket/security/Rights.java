//    ---------------------------------------------------------------------------
//    jWebSocket - Right Class
//    Copyright (c) 2010 jWebSocket.org, Alexander Schulze, Innotrade GmbH
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
package org.jwebsocket.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javolution.util.FastMap;

/**
 * implements a FastMap of rights to act as a role.
 * @author aschulze
 */
public class Rights {

    private Map<String, Right> mRights = new FastMap<String, Right>();

    /**
     * adds a new right to the FastMap of rights. If there is already a right with
     * the given stored in the FastMap it will be overwritten. If null is passed or
     * if the right has no valid key no operation is performed.
     * @param aRight
     */
    public void addRight(Right aRight) {
        if (aRight != null && aRight.getId() != null) {
            mRights.put(aRight.getId(), aRight);
        }
    }

    /**
     * returns a right identified by its key or <tt>null</tt> if the right
     * cannot be found in the FastMap or the key passed is <tt>null</tt>.
     * @param aKey
     * @return
     */
    public Right get(String aKey) {
        if (aKey != null) {
            return mRights.get(aKey);
        }
        return null;
    }

    /**
     * removes a certain right identified by its key from the FastMap of rights.
     * If the key is <tt>null</tt> or right could not be found in the FastMap no
     * operation is performed.
     * @param aKey
     */
    public void removeRight(String aKey) {
        if (aKey != null) {
            mRights.remove(aKey);
        }
    }

    /**
     * removes a certain right from the FastMap of rights.
     * If the right could not be found in the FastMap no operation is performed.
     * @param aRight
     */
    public void removeRight(Right aRight) {
        if (aRight != null) {
            mRights.remove(aRight.getId());
        }
    }

    /**
     * checks if the FastMap of rights contains a certain right. The key of the
     * right passed must not be null.
     * @param aRight
     * @return
     */
    public boolean hasRight(Right aRight) {
        if (aRight != null && aRight.getId() != null) {
            return mRights.containsKey(aRight.getId());
        } else {
            return false;
        }
    }

    /**
     * checks if the FastMap of rights contains a certain right identified by its
     * key. The key must not be null.
     * @param aKey
     * @return
     */
    public boolean hasRight(String aKey) {
        if (aKey != null) {
            return mRights.containsKey(aKey);
        } else {
            return false;
        }
    }

    /**
     * returns an unmodifiable collection of rights.
     * @return
     */
    public Collection<Right> getRights() {
        return Collections.unmodifiableCollection(mRights.values());
    }

    /**
     * returns an unmodifiable set of the ids of all rights.
     * @return
     */
    public Set<String> getRightIdSet() {
        return Collections.unmodifiableSet(mRights.keySet());
    }
}
