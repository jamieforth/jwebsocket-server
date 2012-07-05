//        ---------------------------------------------------------------------------
//        jWebSocket - Copyright (c) 2010 jwebsocket.org
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
package org.jwebsocket.console;

import org.jwebsocket.factory.JWebSocketFactory;
import org.jwebsocket.server.CustomServer;
import org.jwebsocket.server.TokenServer;

/**
 *
 * @author aschulze
 */
public class JWebSocketEmbedded {

        public static void main(String[] aArgs) {
                // instantiate an embedded jWebSocket Server
                JWebSocketSubSystemSample jWebSocketSubsystem = new JWebSocketSubSystemSample(aArgs);
                // instantiate an embedded listener class and add it to the subsystem
                jWebSocketSubsystem.addListener(new JWebSocketTokenListenerSample());

                // start the subsystem
                jWebSocketSubsystem.start();

                // get the token server
                TokenServer lTS0 = JWebSocketFactory.getTokenServer();
                if (lTS0 != null) {
                        // and add the sample listener to the server's listener chain
                        lTS0.addListener(new JWebSocketTokenListenerSample());
                }

                // get the custom server
                CustomServer lCS0 = (CustomServer) JWebSocketFactory.getServer("cs0");
                if (lCS0 != null) {
                        // and add the sample listener to the server's listener chain
                        lCS0.addListener(new JWebSocketCustomListenerSample());
                }


                // wait until shutdown requested
                jWebSocketSubsystem.run();
                // stop the subsystem
                jWebSocketSubsystem.stop();
        }
}
