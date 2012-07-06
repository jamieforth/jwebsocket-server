//    ---------------------------------------------------------------------------
//    jWebSocket - Factory Singleton
//    Copyright (c) 2010 jWebSocket.org, Alexander Schulze, Innotrade GmbH
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
package org.jwebsocket.factory;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jwebsocket.api.*;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketConfig;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.instance.JWebSocketInstance;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.WebSocketException;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.spring.JWebSocketBeanFactory;
import org.jwebsocket.util.Tools;
import org.springframework.beans.factory.BeanFactory;

/**
 * Factory to initialize and start the jWebSocket components
 *
 * @author aschulze
 * @version $Id:$
 */
public class JWebSocketFactory {

    // don't instantiate logger here! first read args!
    private static Logger mLog = null;
    private static Map<String, WebSocketEngine> mEngines = null;
    private static List<WebSocketServer> mServers = null;
    private static TokenServer mTokenServer = null;
    private static BeanFactory mBeanFactory;
    private static JWebSocketJarClassLoader mClassLoader = null;

    /**
     *
     * @return The class loader used to load the system resources like
     * libraries, engines, plug-ins, ...
     */
    public static JWebSocketJarClassLoader getClassLoader() {
        return mClassLoader;
    }

    /**
     *
     * @param classLoader
     */
    
    public static void setClassLoader(JWebSocketJarClassLoader aClassLoader) {
        JWebSocketFactory.mClassLoader = aClassLoader;
    }
    
    
    /**
     *
     */
    public static void printCopyrightToConsole() {
        // the following 3 lines must not be removed due to GNU LGPL 3.0 license!
        System.out.println("jWebSocket Ver. "
                + JWebSocketServerConstants.VERSION_STR
                + " (" + System.getProperty("sun.arch.data.model") + "bit)");
        System.out.println(JWebSocketCommonConstants.COPYRIGHT);
        System.out.println(JWebSocketCommonConstants.LICENSE);
    }

    /**
     *
     */
    public static void start() {
        start(null, null);
    }

    /**
     *
     * @param aConfigPath
     * @param aBootstrapPath
     */
    public static void start(String aConfigPath, String aBootstrapPath) {

        mLog = Logging.getLogger();

        if (null == aConfigPath) {
            aConfigPath = JWebSocketConfig.getConfigPath();
        }
        if (null == aBootstrapPath) {
            aBootstrapPath = JWebSocketConfig.getBootstrapPath();
        }

        JWebSocketInstance.setStatus(JWebSocketInstance.STARTING);

        // start the shared utility timer
        Tools.startUtilityTimer();

        JWebSocketLoader lLoader = new JWebSocketLoader();

        // try to load bean from bootstrap
        try {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Loading bootstrap '" + aBootstrapPath + "'...");
            }
            JWebSocketBeanFactory.load(aBootstrapPath, Thread.currentThread().getContextClassLoader());
            if (mLog.isDebugEnabled()) {
                mLog.debug("Bootstrap '" + aBootstrapPath + "' successfully loaded.");
            }
        } catch (Exception lEx) {
            if (mLog.isDebugEnabled()) {
                mLog.debug(Logging.getSimpleExceptionMessage(lEx, "loading bootstrap."));
            }
        }

        // try to load configuration from .xml file
        try {
            WebSocketInitializer lInitializer =
                    lLoader.initialize(aConfigPath);

            if (lInitializer == null) {
                JWebSocketInstance.setStatus(JWebSocketInstance.SHUTTING_DOWN);
                return;
            }

            lInitializer.initializeLogging();

            if (mLog.isDebugEnabled()) {
                mLog.debug("Starting jWebSocket Server Sub System...");
            }

            // load and init all external libraries
            ClassLoader lClassLoader = lInitializer.initializeLibraries();
            if (lClassLoader != null) {
                JWebSocketConfig.setClassLoader(lClassLoader);
            }
            mEngines = lInitializer.initializeEngines();

            if (null == mEngines || mEngines.size() <= 0) {
                // the loader already logs an error!
                JWebSocketInstance.setStatus(JWebSocketInstance.SHUTTING_DOWN);
                return;
            }

            // initialize and start the server
            if (mLog.isDebugEnabled()) {
                mLog.debug("Initializing servers...");
            }
            mServers = lInitializer.initializeServers();
            Map<String, List<WebSocketPlugIn>> lPluginMap =
                    lInitializer.initializePlugins();

            if (mLog.isDebugEnabled()) {
                mLog.debug("Initializing plugins...");
            }

            for (WebSocketServer lServer : mServers) {
                for (WebSocketEngine lEngine : mEngines.values()) {
                    lServer.addEngine(lEngine);
                }
                List<WebSocketPlugIn> lPlugIns = lPluginMap.get(lServer.getId());
                for (WebSocketPlugIn lPlugIn : lPlugIns) {
                    lServer.getPlugInChain().addPlugIn(lPlugIn);
                }
                if (mLog.isInfoEnabled()) {
                    mLog.info(lPlugIns.size()
                            + " plugin(s) initialized for server '"
                            + lServer.getId() + "'.");
                }
            }
            Map<String, List<WebSocketFilter>> lFilterMap =
                    lInitializer.initializeFilters();


            if (mLog.isDebugEnabled()) {
                mLog.debug("Initializing filters...");
            }

            for (WebSocketServer lServer : mServers) {
                // lServer.addEngine(mEngine);
                List<WebSocketFilter> lFilters = lFilterMap.get(lServer.getId());
                for (WebSocketFilter lFilter : lFilters) {
                    lServer.getFilterChain().addFilter(lFilter);
                }
                if (mLog.isInfoEnabled()) {
                    mLog.info(lFilters.size()
                            + " filter(s) initialized for server '"
                            + lServer.getId() + "'.");
                }
            }
            boolean lEngineStarted = false;
            // first start the engine


            if (mLog.isDebugEnabled()) {
                String lEnginesStr = "";
                for (WebSocketEngine lEngine : mEngines.values()) {
                    lEnginesStr += lEngine.getId() + ", ";
                }
                if (lEnginesStr.length() > 0) {
                    lEnginesStr = lEnginesStr.substring(0, lEnginesStr.length() - 2);
                }
                mLog.debug("Starting engine(s) '" + lEnginesStr + "'...");
            }

            for (WebSocketEngine lEngine : mEngines.values()) {
                try {
                    lEngine.startEngine();
                    lEngineStarted = true;
                } catch (Exception lEx) {
                    mLog.error("Starting engine '" + lEngine.getId()
                            + "' failed (" + lEx.getClass().getSimpleName() + ": "
                            + lEx.getMessage() + ").");
                }
            }

            // do not start any servers no engine could be started
            if (lEngineStarted) {
                // now start the servers
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Starting servers...");
                }
                for (WebSocketServer lServer : mServers) {
                    try {
                        lServer.startServer();
                    } catch (Exception lEx) {
                        mLog.error("Starting server '" + lServer.getId()
                                + "' failed (" + lEx.getClass().getSimpleName()
                                + ": " + lEx.getMessage() + ").");
                    }
                }

                if (mLog.isInfoEnabled()) {
                    mLog.info("jWebSocket server startup complete");
                }

                // if everything went fine...
                JWebSocketInstance.setStatus(JWebSocketInstance.STARTED);
            } else {
                // if engine couldn't be started due to whatever reasons...
                JWebSocketInstance.setStatus(JWebSocketInstance.SHUTTING_DOWN);
            }
        } catch (WebSocketException lEx) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Exception during startup", lEx);
            }
            if (mLog != null && mLog.isInfoEnabled()) {
                mLog.info("jWebSocketServer failed to start.");
            }
            JWebSocketInstance.setStatus(JWebSocketInstance.SHUTTING_DOWN);
        }
    }

    /**
     *
     */
    public static void run() {
        // remain here until shut down request
        while (JWebSocketInstance.getStatus() != JWebSocketInstance.SHUTTING_DOWN) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException lEx) {
                // no handling required here
            }
        }

    }

    /**
     *
     */
    public static void stop() {

        // set instance status to not accept any new incoming connections
        JWebSocketInstance.setStatus(JWebSocketInstance.STOPPING);

        if (mLog != null && mLog.isDebugEnabled()) {
            mLog.debug("Stopping jWebSocket Sub System...");
        }

        if (null != mEngines) {
            for (WebSocketEngine lEngine : mEngines.values()) {
                // stop engine if previously started successfully
                if (lEngine != null) {
                    if (mLog != null && mLog.isDebugEnabled()) {
                        mLog.debug("Stopping engine...");
                    }
                    try {
                        lEngine.stopEngine(CloseReason.SHUTDOWN);
                        if (mLog != null && mLog.isInfoEnabled()) {
                            mLog.info("jWebSocket engine '" + lEngine.getId() + "' stopped.");
                        }
                    } catch (WebSocketException lEx) {
                        if (mLog != null) {
                            mLog.error("Stopping engine '" + lEngine.getId() + "': " + lEx.getMessage());
                        }
                    }
                }
            }
        }

        if (null != mServers) {
            // now stop the servers
            if (mLog != null && mLog.isDebugEnabled()) {
                mLog.debug("Stopping servers...");
            }
            for (WebSocketServer lServer : mServers) {
                try {
                    lServer.stopServer();
                    if (mLog != null && mLog.isInfoEnabled()) {
                        mLog.info("jWebSocket server '" + lServer.getId() + "' stopped.");
                    }
                } catch (WebSocketException lEx) {
                    if (mLog != null) {
                        mLog.error("Stopping server: " + lEx.getMessage());
                    }
                }
            }
        }

        if (null != mLog && mLog.isInfoEnabled()) {
            mLog.info("jWebSocket Server Sub System stopped.");
        }
        Logging.exitLogs();

        // stop the shared utility timer
        Tools.stopUtilityTimer();

        // set instance status
        JWebSocketInstance.setStatus(JWebSocketInstance.STOPPED);
    }

    /**
     *
     * @return
     */
    public static BeanFactory getBeans() {
        return mBeanFactory;
    }

    /**
     *
     * @param aCoreBeans
     */
    public static void setBeans(BeanFactory aCoreBeans) {
        mBeanFactory = aCoreBeans;
    }

    /**
     *
     * @return
     */
    public static Map<String, WebSocketEngine> getEngines() {
        return mEngines;
    }

    /**
     *
     * @return
     */
    public static WebSocketEngine getEngine(String aId) {
        return mEngines.get(aId);
    }

    /**
     *
     * @return
     */
    public static WebSocketEngine getEngine() {
        return mEngines.values().iterator().next();
    }

    /**
     *
     * @return
     */
    public static List<WebSocketServer> getServers() {
        return mServers;
    }

    /**
     * Returns the server identified by it's id or <tt>null</tt> if no server
     * with that id could be found in the factory.
     *
     * @param aId id of the server to be returned.
     * @return WebSocketServer with the given id or <tt>null</tt> if not found.
     */
    public static WebSocketServer getServer(String aId) {
        if (aId != null && mServers != null) {
            for (WebSocketServer lServer : mServers) {
                if (lServer != null && aId.equals(lServer.getId())) {
                    return lServer;
                }
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public static TokenServer getTokenServer() {
        if (mTokenServer == null) {
            mTokenServer = (TokenServer) getServer("ts0");
        }
        return mTokenServer;
    }
}
