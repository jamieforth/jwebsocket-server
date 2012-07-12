//    ---------------------------------------------------------------------------
//    jWebSocket - TokenFilterChain Implementation
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

import java.util.List;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketFilter;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.api.WebSocketServer;
import org.jwebsocket.kit.ChangeType;
import org.jwebsocket.kit.FilterResponse;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.token.Token;

/**
 *
 * @author aschulze
 */
public class TokenFilterChain extends BaseFilterChain {

    private static Logger mLog = Logging.getLogger();

    /**
     *
     * @param aServer
     */
    public TokenFilterChain(WebSocketServer aServer) {
        super(aServer);
    }

    /**
     * @return the server
     */
    @Override
    public TokenServer getServer() {
        return (TokenServer) super.getServer();
    }

    /**
     * 
     * @param aFilter
     */
    @Override
    public void addFilter(WebSocketFilter aFilter) {
        if (aFilter != null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Adding token filter " + aFilter + "...");
            }
            super.addFilter(aFilter);
        }
    }

    /**
     * 
     * @param aFilter
     */
    @Override
    public void removeFilter(WebSocketFilter aFilter) {
        if (aFilter != null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Removing token filter " + aFilter + "...");
            }
            super.removeFilter(aFilter);
        }
    }

    /**
     * 
     * @param aConnector
     * @param aDataPacket
     * @return
     */
    @Override
    public FilterResponse processPacketIn(WebSocketConnector aConnector, WebSocketPacket aDataPacket) {
        // FilterResponse lFilterResponse = new FilterResponse();
        // return lFilterResponse;
        return null;
    }

    /**
     * 
     * @param aSource
     * @param aTarget
     * @param aDataPacket
     * @return
     */
    @Override
    public FilterResponse processPacketOut(WebSocketConnector aSource, WebSocketConnector aTarget, WebSocketPacket aDataPacket) {
        // FilterResponse lFilterResponse = new FilterResponse();
        // return lFilterResponse;
        return null;
    }

    /**
     *
     * @param aConnector
     * @param aToken
     * @return
     */
    public FilterResponse processTokenIn(WebSocketConnector aConnector, Token aToken) {
        FilterResponse lFilterResponse = new FilterResponse();
        for (WebSocketFilter lFilter : getFilters()) {
            if (lFilter.getEnabled()) {
                try {
                    ((TokenFilter) lFilter).processTokenIn(lFilterResponse, aConnector, aToken);
                } catch (RuntimeException lEx) {
                    mLog.error(lEx.getClass().getSimpleName()
                            + " in incoming filter: " + lFilter.getId()
                            + ": " + lEx.getMessage());
                }
                if (lFilterResponse.isRejected()) {
                    break;
                }
            }
        }
        return lFilterResponse;
    }

    /**
     * 
     * @param aSource
     * @param aTarget
     * @param aToken
     * @return
     */
    public FilterResponse processTokenOut(WebSocketConnector aSource, WebSocketConnector aTarget, Token aToken) {
        FilterResponse lFilterResponse = new FilterResponse();
        for (WebSocketFilter lFilter : getFilters()) {
            if (lFilter.getEnabled()) {
                try {
                    ((TokenFilter) lFilter).processTokenOut(lFilterResponse, aSource, aTarget, aToken);
                } catch (RuntimeException lEx) {
                    mLog.error(lEx.getClass().getSimpleName()
                            + " in outgoing filter: " + lFilter.getId()
                            + ": " + lEx.getMessage());
                }
                if (lFilterResponse.isRejected()) {
                    break;
                }
            }
        }
        return lFilterResponse;
    }

    /**
     * 
     * @param aFilter
     * @param aReasonOfChange
     * @param aVersion
     * @param aReason
     * @return
     */
    public Boolean reloadFilter(WebSocketFilter aFilter, Token aReasonOfChange, String aVersion, String aReason) {
        List<WebSocketFilter> lFilters = getFilters();

        for (int i = 0; i < lFilters.size(); i++) {
            if (lFilters.get(i).getId().equals(aFilter.getId())) {
                aFilter.setFilterChain(this);
                lFilters.get(i).setEnabled(false);
                ((TokenFilter) lFilters.get(i)).createReasonOfChange(aReasonOfChange, ChangeType.UPDATED, aVersion, aReason);
                lFilters.set(i, aFilter);
                return true;
            }
        }
        return false;
    }
}
