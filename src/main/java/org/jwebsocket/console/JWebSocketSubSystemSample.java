//        ---------------------------------------------------------------------------
//        jWebSocket - Subsystem Sample
//        Copyright (c) 2010 jWebSocket.org, Alexander Schulze, Innotrade GmbH
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

import java.util.List;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import org.jwebsocket.api.EngineConfiguration;
import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.api.ServerConfiguration;
import org.jwebsocket.api.WebSocketServerListener;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketConfig;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.config.xml.EngineConfig;
import org.jwebsocket.config.xml.LoggingConfig;
import org.jwebsocket.config.xml.PluginConfig;
import org.jwebsocket.config.xml.ServerConfig;
import org.jwebsocket.factory.JWebSocketFactory;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.WebSocketException;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.plugins.TokenPlugInChain;
import org.jwebsocket.plugins.flashbridge.FlashBridgePlugIn;
import org.jwebsocket.plugins.system.SystemPlugIn;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.tcp.TCPEngine;

/**
 * Example of a pure programmatic embedded jWebSocket server.
 *
 * @author aschulze
 */
public class JWebSocketSubSystemSample {

        private static Logger mLog = null;
        private TokenServer mServer = null;
        private TCPEngine mEngine = null;

        /**
         */
        public JWebSocketSubSystemSample(String[] aArgs) {
                // the following line may not be removed due to GNU LGPL 3.0 license!
                JWebSocketFactory.printCopyrightToConsole();
                // check if home, config or bootstrap path are passed by command line
                JWebSocketConfig.initForConsoleApp(aArgs);

                // initialize the logging system
                LoggingConfig lLoggingConfig = new LoggingConfig(
                                20000 // reload delay
                                );
                Logging.initLogs(lLoggingConfig);
                mLog = Logging.getLogger(JWebSocketEmbedded.class);

                // initialize the engine
                List lDomains = new FastList();
                lDomains.add("http://jwebsocket.org");
                EngineConfiguration lEngineConfig = new EngineConfig(
                                "tcp0", // id
                                "org.jwebsocket.tcp.engines.TCPEngine", // name
                                "-", // jar, needs to be in classpath, i.e. embedded in .jar'/manifest
                                JWebSocketCommonConstants.DEFAULT_PORT, // unsecured nonssl-port
                                JWebSocketCommonConstants.DEFAULT_SSLPORT, // secured ssl-port
                                JWebSocketServerConstants.JWEBSOCKET_KEYSTORE, // default keystore file
                                JWebSocketServerConstants.JWEBSOCKET_KS_DEF_PWD, // default keystore file password
                                JWebSocketCommonConstants.JWEBSOCKET_DEF_CONTEXT, // context if such
                                JWebSocketCommonConstants.JWEBSOCKET_DEF_SERVLET, // servlet if such
                                JWebSocketCommonConstants.DEFAULT_TIMEOUT, // default session timeout
                                JWebSocketCommonConstants.DEFAULT_MAX_FRAME_SIZE, // max framesize
                                lDomains, // list of accepted domains
                                JWebSocketServerConstants.DEFAULT_MAX_CONNECTIONS, // max connections
                                JWebSocketServerConstants.DEFAULT_ON_MAX_CONNECTIONS_STRATEGY, // on max connections reached strategy
                                new FastMap());
                mEngine = new TCPEngine(lEngineConfig);

                // if engine could be instantiated properly...
                if (mEngine != null) {
                        // initialize the server
                        ServerConfiguration lServerConfig = new ServerConfig(
                                        "ts0", // id
                                        "org.jwebsocket.server.TokenServer", // name
                                        "-", // jar, needs to be in classpath, i.e. embedded in .jar'/manifest
                                        new FastMap());
                        mServer = new TokenServer(lServerConfig);

                        // link server and engine
                        mEngine.addServer(mServer);
                        mServer.addEngine(mEngine);

                        // add desired plug-ins
                        TokenPlugInChain lPlugInChain = mServer.getPlugInChain();
                        // the system plug-in is essential to process authentication
                        // send and broadcast
                        PluginConfiguration lPlugInConfig = new PluginConfig(
                                        "jws.system", // id
                                        "SystemPlugIn", // name
                                        "org.jwebsocket.plugins.system", // package
                                        "jWebSocketServer-1.0.jar", // jar
                                        "org.jwebsocket.plugins.system", // namespace
                                        lDomains, // list of accepted domains
                                        null, // settings
                                        true // enabled
                                        );
                        lPlugInChain.addPlugIn(new SystemPlugIn(lPlugInConfig));
                        // the FlashBrigde plug-in is strongly recommended to also support
                        // non websocket compliant browsers
                        lPlugInChain.addPlugIn(new FlashBridgePlugIn(null));
                }
        }

        public void start() {
                // start the jWebsocket Server
                try {
                        mEngine.startEngine();
                } catch (WebSocketException ex) {
                        mLog.error("Exception on starting jWebSocket engine: " + ex.getMessage());
                }
                try {
                        mServer.startServer();
                } catch (WebSocketException ex) {
                        mLog.error("Exception on starting jWebSocket server: " + ex.getMessage());
                }
        }

        public void run() {
                // wait until engine has terminated (e.g. by "shutdown" command)
                // TODO: Use JWebSocketInstance getState here!
                while (mEngine.isAlive()) {
                        try {
                                Thread.sleep(250);
                        } catch (InterruptedException ex) {
                                // no handling required here
                        }
                }
        }

        public void stop() {
                try {
                        // terminate all instances
                        mEngine.stopEngine(CloseReason.SERVER);
                } catch (WebSocketException ex) {
                        mLog.error("Exception on stopping jWebSocket subsystem: " + ex.getMessage());
                }
        }

        /**
         * adds a new listener to the server of the jWebSocket subsystem.
         *
         * @param aListener
         */
        public void addListener(WebSocketServerListener aListener) {
                if (aListener != null && mServer != null) {
                        // add listener to the server
                        mServer.addListener(aListener);
                }
        }

        /**
         * removes a listener from the server of the jWebSocket subsystem.
         *
         * @param aListener
         */
        public void removeListener(WebSocketServerListener aListener) {
                if (aListener != null && mServer != null) {
                        // remove listener from the server
                        mServer.removeListener(aListener);
                }
        }
}
