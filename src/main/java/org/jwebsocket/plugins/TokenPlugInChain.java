//    ---------------------------------------------------------------------------
//    jWebSocket - Chain of Token Plug-Ins
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
package org.jwebsocket.plugins;

import java.util.List;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketPlugIn;
import org.jwebsocket.api.WebSocketServer;
import org.jwebsocket.kit.ChangeType;
import org.jwebsocket.kit.PlugInResponse;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.token.Token;

/**
 * instantiates the chain of token plug-ins.
 * @author aschulze
 */
public class TokenPlugInChain extends BasePlugInChain {

    private static Logger mLog = Logging.getLogger();

    /**
     *
     * @param aServer
     */
    public TokenPlugInChain(WebSocketServer aServer) {
        super(aServer);
    }

    /**
     *
     * @param aConnector
     * @param aToken
     * @return
     */
    public PlugInResponse processToken(WebSocketConnector aConnector, Token aToken) {
        PlugInResponse lPlugInResponse = new PlugInResponse();
        String lNS = aToken.getNS();
        // tokens without namespace are not accepted anymore since jWebSocket 1.0a11
        if (lNS != null) {
            for (WebSocketPlugIn lPlugIn : getPlugIns()) {
                if (lPlugIn.getEnabled()) {
                    try {
                        TokenPlugIn lTokenPlugIn = ((TokenPlugIn) lPlugIn);
                        if (lNS.equals(lTokenPlugIn.getNamespace())) {
                            lTokenPlugIn.processToken(lPlugInResponse, aConnector, aToken);
                        }
                    } catch (RuntimeException lEx) {
                        mLog.error("(plug-in '"
                                + ((TokenPlugIn) lPlugIn).getNamespace() + "') "
                                + lEx.getClass().getSimpleName() + ": "
                                + lEx.getMessage()
                                + ", token: " + aToken.toString());
                    }
                    if (lPlugInResponse.isChainAborted()) {
                        break;
                    }
                }
            }
        }
        return lPlugInResponse;
    }
    
    public Boolean reloadPlugIn(WebSocketPlugIn aPlugIn, Token aReasonOfChange,String aVersion, String aReason) {
        List<WebSocketPlugIn> lPlugins = getPlugIns();
        
        for (int i = 0; i < lPlugins.size(); i++) {
            if (lPlugins.get(i).getId().equals(aPlugIn.getId())) {
                aPlugIn.setPlugInChain(this);
                lPlugins.get(i).setEnabled(false);
                ((TokenPlugIn) lPlugins.get(i)).createReasonOfChange(aReasonOfChange, ChangeType.UPDATED, aVersion, aReason);
                lPlugins.set(i, aPlugIn);
                return true;
            }
        }
        return false;
    }
}