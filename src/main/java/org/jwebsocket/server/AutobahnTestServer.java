/*
 * jWebSocket - AutobahnTestServer
 *
 * Copyright (C) 2012  Jamie Forth <jamie.forth@eecs.qmul.ac.uk>, 
 * SerenA <http://www.serena.ac.uk>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jwebsocket.server;

import org.apache.log4j.Logger;
import org.jwebsocket.api.ServerConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.filter.BaseFilterChain;
import org.jwebsocket.kit.RawPacket;
import org.jwebsocket.kit.RequestHeader;
import org.jwebsocket.kit.WebSocketFrameType;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.plugins.BasePlugInChain;
import org.jwebsocket.server.BaseServer;

/**
 * 
 * @author jforth
 */
public class AutobahnTestServer extends BaseServer {

    private static Logger mLog = Logging.getLogger(AutobahnTestServer.class);

    /**
     * Minimal server for testing against the Autobahn Test Suite.
     */
    public AutobahnTestServer(ServerConfiguration aServerConfig) {
        super(aServerConfig);
        // FIXME: Plug-ins and filters unfortunately seem to be required by the
        // configuration setup. Furthermore, it should be possible to
        // programmatically instantiate a minimal server (easily), which
        // would simplify testing.
        mPlugInChain = new BasePlugInChain(this);
        mFilterChain = new BaseFilterChain(this);
    }

    @Override
    public void processPacket(WebSocketEngine aEngine, WebSocketConnector aConnector,
            WebSocketPacket aDataPacket) {

        // FIXME: Duplicating TEXT/BINARY checking here is stupid. Are JWS
        // "packets" always TEXT? Maybe better to make a custom server that
        // doesn't use the packet abstraction?
        if (WebSocketFrameType.TEXT.equals(aDataPacket.getFrameType()))
            processText(aEngine, aConnector, aDataPacket);
        else if (WebSocketFrameType.BINARY.equals(aDataPacket.getFrameType()))
            processBinary(aEngine, aConnector, aDataPacket);
    }

    public void processText(WebSocketEngine aEngine, WebSocketConnector aConnector,
            WebSocketPacket aDataPacket) {

        // TEXT MUST be UTF-8 according to the spec.
        // FIXME: This should fail on invalid UTF-8!
        String payload = aDataPacket.getUTF8(); 

        if (mLog.isDebugEnabled()) {
            if (payload.isEmpty())
                mLog.debug("Processing empty text packet.'");
            else if (payload.length() < 50)
                mLog.debug("Processing text packet '" + payload + "'.");
            else
                mLog.debug("Processing text packet '" + payload.substring(0, 50)
                        + "'... (total payload length: " + payload.length() + ")");
        }

        RequestHeader lHeader = aConnector.getHeader();
        String lFormat = (lHeader != null ? lHeader.getFormat() : null);

        // FIXME: This should fail on invalid UTF-8!
        RawPacket replyPacket = new RawPacket(WebSocketFrameType.TEXT, payload);
        sendPacket(aConnector, replyPacket);
    }

    public void processBinary(WebSocketEngine aEngine, WebSocketConnector aConnector,
            WebSocketPacket aDataPacket) {

        byte[] payload = aDataPacket.getByteArray();

        if (mLog.isDebugEnabled()) {
            if (payload.length == 0)
                mLog.debug("Processing empty binary packet.'");
            else
                mLog.debug("Processing binary packet of length: '" + payload.length + "'.");
        }

        RequestHeader lHeader = aConnector.getHeader();
        String lFormat = (lHeader != null ? lHeader.getFormat() : null);

        RawPacket replyPacket = new RawPacket(WebSocketFrameType.BINARY, payload);
        sendPacket(aConnector, replyPacket);
    }
}
