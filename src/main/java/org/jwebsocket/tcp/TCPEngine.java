//    ---------------------------------------------------------------------------
//    jWebSocket - TCP Engine
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
package org.jwebsocket.tcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.log4j.Logger;
import org.jwebsocket.api.EngineConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketConfig;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.engines.BaseEngine;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.WebSocketException;
import org.jwebsocket.logging.Logging;

/**
 * Implementation of the jWebSocket TCP engine. The TCP engine provide a Java
 * Socket implementation of the WebSocket protocol. It contains the handshake
 *
 * @author aschulze
 * @author jang
 */
public class TCPEngine extends BaseEngine {

    private static Logger mLog = Logging.getLogger();
    private ServerSocket mTCPServerSocket = null;
    private SSLServerSocket mSSLServerSocket = null;
    private int mTCPListenerPort = JWebSocketCommonConstants.DEFAULT_PORT;
    private int mSSLListenerPort = JWebSocketCommonConstants.DEFAULT_SSLPORT;
    private int mSessionTimeout = JWebSocketCommonConstants.DEFAULT_TIMEOUT;
    private String mKeyStore = JWebSocketServerConstants.JWEBSOCKET_KEYSTORE;
    private String mKeyStorePassword = JWebSocketServerConstants.JWEBSOCKET_KS_DEF_PWD;
    private boolean mIsRunning = false;
    private boolean mEventsFired = false;
    private Thread mTCPEngineThread = null;
    private Thread mSSLEngineThread = null;

    public TCPEngine(EngineConfiguration aConfiguration) {
        super(aConfiguration);
        mTCPListenerPort = aConfiguration.getPort();
        mSSLListenerPort = aConfiguration.getSSLPort();
        mSessionTimeout = aConfiguration.getTimeout();
        mKeyStore = aConfiguration.getKeyStore();
        mKeyStorePassword = aConfiguration.getKeyStorePassword();
    }

    @Override
    public void startEngine()
            throws WebSocketException {

        // start timeout surveillance timer
        TimeoutOutputStreamNIOWriter.startTimer();

        setSessionTimeout(mSessionTimeout);

        // create unencrypted server socket for ws:// protocol
        if (mLog.isDebugEnabled()) {
            mLog.debug("Starting TCP engine '"
                    + getId()
                    + "' at port " + mTCPListenerPort
                    + " with default timeout "
                    + (mSessionTimeout > 0 ? mSessionTimeout + "ms" : "infinite")
                    + "...");
        }
        try {
            mTCPServerSocket = new ServerSocket(mTCPListenerPort);

            EngineListener lListener = new EngineListener(this, mTCPServerSocket);
            mTCPEngineThread = new Thread(lListener);
            mTCPEngineThread.start();

        } catch (IOException lEx) {
            throw new WebSocketException(lEx.getMessage());
        }

        // TODO: results in firing started event twice! make more clean!
        // super.startEngine();
        if (mLog.isInfoEnabled()) {
            mLog.info("TCP engine '"
                    + getId() + "' started' at port "
                    + mTCPListenerPort + " with default timeout "
                    + (mSessionTimeout > 0 ? mSessionTimeout + "ms" : "infinite")
                    + "...");
        }

        // tutorial see: http://javaboutique.internet.com/tutorials/jkey/index.html

        // create encrypted (SSL) server socket for wss:// protocol
        if (mSSLListenerPort > 0) {
            // create unencrypted server socket for ws:// protocol
            if (mLog.isDebugEnabled()) {
                mLog.debug("Trying to initiate SSL on port " + mSSLListenerPort + "...");
            }
            if (mKeyStore != null && !mKeyStore.isEmpty()
                    && mKeyStorePassword != null && !mKeyStorePassword.isEmpty()) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Using keystore " + mKeyStore + "...");
                    mLog.debug("Starting SSL engine '"
                            + getId()
                            + "' at port " + mSSLListenerPort + ","
                            + " with default timeout "
                            + (mSessionTimeout > 0 ? mSessionTimeout + "ms" : "infinite")
                            + "...");
                }
                try {
                    SSLContext lSSLContext = SSLContext.getInstance("TLS");
                    KeyManagerFactory lKMF = KeyManagerFactory.getInstance("SunX509");
                    KeyStore lKeyStore = KeyStore.getInstance("JKS");

                    String lKeyStorePath = JWebSocketConfig.expandEnvAndJWebSocketVars(mKeyStore);
                    if (lKeyStorePath != null) {
                        char[] lPassword = mKeyStorePassword.toCharArray();
                        URL lURL = JWebSocketConfig.getURLFromPath(lKeyStorePath);
                        lKeyStore.load(new FileInputStream(lURL.getPath()), lPassword);
                        lKMF.init(lKeyStore, lPassword);

                        lSSLContext.init(lKMF.getKeyManagers(), null, new java.security.SecureRandom());
                        SSLServerSocketFactory lSSLFactory = lSSLContext.getServerSocketFactory();
                        mSSLServerSocket = (SSLServerSocket) lSSLFactory.createServerSocket(
                                mSSLListenerPort);
                        // enable all protocols
                        mSSLServerSocket.setEnabledProtocols(mSSLServerSocket.getEnabledProtocols());
                        // enable all cipher suites
                        mSSLServerSocket.setEnabledCipherSuites(mSSLServerSocket.getSupportedCipherSuites());
                        EngineListener lSSLListener = new EngineListener(this, mSSLServerSocket);
                        mSSLEngineThread = new Thread(lSSLListener);
                        mSSLEngineThread.start();

                        if (mLog.isInfoEnabled()) {
                            mLog.info("SSL engine '"
                                    + getId() + "' started' at port "
                                    + mSSLListenerPort + " with default timeout "
                                    + (mSessionTimeout > 0
                                    ? mSessionTimeout + "ms" : "infinite")
                                    + ".");
                        }
                    } else {
                        mLog.error("SSL engine could not be instantiated: "
                                + "KeyStore '" + mKeyStore + "' not found.");
                    }
                } catch (Exception lEx) {
                    mLog.error(Logging.getSimpleExceptionMessage(lEx, "instantiating SSL engine"));
                }
            } else {
                mLog.error("SSL engine could not be instantiated due to missing configuration,"
                        + " please set sslport, keystore and password options.");
            }
        } else {
            mLog.info("No SSL engine configured,"
                    + " set sslport, keystore and password options if desired.");
        }
    }

    @Override
    public void stopEngine(CloseReason aCloseReason)
            throws WebSocketException {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Stopping TCP engine '" + getId()
                    + "' at port " + mTCPListenerPort + "...");
        }
        // resetting "isRunning" causes engine listener to terminate
        mIsRunning = false;
        long lStarted = new Date().getTime();

        // close unencrypted TCP server socket
        try {
            // when done, close server socket
            // closing the server socket should lead to an IOExeption
            // at accept in the listener thread which terminates the listener
            if (mTCPServerSocket != null && !mTCPServerSocket.isClosed()) {
                mTCPServerSocket.close();
                if (mLog.isInfoEnabled()) {
                    mLog.info("TCP engine '" + getId()
                            + "' stopped at port " + mTCPListenerPort
                            + " (closed=" + mTCPServerSocket.isClosed() + ").");
                }
                mTCPServerSocket = null;
            } else {
                mLog.warn("Stopping TCP engine '" + getId()
                        + "': no server socket or server socket closed.");
            }
        } catch (Exception lEx) {
            mLog.error(lEx.getClass().getSimpleName()
                    + " on stopping TCP engine '" + getId()
                    + "': " + lEx.getMessage());
        }

        // close encrypted SSL server socket
        try {
            // when done, close server socket
            // closing the server socket should lead to an IOExeption
            // at accept in the listener thread which terminates the listener
            if (mSSLServerSocket != null && !mSSLServerSocket.isClosed()) {
                mSSLServerSocket.close();
                if (mLog.isInfoEnabled()) {
                    mLog.info("SSL engine '" + getId()
                            + "' stopped at port " + mSSLListenerPort
                            + " (closed=" + mSSLServerSocket.isClosed() + ").");
                }
                mSSLServerSocket = null;
            } else {
                mLog.warn("Stopping SSL engine '" + getId()
                        + "': no server socket or server socket closed.");
            }
        } catch (Exception lEx) {
            mLog.error(lEx.getClass().getSimpleName()
                    + " on stopping SSL engine '" + getId()
                    + "': " + lEx.getMessage());
        }

        // stop TCP listener thread
        if (mTCPEngineThread != null) {
            try {
                // TODO: Make this timeout configurable one day
                mTCPEngineThread.join(10000);
            } catch (Exception lEx) {
                mLog.error(lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
            }
            if (mLog.isDebugEnabled()) {
                long lDuration = new Date().getTime() - lStarted;
                if (mTCPEngineThread.isAlive()) {
                    mLog.warn("TCP engine '" + getId()
                            + "' did not stop after " + lDuration + "ms.");
                } else {
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("TCP engine '" + getId()
                                + "' stopped after " + lDuration + "ms.");
                    }
                }
            }
        }

        // stop SSL listener thread
        if (mSSLEngineThread != null) {
            try {
                // TODO: Make this timeout configurable one day
                mSSLEngineThread.join(10000);
            } catch (Exception lEx) {
                mLog.error(lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
            }
            if (mLog.isDebugEnabled()) {
                long lDuration = new Date().getTime() - lStarted;
                if (mSSLEngineThread.isAlive()) {
                    mLog.warn("SSL engine '" + getId()
                            + "' did not stop after " + lDuration + "ms.");
                } else {
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("SSL engine '" + getId()
                                + "' stopped after " + lDuration + "ms.");
                    }
                }
            }
        }

        // inherited method stops all connectors
        lStarted = new Date().getTime();
        int lNumConns = getConnectors().size();
        super.stopEngine(aCloseReason);

        // now wait until all connectors have been closed properly
        // or timeout exceeds...
        try {
            while (getConnectors().size() > 0
                    && new Date().getTime() - lStarted < 10000) {
                Thread.sleep(250);
            }
        } catch (Exception lEx) {
            mLog.error(lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
        }
        if (mLog.isDebugEnabled()) {
            long lDuration = new Date().getTime() - lStarted;
            int lRemConns = getConnectors().size();
            if (lRemConns > 0) {
                mLog.warn(lRemConns + " of " + lNumConns
                        + " TCP connectors '" + getId()
                        + "' did not stop after " + lDuration + "ms.");
            } else {
                if (mLog.isDebugEnabled()) {
                    mLog.debug(lNumConns
                            + " TCP connectors '" + getId()
                            + "' stopped after " + lDuration + "ms.");
                }
            }
        }

        // stop timeout surveillance timer
        TimeoutOutputStreamNIOWriter.stopTimer();
    }

    @Override
    public void connectorStarted(WebSocketConnector aConnector) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Detected new connector at port " + aConnector.getRemotePort() + ".");
        }
        super.connectorStarted(aConnector);
    }

    @Override
    public void connectorStopped(WebSocketConnector aConnector, CloseReason aCloseReason) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Detected stopped connector at port " + aConnector.getRemotePort() + ".");
        }
        super.connectorStopped(aConnector, aCloseReason);
    }

    @Override
    /*
     * Returns {@code true} if the TCP engine is running or {@code false}
     * otherwise. The alive status represents the state of the TCP engine
     * listener thread.
     */
    public boolean isAlive() {
        return (mTCPEngineThread != null && mTCPEngineThread.isAlive());
    }

    private class EngineListener implements Runnable {

        private WebSocketEngine mEngine = null;
        private ServerSocket mServer = null;

        /**
         * Creates the server socket listener for new incoming socket
         * connections.
         *
         * @param aEngine
         */
        public EngineListener(WebSocketEngine aEngine, ServerSocket aServerSocket) {
            mEngine = aEngine;
            mServer = aServerSocket;
        }

        @Override
        public void run() {
            Thread.currentThread().setName(
                    "jWebSocket TCP-Engine (" + mServer.getLocalPort() + ", "
                    + (mServer instanceof SSLServerSocket
                    ? "SSL secured)"
                    : "non secured)"));

            // notify server that engine has started
            if (!mEventsFired) {
                mEventsFired = true;
                engineStarted();
            }

            mIsRunning = true;
            while (mIsRunning) {

                try {
                    // accept is blocking so here is no need
                    // to put any sleeps into this loop
                    // if (log.isDebugEnabled()) {
                    //    log.debug("Waiting for client...");
                    // }
                    Socket lClientSocket = null;
                    boolean lReject = false;
                    boolean lRedirect = false;
                    String lOnMaxConnectionStrategy = mEngine.getConfiguration().getOnMaxConnectionStrategy();

                    // Accept new connections only if the maximun number of connections
                    // has not been reached
                    if ("wait".equals(lOnMaxConnectionStrategy)) {
                        if (mEngine.getConnectors().size() >= mEngine.getMaxConnections()) {
                            Thread.sleep(1000);
                            continue;
                        } else {
                            lClientSocket = mServer.accept();
                        }
                    } else if ("close".equals(lOnMaxConnectionStrategy)) {
                        lClientSocket = mServer.accept();
                        if (mEngine.getConnectors().size() >= mEngine.getMaxConnections()) {
                            if (mLog.isDebugEnabled()) {
                                mLog.debug("Closing incoming socket client on  port '" + lClientSocket.getPort() + "' "
                                        + "because the maximum number of connections "
                                        + "has been reached...");
                            }

                            lClientSocket.close();
                            continue;
                        }
                    } else if ("reject".equals(lOnMaxConnectionStrategy)) {
                        lClientSocket = mServer.accept();
                        if (mEngine.getConnectors().size() >= mEngine.getMaxConnections()) {
                            lReject = true;
                        }
                    } else if ("redirect".equals(lOnMaxConnectionStrategy)) {
                        lClientSocket = mServer.accept();
                        if (mEngine.getConnectors().size() >= mEngine.getMaxConnections()) {
                            lRedirect = true;
                        }
                    }

                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Client trying to connect on port #"
                                + lClientSocket.getPort() + "...");
                    }

                    try {
                        WebSocketConnector lConnector = new TCPConnector(mEngine, lClientSocket);

                        // Check for maximum connections reached strategies
                        if (lReject) {
                            if (mLog.isDebugEnabled()) {
                                mLog.debug("Rejecting incoming connector '"
                                        + lConnector.getId() + "' "
                                        + "because the maximum number of connections "
                                        + "has been reached.");
                            }
                            lConnector.stopConnector(CloseReason.SERVER_REJECT_CONNECTION);

                            continue;
                        } else if (lRedirect) {
                            // TODO: Pending for implementation to discover the redirection
                            // server URL

                            if (mLog.isDebugEnabled()) {
                                mLog.debug("Redirecting incoming connector '" + lConnector.getId() + "' "
                                        + "because the maximum number of connections "
                                        + "has been reached.");
                            }
                            lConnector.stopConnector(CloseReason.SERVER_REDIRECT_CONNECTION);

                            continue;
                        } else {
                            // log.debug("Adding connector to engine...");
                            getConnectors().put(lConnector.getId(), lConnector);
                            //Starting new connection
                            lConnector.startConnector();
                        }
                    } catch (Exception lEx) {
                        mLog.error(
                                (mServer instanceof SSLServerSocket
                                ? "SSL" : "TCP") + " engine: "
                                + lEx.getClass().getSimpleName()
                                + ": " + lEx.getMessage());
                    }
                } catch (Exception lEx) {
                    if (mIsRunning) {
                        mIsRunning = false;
                        mLog.error(
                                (mServer instanceof SSLServerSocket ? "SSL" : "TCP")
                                + " engine: "
                                + lEx.getClass().getSimpleName()
                                + ": " + lEx.getMessage());
                    } else {
                        if (mLog.isInfoEnabled()) {
                            mLog.info(
                                    (mServer instanceof SSLServerSocket ? "SSL" : "TCP")
                                    + " engine: "
                                    + "Server listener thread stopped.");
                        }
                    }
                }
            }

            // notify server that engine has stopped
            // this closes all connections
            if (mEventsFired) {
                mEventsFired = false;
                engineStopped();
            }
        }
    }
}
