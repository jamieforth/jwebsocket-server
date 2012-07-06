//    ---------------------------------------------------------------------------
//    jWebSocket - Packet Builder for Fragmentation support
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
package org.jwebsocket.server;

import java.util.Map;
import javolution.util.FastMap;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.kit.RawPacket;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;

/**
 *
 * @author aschulze
 */
public class FragmentedTokenBuilder {

    private static Map<String, WebSocketPacket> mPackets = new FastMap<String, WebSocketPacket>();

    /**
     *
     * @param aConnector
     * @param aToken
     * @return
     */
    public static Token putFragment(WebSocketConnector aConnector, Token aToken) {
        String lKey = aConnector.getId() + "." + aToken.getInteger("utid");

        WebSocketPacket lDataPacket = (WebSocketPacket) mPackets.get(lKey);
        if (lDataPacket == null) {
            lDataPacket = new RawPacket(aToken.getInteger("total"));
            mPackets.put(lKey, lDataPacket);
        }

        lDataPacket.setFragment(aToken.getString("data"), aToken.getInteger("index"));
        if (lDataPacket.isComplete()) {
            String lFormat = aConnector.getHeader().getFormat();
            lDataPacket.packFragments();
            mPackets.remove(lKey);
            return TokenFactory.packetToToken(lFormat, lDataPacket);
        }

        return null;
    }
/*
    public static void cleanUp() {

    }

    static {
        new Thread() {

            @Override
            public void run() {
                try {
                    cleanUp();
                    sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }.start();
    }
 */
}
