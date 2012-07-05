//        ---------------------------------------------------------------------------
//        jWebSocket - Copyright (c) 2010 Innotrade GmbH
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
package org.jwebsocket.listener;

import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketServer;
import org.jwebsocket.kit.BroadcastOptions;
import org.jwebsocket.kit.WebSocketServerEvent;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.token.Token;

/**
 *
 * @author aschulze
 */
public class WebSocketServerTokenEvent extends WebSocketServerEvent {

        /**
         * 
         * @param aConnector
         * @param aServer
         */
        public WebSocketServerTokenEvent(WebSocketConnector aConnector, WebSocketServer aServer) {
                super(aConnector, aServer);
        }

        /**
         *
         * @param aToken
         */
        public void sendToken(Token aToken) {
                ((TokenServer) getServer()).sendToken(getConnector(), aToken);
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>createResponse</tt> to simplify token plug-in code.
         * @param aInToken
         * @return
         */
        public Token createResponse(Token aInToken) {
                TokenServer lServer = (TokenServer) getServer();
                if (lServer != null) {
                        return lServer.createResponse(aInToken);
                } else {
                        return null;
                }
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>createAccessDenied</tt> to simplify token plug-in code.
         * @param aInToken
         * @return
         */
        public Token createAccessDenied(Token aInToken) {
                TokenServer lServer = (TokenServer) getServer();
                if (lServer != null) {
                        return lServer.createAccessDenied(aInToken);
                } else {
                        return null;
                }
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>sendToken</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public void sendToken(WebSocketConnector aSource, WebSocketConnector aTarget, Token aToken) {
                TokenServer lServer = (TokenServer) getServer();
                if (lServer != null) {
                        lServer.sendToken(aSource, aTarget, aToken);
                }
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>sendToken</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        /*
        public void sendToken(WebSocketConnector aTarget, Token aToken) {
        TokenServer lServer = getServer();
        if (lServer != null) {
        lServer.sendToken(aTarget, aToken);
        }
        }
         */
        /**
         * Convenience method, just a wrapper for token server method
         * <tt>sendToken</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public void broadcastToken(WebSocketConnector aSource, Token aToken) {
                TokenServer lServer = (TokenServer) getServer();
                if (lServer != null) {
                        lServer.broadcastToken(aSource, aToken);
                }
        }

        public void broadcastToken(WebSocketConnector aSource, Token aToken,
                        BroadcastOptions aBroadcastOptions) {
                TokenServer lServer = (TokenServer) getServer();
                if (lServer != null) {
                        lServer.broadcastToken(aSource, aToken, aBroadcastOptions);
                }
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>getUsername</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public String getUsername(WebSocketConnector aConnector) {
                return ((TokenServer) getServer()).getUsername(aConnector);
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>setUsername</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public void setUsername(WebSocketConnector aConnector, String aUsername) {
                ((TokenServer) getServer()).setUsername(aConnector, aUsername);
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>removeUsername</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public void removeUsername(WebSocketConnector aConnector) {
                ((TokenServer) getServer()).removeUsername(aConnector);
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>getConnector</tt> to simplify token plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public WebSocketConnector getConnector(String aId) {
                return ((TokenServer) getServer()).getConnector(aId);
        }

        /**
         * Convenience method, just a wrapper for token server method
         * <tt>getServer().getAllConnectors().size()</tt> to simplify token
         * plug-in code.
         * @param aSource
         * @param aTarget
         * @param aToken
         */
        public int getConnectorCount() {
                return ((TokenServer) getServer()).getAllConnectors().size();
        }
}
