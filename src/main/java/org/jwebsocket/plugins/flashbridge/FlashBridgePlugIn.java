//    ---------------------------------------------------------------------------
//    jWebSocket - FlashBridge Plug-In
//    Copyright (c) 2010 Innotrade GmbH (http://jWebSocket.org)
//    ---------------------------------------------------------------------------
//    This program is free software; you can redistribute it and/or modify it
//    under the terms of the GNU Lesser General Public License as published by the
//    Free Software Foundation; either version 3 of the License, or (at your
//    option) any later version.
//    This program is distributed in the hope that it will be useful, but WITHOUT
//    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//    FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
//  for more details.
//    You should have received a copy of the GNU Lesser General Public License along
//    with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//    ---------------------------------------------------------------------------
package org.jwebsocket.plugins.flashbridge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.config.JWebSocketConfig;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.plugins.TokenPlugIn;

/**
 * This plug-in processes the policy-file-request from the browser side flash
 * plug-in. This makes jWebSocket cross-browser-compatible.
 *
 * @author aschulze
 */
public class FlashBridgePlugIn extends TokenPlugIn {

    private static Logger mLog = Logging.getLogger();
    private ServerSocket mServerSocket = null;
    private int mListenerPort = 843;
    private boolean mIsRunning = false;
    private int mEngineInstanceCount = 0;
    private BridgeProcess mBridgeProcess = null;
    private Thread mBridgeThread = null;
    private final static String PATH_TO_CROSSDOMAIN_XML = "crossdomain_xml";
    private static String mCrossDomainXML =
            "<cross-domain-policy>"
            + "<allow-access-from domain=\"*\" to-ports=\"*\" />"
            + "</cross-domain-policy>";
    
    /**
     *
     * @param aConfiguration
     */
    public FlashBridgePlugIn(PluginConfiguration aConfiguration) {
        super(aConfiguration);
        if (mLog.isDebugEnabled()) {
            mLog.debug("Instantiating FlashBridge plug-in...");
        }

        mGetSettings();

        try {
            mServerSocket = new ServerSocket(mListenerPort);

            mBridgeProcess = new BridgeProcess(this);
            mBridgeThread = new Thread(mBridgeProcess);
            // FindBug:
            // The constructor starts a thread. This is likely to be wrong if
            // the class is ever extended/subclassed, since the thread will be
            // started before the subclass constructor is started.     
            mBridgeThread.start();
            if (mLog.isInfoEnabled()) {
                mLog.info("FlashBridge plug-in successfully instantiated.");
            }
        } catch (IOException lEx) {
            mLog.error("FlashBridge could not be started: " + lEx.getMessage());
        }
    }

    private void mGetSettings() {
        // load global settings, default to "true"
        String lPathToCrossDomainXML = getString(PATH_TO_CROSSDOMAIN_XML);
        if (lPathToCrossDomainXML != null) {
            try {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Trying to load " + lPathToCrossDomainXML + "...");
                }
                lPathToCrossDomainXML = JWebSocketConfig.expandEnvAndJWebSocketVars(lPathToCrossDomainXML);
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Trying to load expanded " + lPathToCrossDomainXML + "...");
                }
                URL lURL = JWebSocketConfig.getURLFromPath(lPathToCrossDomainXML);
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Trying to load from URL " + lURL + "...");
                }
                File lFile = new File(lURL.getPath());
                mCrossDomainXML = FileUtils.readFileToString(lFile, "UTF-8");
                if (mLog.isInfoEnabled()) {
                    mLog.info("crossdomain config successfully loaded from " + lPathToCrossDomainXML + ".");
                }
            } catch (IOException lEx) {
                mLog.error(lEx.getClass().getSimpleName()
                        + " reading crossdomain.xml: " + lEx.getMessage());
            }
        }
    }

    private class BridgeProcess implements Runnable {

        private final FlashBridgePlugIn mPlugIn;

        /**
         * creates the server socket bridgeProcess for new incoming socket
         * connections.
         *
         * @param aPlugIn
         */
        public BridgeProcess(FlashBridgePlugIn aPlugIn) {
            // FindBug: This field is never read.  Consider removing it from the class.
            mPlugIn = aPlugIn;
        }

        @Override
        public void run() {

            if (mLog.isDebugEnabled()) {
                mLog.debug("Starting FlashBridge process...");
            }
            mIsRunning = true;
            Thread.currentThread().setName("jWebSocket FlashBridge");
            while (mIsRunning) {
                try {
                    // accept is blocking so here is no need
                    // to put any sleeps into the loop
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Waiting on flash policy-file-request on port "
                                + mServerSocket.getLocalPort() + "...");
                    }
                    Socket lClientSocket = mServerSocket.accept();
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Client connected...");
                    }
                    try {
                        // clientSocket.setSoTimeout(TIMEOUT);
                        InputStream lIS = lClientSocket.getInputStream();
                        OutputStream lOS = lClientSocket.getOutputStream();
                        byte[] lBA = new byte[4096];
                        String lLine = "";
                        boolean lFoundPolicyFileRequest = false;
                        int lLen = 0;
                        while (lLen >= 0 && !lFoundPolicyFileRequest) {
                            lLen = lIS.read(lBA);
                            if (lLen > 0) {
                                lLine += new String(lBA, 0, lLen, "US-ASCII");
                            }
                            if (mLog.isDebugEnabled()) {
                                mLog.debug("Received " + lLine + "...");
                            }
                            lFoundPolicyFileRequest =
                                    lLine.indexOf("policy-file-request") >= 0; // "<policy-file-request/>"
                        }
                        if (lFoundPolicyFileRequest) {
                            if (mLog.isDebugEnabled()) {
                                mLog.debug("Answering on flash policy-file-request (" + lLine + ")...");
                                // mLog.debug("Answer: " + mCrossDomainXML);
                            }
                            lOS.write(mCrossDomainXML.getBytes("UTF-8"));
                            lOS.flush();
                        } else {
                            mLog.warn("Received invalid policy-file-request (" + lLine + ")...");
                        }
                    } catch (Exception lEx) {
                        mLog.error(lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
                    }

                    lClientSocket.close();
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Client disconnected...");
                    }
                } catch (IOException lEx) {
                    if (mIsRunning) {
                        mIsRunning = false;
                        mLog.error(lEx.getClass().getSimpleName() + ": " + lEx.getMessage());
                    }
                    // otherwise closing the socket was intended, 
                    // no error in this case!
                }
            }
            if (mLog.isInfoEnabled()) {
                mLog.info("FlashBridge process stopped.");
            }
        }
    }

    @Override
    public void engineStarted(WebSocketEngine aEngine) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Engine '" + aEngine.getId() + "' started.");
        }
        // every time an engine starts increment counter
        mEngineInstanceCount++;
    }

    @Override
    public void engineStopped(WebSocketEngine aEngine) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Engine '" + aEngine.getId() + "' stopped.");
        }
        // every time an engine starts decrement counter
        mEngineInstanceCount--;
        // when last engine stopped also stop the FlashBridge
        if (mEngineInstanceCount <= 0) {
            super.engineStopped(aEngine);

            mIsRunning = false;
            long lStarted = new Date().getTime();

            try {
                // when done, close server socket
                // closing the server socket should lead to an exception
                // at accept in the bridgeProcess thread which terminates the
                // bridgeProcess
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Closing FlashBridge server socket...");
                }
                mServerSocket.close();
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Closed FlashBridge server socket.");
                }
            } catch (IOException ex) {
                mLog.error("(accept) " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }

            try {
                mBridgeThread.join(10000);
            } catch (InterruptedException ex) {
                mLog.error(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
            if (mLog.isDebugEnabled()) {
                long lDuration = new Date().getTime() - lStarted;
                if (mBridgeThread.isAlive()) {
                    mLog.warn("FlashBridge did not stopped after " + lDuration + "ms.");
                } else {
                    mLog.debug("FlashBridge stopped after " + lDuration + "ms.");
                }
            }
        }
    }
}
