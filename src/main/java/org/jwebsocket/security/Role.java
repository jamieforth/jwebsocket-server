//    ---------------------------------------------------------------------------
//    jWebSocket - Role Class
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

/**
 * implements a role which contains a set of rights.
 * @author aschulze
 */
public class Role {

    private String mId = null;
    private String mDescription = null;
    private Rights mRights = new Rights();

    /**
     * creates a new empty Role object.
     */
    public Role() {
    }

    /**
     * creates a new Role object and initializes its key and description.
     * @param aId
     * @param aDescription
     */
    public Role(String aId, String aDescription) {
        mId = aId;
        mDescription = aDescription;
    }

    /**
     * creates a new Role object and initializes its key, description and
     * rights.
     * @param aId
     * @param aDescription
     * @param aRights
     */
    public Role(String aId, String aDescription, Right... aRights) {
        mId = aId;
        mDescription = aDescription;
        if (aRights != null) {
            for (int i = 0; i < aRights.length; i++) {
                addRight(aRights[i]);
            }
        }
    }

    /**
     * creates a new Role object and initializes its key, description and
     * rights.
     * @param aId
     * @param aDescription
     * @param aRights
     */
    public Role(String aId, String aDescription, Rights aRights) {
        mId = aId;
        mDescription = aDescription;
        mRights = aRights;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     *
     * @param aId
     */
    public void setKey(String aId) {
        this.mId = aId;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     *
     * @param description
     */
    public void setDescription(String description) {
        this.mDescription = description;
    }

    /**
     *
     * @param aRight
     */
    public void addRight(Right aRight) {
        mRights.addRight(aRight);
    }

    /**
     *
     * @param aRight
     * @return
     */
    public boolean hasRight(Right aRight) {
        return mRights.hasRight(aRight);
    }

    /**
     *
     * @param aRight
     * @return
     */
    public boolean hasRight(String aRight) {
        return mRights.hasRight(aRight);
    }

    /**
     * returns all rights of this role instance
     * @return
     */
    public Collection<Right> getRights() {
        // getRights already returns an unmodifiable collection
        return mRights.getRights();
    }
}
