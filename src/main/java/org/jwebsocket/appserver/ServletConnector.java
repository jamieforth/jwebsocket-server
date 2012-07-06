//    ---------------------------------------------------------------------------
//    jWebSocket - Servlet Connector Implementation
//    Copyright (c) 2010 Alexander Schulze, Innotrade GmbH
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.connectors.BaseConnector;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.RequestHeader;

/**
 *
 * @author aschulze
 */
public class ServletConnector extends BaseConnector {

    private HttpServletRequest mRequest = null;
    // private HttpServletResponse mResponse = null;
    private String mPlainResponse = "";

    /**
     *
     * @param aRequest
     * @param aResponse
     */
    public ServletConnector() {
        // no "engine" available here
        super(null);
        // TODO: Overhaul this hardcoded reference! See TokenServer class!
        setBoolean("org.jwebsocket.tokenserver.isTS", true);
        RequestHeader lHeader = new RequestHeader();
        lHeader.put(RequestHeader.WS_PROTOCOL, JWebSocketCommonConstants.WS_SUBPROT_DEFAULT);
        setHeader(lHeader);
    }

    @Override
    public void startConnector() {
    }

    @Override
    public void stopConnector(CloseReason aCloseReason) {
    }

    @Override
    public void processPacket(WebSocketPacket aDataPacket) {
    }

    @Override
    public void sendPacket(WebSocketPacket aDataPacket) {
        mPlainResponse = aDataPacket.getUTF8();
    }

    @Override
    public WebSocketEngine getEngine() {
        return null;
    }

    @Override
    public String generateUID() {
        return getRequest().getSession().getId();
    }

    @Override
    public int getRemotePort() {
        return getRequest().getRemotePort();
    }

    @Override
    public InetAddress getRemoteHost() {
        try {
            return InetAddress.getByName(getRequest().getRemoteAddr());
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    @Override
    public String getId() {
        return String.valueOf(getRemotePort());
    }

    public String getPlainResponse() {
        return mPlainResponse;
    }

    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return mRequest;
    }

    /**
     * @param aRequest the request to set
     */
    public void setRequest(HttpServletRequest aRequest) {
        this.mRequest = aRequest;
    }
}
