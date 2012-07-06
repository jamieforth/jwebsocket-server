//    ---------------------------------------------------------------------------
//    jWebSocket - Copyright (c) 2010 jwebsocket.org
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
package org.jwebsocket.console;

import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.api.WebSocketServerListener;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.kit.WebSocketServerEvent;
import org.jwebsocket.logging.Logging;

/**
 * This shows an example of a simple WebSocket listener
 * @author aschulze
 */
public class JWebSocketCustomListenerSample implements WebSocketServerListener {

    private static Logger log = Logging.getLogger();

    /**
     *
     * @param aEvent
     */
    @Override
    public void processOpened(WebSocketServerEvent aEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Client '" + aEvent.getConnector() + "' connected.");
        }
    }

    /**
     *
     * @param aEvent
     * @param aPacket
     */
    @Override
    public void processPacket(WebSocketServerEvent aEvent, WebSocketPacket aPacket) {
        if (log.isDebugEnabled()) {
            log.debug("Processing data packet '" + aPacket.getUTF8() + "'...");
        }
        aPacket.setUTF8("[echo from jWebSocket v" + JWebSocketServerConstants.VERSION_STR + "] " + aPacket.getUTF8());
        /*
        StringBuilder lStrBuf = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            lStrBuf.append("<br>" + i + ": 1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        aPacket.setUTF8(lStrBuf.toString());
         */
        aEvent.sendPacket(aPacket);
    }

    /**
     *
     * @param aEvent
     */
    @Override
    public void processClosed(WebSocketServerEvent aEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Client '" + aEvent.getConnector() + "' disconnected.");
        }
    }
}
