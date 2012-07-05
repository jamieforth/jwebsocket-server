//        ---------------------------------------------------------------------------
//        jWebSocket - Users Class
//        Copyright (c) 2010 jWebSocket.org, Alexander Schulze, Innotrade GmbH
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
package org.jwebsocket.security;

import java.util.Map;
import javolution.util.FastMap;

/**
 * maintains the internal jWebSocket user FastMap. The users are loaded during the
 * startup process from the jWebSocket.xml file.
 * @author aschulze
 */
public class Users {

        private Map<String, User> mUsers = new FastMap<String, User>();

        /**
         * returns the user identified by its login name or <tt>null</tt> if no
         * user with the given login name could be found.
         * @param aLoginName
         * @return
         */
        public User getUserByLoginName(String aLoginName) {
                if (aLoginName != null) {
                        return mUsers.get(aLoginName);
                }
                return null;
        }

        /**
         * Adds a new user to the FastMap of users. If null is passed no operation
         * is performed.
         * @param aUser
         */
        public void addUser(User aUser) {
                if (aUser != null) {
                        mUsers.put(aUser.getLoginname(), aUser);
                }
        }

        /**
         * Removes a certain user identified by its login name from the FastMap
         * of users. If no user with the given login name could be found or the
         * given login name is null no operation is performed.
         * @param aLoginName
         */
        public void removeUser(String aLoginName) {
                if (aLoginName != null) {
                        mUsers.remove(aLoginName);
                }
        }

        /**
         * Removes a certain user from the FastMap of users. If the user could be found
         * or the given user object is null no operation is performed.
         * @param aUser
         */
        public void removeUser(User aUser) {
                if (aUser != null) {
                        mUsers.remove(aUser.getLoginname());
                }
        }
}
