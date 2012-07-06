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

/**
 * implements a right as part of a FastMap of rights for a certain role.
 * @author aschulze
 */
public class Right {

    private String mId = null;
    private String mDescription = null;

    /**
     * creates a new default right with a name space, an id and a description.
     * @param aId
     * @param aDescription
     */
    public Right(String aNS, String aId, String aDescription) {
        mId = aNS + "." + aId;
        mDescription = aDescription;
    }

    /**
     * creates a new default right with a id and a description.
     * @param aId
     * @param aDescription
     */
    public Right(String aId, String aDescription) {
        mId = aId;
        mDescription = aDescription;
    }
    /**
     * returns the id of the right. The key is the unique identifier of the
     * right and should contain the entire name space 
     * e.g. <tt>org.jwebsocket.plugins.chat.broadcast</tt>.
     * The key is case-sensitve.
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * specifies the id of the right. The key is the unique identifier of the
     * right and should contain the entire name space
     * e.g. <tt>org.jwebsocket.plugins.chat.broadcast</tt>.
     * The key is case-sensitve.
     * @param aId
     */
    public void setId(String aId) {
        this.mId = aId;
    }

    /**
     * returns the description of the right.
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * specifies the description of the right.
     * @param description
     */
    public void setDescription(String description) {
        this.mDescription = description;
    }

}
