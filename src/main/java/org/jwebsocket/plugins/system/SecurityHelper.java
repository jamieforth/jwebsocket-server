//    ---------------------------------------------------------------------------
//    Copyright (c) 2011 jWebSocket.org, Innotrade GmbH
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
package org.jwebsocket.plugins.system;

import java.util.Map;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.session.SessionManager;
import org.jwebsocket.spring.JWebSocketBeanFactory;

/**
 *
 * @author kyberneees
 */
public class SecurityHelper {

    private static SessionManager mSessionManager = null;

    private static SessionManager getSessionManager() {
        if (null == mSessionManager) {
            mSessionManager = (SessionManager) JWebSocketBeanFactory.getInstance().getBean("sessionManager");
        }
        return mSessionManager;
    }

    public static Boolean isUserAuthenticated(String aSessionId) {
        try {
            Map<String, Object> session = getSessionManager().getSession(aSessionId);
            if (session.containsKey(SystemPlugIn.IS_AUTHENTICATED)) {
                return (Boolean) session.get(SystemPlugIn.IS_AUTHENTICATED);
            }
            return false;
        } catch (Exception ex) {
            // Not necessary to try this
            return false;
        }
    }

    public static boolean userHasAuthority(String aSessionId, String aAuthority) {
        try {
            Map<String, Object> session = getSessionManager().getSession(aSessionId);
            if (session.containsKey(SystemPlugIn.AUTHORITIES)) {
                String[] authorities = ((String) session.get(SystemPlugIn.AUTHORITIES)).split(" ");
                int end = authorities.length;

                for (int i = 0; i < end; i++) {
                    if (authorities[i].equals(aAuthority)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception ex) {
            //Not necessary to try this
            return false;
        }
    }

    public static boolean isUserAuthenticated(WebSocketConnector aConnector) {
        return isUserAuthenticated(aConnector.getSession().getSessionId());
    }

    public static boolean userHasAuthority(WebSocketConnector aConnector, String aAuthority) {
        return userHasAuthority(aConnector.getSession().getSessionId(), aAuthority);
    }
}
