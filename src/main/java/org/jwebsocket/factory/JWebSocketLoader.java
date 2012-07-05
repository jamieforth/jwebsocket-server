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
package org.jwebsocket.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketInitializer;
import org.jwebsocket.config.JWebSocketConfig;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.config.xml.JWebSocketConfigHandler;
import org.jwebsocket.kit.WebSocketException;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.security.SecurityFactory;
import org.jwebsocket.util.Tools;

/**
 * An object that does the process of loading configuration, initialization of
 * the jWebSocket server system.
 *
 * @author puran
 * @version $Id: JWebSocketLoader.java 345 2010-04-10 20:03:48Z fivefeetfurther$
 */
public final class JWebSocketLoader {

        private static Logger mLog = Logging.getLogger();
        ;
        private JWebSocketConfigHandler mConfigHandler = new JWebSocketConfigHandler();

        /**
         * Initialize the jWebSocket Server system
         *
         * @param aConfigPath the overridden path to xml config file
         * @return the initializer object
         * @throws WebSocketException if there's an exception while initialization
         */
        public final WebSocketInitializer initialize(String aConfigPath)
                        throws WebSocketException {
                String lConfigPath;
                if (aConfigPath != null && !"".equals(aConfigPath)) {
                        lConfigPath = Tools.expandEnvVarsAndProps(aConfigPath);
                        if (mLog.isDebugEnabled()) {
                                mLog.debug("Initializing: Using config file '" + aConfigPath + "'...");
                        }
                } else {
                        lConfigPath = JWebSocketConfig.getConfigPath();
                }
                if (lConfigPath == null) {
                        throw new WebSocketException("Either "
                                        + JWebSocketServerConstants.JWEBSOCKET_HOME
                                        + " variable is not set"
                                        + " or jWebSocket.xml file does neither exist at ${"
                                        + JWebSocketServerConstants.JWEBSOCKET_HOME
                                        + "}/conf nor at ${CLASSPATH}/conf.");
                }
                // load the entire settings from the configuration xml file
                JWebSocketConfig lConfig = loadConfiguration(lConfigPath);

                // initialize security by using config settings
                SecurityFactory.initFromConfig(lConfig);
                WebSocketInitializer lInitializer = new JWebSocketXmlConfigInitializer(lConfig);
                return lInitializer;
        }

        /**
         * Load all the configurations based on jWebSocket.xml file at the given
         * <tt>configFilePath</tt> location.
         *
         * @param aConfigPath the path to jWebSocket.xml file
         * @return the web socket config object with all the configuration
         * @throws WebSocketException if there's any while loading configuration
         */
        public JWebSocketConfig loadConfiguration(final String aConfigPath) throws WebSocketException {
                JWebSocketConfig lConfig = null;
                String lMsg;
                try {
                        InputStream lIS;
                        if (JWebSocketConfig.isLoadConfigFromResource()) {
                                if (mLog.isDebugEnabled()) {
                                        mLog.debug("Loading configuration from resource '" + aConfigPath + "...");
                                }
                                lIS = this.getClass().getResourceAsStream("/" + aConfigPath);
                        } else {
                                if (mLog.isDebugEnabled()) {
                                        mLog.debug("Loading configuration from file '" + aConfigPath + "...");
                                }
                                File lFile = new File(aConfigPath);
                                lIS = new FileInputStream(lFile);
                        }
                        XMLInputFactory lFactory = XMLInputFactory.newInstance();
                        XMLStreamReader lStreamReader = null;
                        lStreamReader = lFactory.createXMLStreamReader(lIS);
                        lConfig = (JWebSocketConfig) mConfigHandler.processConfig(lStreamReader);
                        if (mLog.isInfoEnabled()) {
                                mLog.info("Configuration successfully loaded from '" + aConfigPath + "'.");
                        }
                } catch (XMLStreamException lEx) {
                        lMsg = lEx.getClass().getSimpleName() + " occurred while creating XML stream (" + aConfigPath + "): " + lEx.getMessage() + ".";
                        throw new WebSocketException(lMsg);
                } catch (FileNotFoundException lEx) {
                        lMsg = "jWebSocket config file not found while creating XML stream (" + aConfigPath + "): " + lEx.getMessage() + ".";
                        throw new WebSocketException(lMsg);
                }
                return lConfig;
        }
}
