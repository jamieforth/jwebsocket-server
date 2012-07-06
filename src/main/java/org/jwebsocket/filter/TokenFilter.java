//    ---------------------------------------------------------------------------
//    jWebSocket - TokenFilter Implementation
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
package org.jwebsocket.filter;

import org.jwebsocket.api.FilterConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.kit.ChangeType;
import org.jwebsocket.kit.FilterResponse;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.token.Token;

/**
 * 
 * @author aschulze
 * @author Marcos Antonio González Huerta (markos0886, UCI)
 */
public class TokenFilter extends BaseFilter {

    public TokenFilter(FilterConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void processPacketIn(FilterResponse aResponse, WebSocketConnector aConnector, WebSocketPacket aPacket) {
    }

    @Override
    public void processPacketOut(FilterResponse aResponse, WebSocketConnector aSource, WebSocketConnector aTarget, WebSocketPacket aPacket) {
    }

    public void processTokenIn(FilterResponse aResponse, WebSocketConnector aConnector, Token aToken) {
    }

    public void processTokenOut(FilterResponse aResponse, WebSocketConnector aSource, WebSocketConnector aTarget, Token aToken) {
    }

    public void createReasonOfChange(Token aResponse, ChangeType aType, String aVersion, String aReason) {
        aResponse.setNS(getFilterConfiguration().getNamespace());
        aResponse.setType("processChangeOfPlugIn");
        aResponse.setString("changeType", aType.toString());
        aResponse.setString("version", aVersion);
        aResponse.setString("reason", aReason);
        aResponse.setString("id", getId());
    }
    
    @Override
    public TokenServer getServer(){
        return (TokenServer) super.getServer();
    }
}
