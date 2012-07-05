//  ---------------------------------------------------------------------------
//  jWebSocket - HttpSessionStorage (an IBasicStorage Implementation)
//  Copyright (c) 2012 Innotrade GmbH, jWebSocket.org
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
package org.jwebsocket.storage.httpsession;

import java.util.Enumeration;
import java.util.Set;
import javax.servlet.http.HttpSession;
import javolution.util.FastSet;
import org.jwebsocket.storage.BaseStorage;

/**
 * A named storage (a map of key/value pairs) for HttpSession wrappers. Consider
 * to use this storage when running jWebSocket in embedded mode inside a Servlet
 * container.
 *
 * @author kyberneees
 */
public class HttpSessionStorage extends BaseStorage<String, Object> {

        private String mName = null;
        private HttpSession mSession;

        /**
         *
         * @return The HttpSession instance
         */
        public HttpSession getSession() {
                return mSession;
        }

        /**
         *
         * @param aSession
         */
        public HttpSessionStorage(HttpSession aSession) {
                mName = aSession.getId();
                mSession = aSession;

                initialize();
        }

        /**
         *
         * {@inheritDoc }
         */
        @Override
        public String getName() {
                return mName;
        }

        /**
         *
         * {@inheritDoc }
         */
        @Override
        public void setName(String aName) throws Exception {
                throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set keySet() {
                Set lKeys = new FastSet();
                Enumeration<String> lSessionKeys = mSession.getAttributeNames();

                while (lSessionKeys.hasMoreElements()) {
                        lKeys.add(lSessionKeys.nextElement());
                }

                return lKeys;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get(Object aKey) {
                return mSession.getAttribute(aKey.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object remove(Object aKey) {
                Object lRes = get(aKey);
                mSession.removeAttribute(aKey.toString());

                return lRes;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear() {
                mSession.invalidate();
        }

        /**
         *
         * {@inheritDoc }
         */
        @Override
        public Object put(String aKey, Object aData) {
                mSession.setAttribute(aKey.toString(), aData);

                return aData;
        }

        /**
         *
         * {@inheritDoc }
         */
        @Override
        public void initialize() {
        }
}
