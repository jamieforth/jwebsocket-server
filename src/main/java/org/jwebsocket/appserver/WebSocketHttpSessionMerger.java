//    ---------------------------------------------------------------------------
//    jWebSocket - Copyright (c) 2010 Innotrade GmbH
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
package org.jwebsocket.appserver;

import javax.servlet.http.HttpSession;
import javolution.util.FastMap;
import org.jwebsocket.kit.WebSocketSession;

/**
 * This class combines both the sessions of the servlet container
 * and the websocket engine.
 * @author aschulze
 */
public class WebSocketHttpSessionMerger {

    private static FastMap<String, HttpSession> mHttpSessions = new FastMap<String, HttpSession>();
    private static FastMap<String, ServletConnector> mServletConnectors = new FastMap<String, ServletConnector>();
    private static FastMap<String, WebSocketSession> mWsSessions = new FastMap<String, WebSocketSession>();

    public static void addHttpSession(HttpSession aHttpSession) {
        mHttpSessions.put(aHttpSession.getId(), aHttpSession);
        // create a new servlet connector for this http session
        mServletConnectors.put(aHttpSession.getId(), new ServletConnector());
    }

    public static void removeHttpSession(HttpSession aHttpSession) {
        mHttpSessions.remove(aHttpSession.getId());
        // discard the servlet connector for the terminated http session
        mServletConnectors.remove(aHttpSession.getId());
    }

    public static ServletConnector getHttpConnector(HttpSession aHttpSession) {
        return mServletConnectors.get(aHttpSession.getId());
    }

    public static void addWebSocketSession(WebSocketSession aWebSocketSession) {
        mWsSessions.put(aWebSocketSession.getSessionId(), aWebSocketSession);
    }

    public static void removeWebSocketSession(WebSocketSession aWebSocketSession) {
        mWsSessions.remove(aWebSocketSession.getSessionId());
    }

    public static String getHttpSessionsCSV() {
        StringBuilder lResSB = new StringBuilder("");
        String lRes="";
        for (HttpSession lSession : mHttpSessions.values()) {
            lResSB.append(lSession.getId()).append(",");
        }
        if (lResSB.length() > 0) {
            lRes = lResSB.substring(0, lResSB.length() - 1);
        }
        return lRes;
    }

    public static String getWebSocketSessionsCSV() {
        StringBuilder lResSB = new StringBuilder("");
        String lRes = "";
        for (WebSocketSession lSession : mWsSessions.values()) {
            lResSB.append(lSession.getSessionId()).append(",");
        }
        if (lResSB.length() > 0) {
            lRes = lResSB.substring(0, lResSB.length() - 1);
        }
        return lRes;
    }
}
