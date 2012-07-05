//        ---------------------------------------------------------------------------
//        jWebSocket - Roles Class
//        CopyRole (c) 2010 jWebSocket.org, Alexander Schulze, Innotrade GmbH
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javolution.util.FastMap;
import javolution.util.FastSet;

/**
 *
 * @author aschulze
 */
public class Roles {

        private Map<String, Role> mRoles = new FastMap<String, Role>();

        /**
         *
         */
        public Roles() {
        }

        /**
         *
         * @param aRoles
         */
        public Roles(Role... aRoles) {
                if (aRoles != null) {
                        for (int i = 0; i < aRoles.length; i++) {
                                addRole(aRoles[i]);
                        }
                }
        }

        /**
         * Adds a new role to the FastMap of roles.
         * @param aRole
         */
        public void addRole(Role aRole) {
                if (aRole != null) {
                        mRoles.put(aRole.getId(), aRole);
                }
        }

        /**
         * Returns a certain role from the FastMap of roles identified by its key or
         * <tt>null</tt> if no role with the given exists in the FastMap of roles.
         * @param aKey
         * @return
         */
        public Role getRole(String aKey) {
                return mRoles.get(aKey);
        }

        /**
         * Removes a certain role from the FastMap of roles.
         * @param aKey
         */
        public void removeRole(String aKey) {
                mRoles.remove(aKey);
        }

        /**
         * Removes a certain role from the FastMap of roles.
         * @param aRole
         */
        public void removeRole(Role aRole) {
                if (aRole != null) {
                        mRoles.remove(aRole.getId());
                }
        }

        /**
         *
         * @param aRight
         * @return
         */
        public boolean hasRight(String aRight) {
                for (Role lRole : mRoles.values()) {
                        if (lRole.hasRight(aRight)) {
                                return true;
                        }
                }
                return false;
        }

        /**
         * returns an unmodifiable set of all rights of this role instance.
         * @return
         */
        public Set<Right> getRights() {
                Set lSet = new FastSet();
                for (Role lRole : mRoles.values()) {
                        for (Right lRight : lRole.getRights()) {
                                lSet.add(lRight);
                        }
                }
                return Collections.unmodifiableSet(lSet);
        }

        /**
         * returns an unmodifiable set of all rights of this role instance.
         * @return
         */
        public Set<String> getRightIdSet() {
                Set lSet = new FastSet();
                for (Role lRole : mRoles.values()) {
                        for (Right lRight : lRole.getRights()) {
                                lSet.add(lRight.getId());
                        }
                }
                return Collections.unmodifiableSet(lSet);
        }

        /**
         * checks if the roles contain a certain role . The role is passed as a
         * string which associates the key of the role.
         * @param aRole
         * @return
         */
        public boolean hasRole(String aRole) {
                return mRoles.containsKey(aRole);
        }

        /**
         * returns an unmodifiable set of the ids of all roles in the role map.
         * @return
         */
        public Set<String> getRoleIdSet() {
                return Collections.unmodifiableSet(mRoles.keySet());
        }
}
