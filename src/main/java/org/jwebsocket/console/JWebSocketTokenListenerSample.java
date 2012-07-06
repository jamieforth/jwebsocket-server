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
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.kit.WebSocketServerEvent;
import org.jwebsocket.listener.WebSocketServerTokenEvent;
import org.jwebsocket.listener.WebSocketServerTokenListener;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.token.Token;

/**
 * This shows an example of a simple WebSocket listener
 * @author aschulze
 */
public class JWebSocketTokenListenerSample implements WebSocketServerTokenListener {

    private static Logger mLog = Logging.getLogger();

    /**
     *
     * @param aEvent
     */
    @Override
    public void processOpened(WebSocketServerEvent aEvent) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Client '" + aEvent.getConnector() + "' connected.");
        }
    }

    /**
     *
     * @param aEvent
     * @param aPacket
     */
    @Override
    public void processPacket(WebSocketServerEvent aEvent, WebSocketPacket aPacket) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing data packet '" + aPacket.getUTF8() + "'...");
        }
        // Here you can answer an arbitrary text package...
        // this is how to easily respond to a previous client's request
        // aEvent.sendPacket(aPacket);
        // this is how to send a packet to any connector
        // aEvent.getServer().sendPacket(aEvent.getConnector(), aPacket);
    }

    /**
     *
     * @param aEvent
     * @param aToken
     */
    @Override
    public void processToken(WebSocketServerTokenEvent aEvent, Token aToken) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Client '" + aEvent.getConnector() + "' sent Token: '" + aToken.toString() + "'.");
        }
        // here you can simply interpret the token type sent from the client
        // according to your needs.
        String lNS = aToken.getNS();
        String lType = aToken.getType();

        // check if token has a type and a matching namespace
        if (lType != null && "my.namespace".equals(lNS)) {
            // if type is "getInfo" return some server information
            Token lResponse = aEvent.createResponse(aToken);
            if ("getInfo".equals(lType)) {
                lResponse.setString("vendor", JWebSocketCommonConstants.VENDOR);
                lResponse.setString("version", JWebSocketServerConstants.VERSION_STR);
                lResponse.setString("copyright", JWebSocketCommonConstants.COPYRIGHT);
                lResponse.setString("license", JWebSocketCommonConstants.LICENSE);
            } else if ("getTokenizable".equals(lType)) {
                // create a new tokenizable object and put it to the token
                SampleTokenizable lTokenizable = new SampleTokenizable(
                        "Alex", "Schulze",
                        "An Vieslapp 29",
                        "D-52134", "Herzogenrath");
                lResponse.set(lTokenizable);
            } else {
                // if unknown type in this namespace, return corresponding error message
                lResponse.setInteger("code", -1);
                lResponse.setString("msg", "Token type '" + lType + "' not supported in namespace '" + lNS + "'.");
            }
            aEvent.sendToken(lResponse);
        }
    }

    /**
     *
     * @param aEvent
     */
    @Override
    public void processClosed(WebSocketServerEvent aEvent) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Client '" + aEvent.getConnector() + "' disconnected.");
        }
    }
}
