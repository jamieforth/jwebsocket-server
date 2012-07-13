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
import org.jwebsocket.kit.RequestHeader;
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
        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing data packet '" + aDataPacket.getUTF8().substring(0, 50) + "'...");
        }
        RequestHeader lHeader = aConnector.getHeader();
        String lFormat = (lHeader != null ? lHeader.getFormat() : null);

        // Simply echo packet (FIXME: more may be required, check Autobahn).
        sendPacket(aConnector, aDataPacket);
    }

}
