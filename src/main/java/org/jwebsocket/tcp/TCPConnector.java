//    ---------------------------------------------------------------------------
//    jWebSocket - TCP Connector
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

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLSocket;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketConnectorStatus;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.connectors.BaseConnector;
import org.jwebsocket.kit.*;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.util.Tools;

/**
 * Implementation of the jWebSocket TCP socket connector.
 *
 * @author aschulze
 * @author jang
 */
public class TCPConnector extends BaseConnector {

    private static Logger mLog = Logging.getLogger();
    private InputStream mIn = null;
    private OutputStream mOut = null;
    private Socket mClientSocket = null;
    private static final int CONNECT_TIMEOUT = 2000;
    /**
     *
     */
    public static final String TCP_LOG = "TCP";
    /**
     *
     */
    public static final String SSL_LOG = "SSL";
    private String mLogInfo = TCP_LOG;
    private CloseReason mCloseReason = CloseReason.TIMEOUT;
    private Thread mClientThread = null;
    private TimeoutOutputStreamNIOWriter mOutputStreamNIOSender;

    /**
     * creates a new TCP connector for the passed engine using the passed client
     * socket. Usually connectors are instantiated by their engine only, not by
     * the application.
     *
     * @param aEngine
     * @param aClientSocket
     */
    public TCPConnector(WebSocketEngine aEngine, Socket aClientSocket) {
        super(aEngine);
        mClientSocket = aClientSocket;
        setSSL(mClientSocket instanceof SSLSocket);
        mLogInfo = isSSL() ? SSL_LOG : TCP_LOG;
        try {
            mIn = mClientSocket.getInputStream();
            mOut = mClientSocket.getOutputStream();

            // @TODO: Make the timeout constructor argument configurable for the future
            mOutputStreamNIOSender = new TimeoutOutputStreamNIOWriter(this, mIn, mOut, 1000);
        } catch (IOException lEx) {
            mLog.error(lEx.getClass().getSimpleName()
                    + " instantiating "
                    + getClass().getSimpleName() + ": "
                    + lEx.getMessage());
        }
    }

    @Override
    public void startConnector() {
        int lPort = -1;
        int lTimeout = -1;
        try {
            lPort = mClientSocket.getPort();
            lTimeout = mClientSocket.getSoTimeout();
        } catch (IOException lEx) {
            // FindBug: This method might ignore an exception.
            // In general, exceptions should be handled or reported in some way,
            // or they should be thrown out of the method.
        }
        String lNodeStr = getNodeId();
        if (lNodeStr != null) {
            lNodeStr = " (unid: " + lNodeStr + ")";
        } else {
            lNodeStr = "";
        }
        if (mLog.isDebugEnabled()) {
            mLog.debug("Starting " + mLogInfo + " connector" + lNodeStr + " on port "
                    + lPort + " with timeout "
                    + (lTimeout > 0 ? lTimeout + "ms" : "infinite") + "");
        }
        // pass reference to connector instance to reader thread
        ClientProcessor lClientProc = new ClientProcessor(this);
        mClientThread = new Thread(lClientProc);
        mClientThread.start();
        if (mLog.isInfoEnabled()) {
            mLog.info("Started " + mLogInfo + " connector" + lNodeStr + " on port "
                    + lPort + " with timeout "
                    + (lTimeout > 0 ? lTimeout + "ms" : "infinite") + "");
        }
    }

    /**
     * This closes all streams, the client socket and shuts down the tread.
     *
     * @param aCloseReason
     */
    private void terminateConnector(CloseReason aCloseReason) {
        setStatus(WebSocketConnectorStatus.DOWN);
        int lPort = mClientSocket.getPort();
        try {
            mOut.close();
        } catch (IOException lEx) {
            mLog.error(lEx.getClass().getSimpleName()
                    + " while closing outbound stream for " + mLogInfo
                    + " connector (" + aCloseReason.name()
                    + ") on port " + lPort + ": " + lEx.getMessage());
        }
        try {
            mIn.close();
        } catch (IOException lEx) {
            mLog.error(lEx.getClass().getSimpleName()
                    + " while closing inbound stream for " + mLogInfo
                    + " connector (" + aCloseReason.name()
                    + ") on port " + lPort + ": " + lEx.getMessage());
        }
        try {
            if (!mClientSocket.isClosed()) {
                mClientSocket.close();
            }
        } catch (IOException lEx) {
            mLog.error(lEx.getClass().getSimpleName()
                    + " while closing socket " + mLogInfo
                    + " connector (" + aCloseReason.name()
                    + ") on port " + lPort + ": " + lEx.getMessage());
        }
    }

    @Override
    public void stopConnector(CloseReason aCloseReason) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Stopping " + mLogInfo
                    + " connector (" + aCloseReason.name() + ")...");
        }
        mCloseReason = aCloseReason;
        synchronized (getWriteLock()) {
            if (!isHixie()) {
                // Hybi specs demand that client must be notified
                // with CLOSE control message before disconnect

                // @TODO the close reason has to be notified to the client
                // following the WebSocket protocol specification for this
                // @see http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17#page-45
                WebSocketPacket lClose;
                lClose = new RawPacket(WebSocketFrameType.CLOSE, WebSocketProtocolAbstraction.calcCloseData(1000, aCloseReason.name()));
                WebSocketConnectorStatus lStatus = getStatus();
                try {
                    // to ensure that the close packet can be sent at all!
                    setStatus(WebSocketConnectorStatus.UP);
                    sendPacketInTransaction(lClose);
                } catch (WebSocketException ex) {
                    // TODO: check if we need to handle that here!
                } finally {
                    setStatus(lStatus);
                }
            }
            stopReader();
        }
        // the connector is removed in the read thread once terminated
    }

    public void stopReader() {
        try {
            // force input stream to close to terminate reader thread
            if (!mClientSocket.isInputShutdown()
                    && !mClientSocket.isClosed()) {
                if (!(mClientSocket instanceof SSLSocket)) {
                    mClientSocket.shutdownInput();
                }
            }
        } catch (IOException lEx) {
            mLog.error(Logging.getSimpleExceptionMessage(lEx, "shutting down reader stream (" + getId() + ")"));
        }
        try {
            // force input stream to close to terminate reader thread
            mIn.close();
        } catch (Exception lEx) {
            mLog.error(Logging.getSimpleExceptionMessage(lEx, "closing reader stream (" + getId() + ")"));
        }
    }

    @Override
    public void processPacket(WebSocketPacket aDataPacket) {
        // forward the data packet to the engine
        getEngine().processPacket(this, aDataPacket);
    }

    @Override
    public void sendPacketInTransaction(WebSocketPacket aDataPacket) throws WebSocketException {
        if (WebSocketConnectorStatus.UP != getStatus()) {
            // TODO: think about if and how to handle the scenario 
            // that other threads send data to a closed or closing connector.
            /*
             * mLog.warn("Trying to send to closing connection: " + getId() + ",
             * " + aDataPacket.getUTF8());
             */
            return;
        }
        boolean lSendSuccess = false;
        String lExMsg = null;
        try {
            if (mClientSocket.isConnected()) {
                if (isHixie()) {
                    sendHixie(aDataPacket);
                } else {
                    sendHybi(getVersion(), aDataPacket);
                }
                lSendSuccess = true;
            } else {
                mLog.error("Trying to send to closed connection: "
                        + getId() + ", " + aDataPacket.getUTF8());
            }
        } catch (IOException lEx) {
            lExMsg = lEx.getMessage();
        }
        // if sending data leads to an exception ...
        // we need to terminate the connection
        if (!lSendSuccess) {
            stopReader();
        }
        if (null != lExMsg) {
            throw new WebSocketException(lExMsg);
        }
    }

    /*
     * this is called by the TimeoutOutputstreamWriter, the data is first
     * written into a queue and then send by a watched thread
     */
    public synchronized void _sendPacket(WebSocketPacket aDataPacket) {
        try {
            if (isHixie()) {
                sendHixie(aDataPacket);
            } else {
                sendHybi(getVersion(), aDataPacket);
            }
            mOut.flush();
        } catch (IOException lEx) {
            // in case a socket gets closed due to a timeout
            // in a write operation, this is not necessarily an error.
            // TODO: think how to eventually better deal with this.
            /*
             * mLog.error(lEx.getClass().getSimpleName() + " sending data
             * packet: " + lEx.getMessage());
             */
        }
    }

    @Override
    public void sendPacket(WebSocketPacket aDataPacket) {
        mOutputStreamNIOSender.sendPacket(aDataPacket);
        // _sendPacket(aDataPacket);
    }

    @Override
    public IOFuture sendPacketAsync(WebSocketPacket aDataPacket) {
        throw new UnsupportedOperationException("Underlying connector:"
                + getClass().getName()
                + " doesn't support asynchronous send operation");
    }

    private void sendHixie(WebSocketPacket aDataPacket) throws IOException {
        // exception handling is done in sendPacket method
        if (aDataPacket.getFrameType() == WebSocketFrameType.BINARY) {
            // each packet is enclosed in 0xFF<length><data>
            // TODO: for future use! Not yet finally spec'd in IETF drafts!
            mOut.write(0xFF);
            byte[] lBA = aDataPacket.getByteArray();
            // TODO: implement multi byte length!
            mOut.write(lBA.length);
            mOut.write(lBA);
            mOut.flush();
        } else {
            // each packet is enclosed in 0x00<data>0xFF
            mOut.write(0x00);
            mOut.write(aDataPacket.getByteArray());
            mOut.write(0xFF);
            mOut.flush();
        }
    }

    // TODO: implement fragmentation for packet sending
    private void sendHybi(int aVersion, WebSocketPacket aDataPacket) throws IOException {
        // exception handling is done in sendPacket method
        byte[] lPacket = WebSocketProtocolAbstraction.rawToProtocolPacket(aVersion, aDataPacket);
        mOut.write(lPacket);
        mOut.flush();
    }

    private RequestHeader processHandshake(Socket aClientSocket)
            throws UnsupportedEncodingException, IOException {

        InputStream lIn = aClientSocket.getInputStream();
        OutputStream lOut = aClientSocket.getOutputStream();

        ByteArrayOutputStream lBAOS = new ByteArrayOutputStream(4096);
        byte[] lBuff = new byte[4096];
        int lRead = 0, lTotal = 0;
        long lStart = System.currentTimeMillis();
        do {
            lRead = lIn.read(lBuff, 0, lBuff.length);
            if (lRead != -1) {
                lTotal += lRead;
                lBAOS.write(lBuff, 0, lRead);
                String lAsStr = lBAOS.toString();
                if (null != lAsStr
                        && lAsStr.indexOf("\r\n\r\n") > 0) {
                    break;
                }
            } else {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                }
            }
        } while (System.currentTimeMillis() - lStart < CONNECT_TIMEOUT);
        if (lTotal <= 0) {
            mLog.warn("Connection "
                    + aClientSocket.getInetAddress() + ":"
                    + aClientSocket.getPort()
                    + " did not detect initial handshake (total bytes read: " + lTotal + ").");
            return null;
        }
        byte[] lReq = lBAOS.toByteArray();

        /*
         * please keep comment for debugging purposes!
         */
        if (mLog.isDebugEnabled()) {
            mLog.debug("Parsing handshake request: " + new String(lReq).replace("\r\n", "\\n"));
        }
        Map lReqMap = WebSocketHandshake.parseC2SRequest(
                lReq, aClientSocket instanceof SSLSocket);
        if (lReqMap == null) {
            return null;
        }

        EngineUtils.parseCookies(lReqMap);
        //Setting the session identifier cookie if not present previously
        if (!((Map) lReqMap.get(RequestHeader.WS_COOKIES)).containsKey(JWebSocketCommonConstants.SESSIONID_COOKIE_NAME)) {
            ((Map) lReqMap.get(RequestHeader.WS_COOKIES)).put(JWebSocketCommonConstants.SESSIONID_COOKIE_NAME, Tools.getMD5(UUID.randomUUID().toString()));
        }

        RequestHeader lHeader = EngineUtils.validateC2SRequest(
                getEngine().getConfiguration().getDomains(), lReqMap, mLog);
        if (lHeader == null) {
            return null;
        }

        // generate the websocket handshake
        // if policy-file-request is found answer it
        byte[] lBA = WebSocketHandshake.generateS2CResponse(lReqMap);
        if (lBA == null) {
            if (mLog.isDebugEnabled()) {
                mLog.warn("TCPEngine detected illegal handshake.");
            }
            return null;
        }

        /*
         * please keep comment for debugging purposes!
         */
        if (mLog.isDebugEnabled()) {
            mLog.debug("Flushing handshake response: " + new String(lBA).replace("\r\n", "\\n"));
            // mLog.debug("Flushing initial WebSocket handshake...");
        }

        lOut.write(lBA);
        lOut.flush();

        // maybe the request is a flash policy-file-request
        String lFlashBridgeReq = (String) lReqMap.get("policy-file-request");
        if (lFlashBridgeReq != null) {
            mLog.warn("TCPEngine returned policy file request ('"
                    + lFlashBridgeReq
                    + "'), check for FlashBridge plug-in.");
        }


        // if we detected a flash policy-file-request return "null"
        // (no websocket header detected)
        if (lFlashBridgeReq != null) {
            mLog.warn("TCP Engine returned policy file response ('"
                    + new String(lBA, "US-ASCII")
                    + "'), check for FlashBridge plug-in.");
            return null;
        }

        if (mLog.isDebugEnabled()) {
            mLog.debug("Handshake flushed.");
        }

        return lHeader;
    }

    private class ClientProcessor implements Runnable {

        private WebSocketConnector mConnector = null;

        /**
         * Creates the new socket listener thread for this connector.
         *
         * @param aConnector
         */
        public ClientProcessor(WebSocketConnector aConnector) {
            mConnector = aConnector;
        }

        @Override
        public void run() {
            WebSocketEngine lEngine = getEngine();
            Thread.currentThread().setName("jWebSocket " + mLogInfo + "-Connector " + getId());

            // start client listener loop
            setStatus(WebSocketConnectorStatus.UP);
            mCloseReason = CloseReason.SERVER;
            int lPort = mClientSocket.getPort();

            String lLogInfo = isSSL() ? "SSL" : "TCP";
            boolean lOk = false;
            try {
                boolean lTCPNoDelay = mClientSocket.getTcpNoDelay();
                // ensure that all packets are sent immediately w/o delay
                // to achieve better latency, no waiting and packaging.
                mClientSocket.setTcpNoDelay(true);

                RequestHeader lHeader = processHandshake(mClientSocket);
                if (lHeader != null) {
                    setHeader(lHeader);
                    int lSessionTimeout = lHeader.getTimeout(getEngine().getSessionTimeout());
                    if (lSessionTimeout > 0) {
                        mClientSocket.setSoTimeout(lSessionTimeout);
                    }
                    setVersion(lHeader.getVersion());
                    setSubprot(lHeader.getSubProtocol());
                    if (mLog.isDebugEnabled()) {
                        mLog.debug(lLogInfo + " client accepted on port "
                                + mClientSocket.getPort()
                                + " with timeout "
                                + (lSessionTimeout > 0 ? lSessionTimeout + "ms" : "infinite")
                                + " (TCPNoDelay was: " + lTCPNoDelay + ")"
                                + "...");
                    }
                    lOk = true;
                } else {
                    mLog.error("Client not accepted on port "
                            + mClientSocket.getPort()
                            + " due to handshake issues");
                }
            } catch (IOException lEx) {
                mLog.error(Logging.getSimpleExceptionMessage(lEx, "executing handshake"));
            }
            try {
                if (!lOk) {
                    // if header could not be parsed properly
                    // immediately disconnect the client.
                    mClientSocket.close();
                    return;
                }
            } catch (Exception lEx) {
                mLog.error(Logging.getSimpleExceptionMessage(lEx, "closing socket"));
                return;
            }

            if (mLog.isDebugEnabled()) {
                mLog.debug("Starting " + lLogInfo + " connector...");
            }

            //Setting the session identifier in the connector's WebSocketSession instance
            mConnector.getSession().setSessionId(mConnector.getHeader().
                    getCookies().get(JWebSocketCommonConstants.SESSIONID_COOKIE_NAME).toString());

            // call connectorStarted method of engine
            lEngine.connectorStarted(mConnector);

            // readHixie and readHybi process potential exceptions already!
            try {
                if (isHixie()) {
                    processHixie(lEngine);
                } else {
                    processHybi(getVersion(), lEngine);
                }
            } finally {
                terminateConnector(mCloseReason);
            }

            // call client stopped method of engine
            // (e.g. to release client from streams)
            lEngine.connectorStopped(mConnector, mCloseReason);

            if (mLog.isInfoEnabled()) {
                mLog.info("Stopped " + mLogInfo
                        + " connector (" + mCloseReason
                        + ") on port " + lPort + ".");
            }
        }

        private void processHixie(WebSocketEngine aEngine) {
            ByteArrayOutputStream lBuff = new ByteArrayOutputStream();
            while (WebSocketConnectorStatus.UP == getStatus()) {
                try {
                    int lIn = WebSocketProtocolAbstraction.read(mIn);
                    // start of frame
                    if (lIn == 0x00) {
                        lBuff.reset();
                        // end of frame
                    } else if (lIn == 0xFF) {
                        RawPacket lPacket = new RawPacket(lBuff.toByteArray());
                        try {
                            aEngine.processPacket(mConnector, lPacket);
                            //Reading pending packets in the buffer (for high concurrency scenarios)
                            if (mIn.available() > 0) {
                                processHixie(aEngine);
                            }
                        } catch (IOException lEx) {
                            mLog.error(lEx.getClass().getSimpleName()
                                    + " in processPacket of connector "
                                    + mConnector.getClass().getSimpleName()
                                    + ": " + lEx.getMessage());
                        }
                        lBuff.reset();
                    } else if (lIn < 0) {
                        mCloseReason = CloseReason.CLIENT;
                        setStatus(WebSocketConnectorStatus.DOWN);
                        // any other byte within or outside a frame
                    } else {
                        lBuff.write(lIn);
                    }
                } catch (SocketTimeoutException lEx) {
                    mLog.error(lEx.getClass().getSimpleName()
                            + " on timeout: " + lEx.getMessage());
                    mCloseReason = CloseReason.TIMEOUT;
                    setStatus(WebSocketConnectorStatus.DOWN);
                } catch (Exception lEx) {
                    mLog.error(lEx.getClass().getSimpleName()
                            + " on other: " + lEx.getMessage());
                    mCloseReason = CloseReason.SERVER;
                    setStatus(WebSocketConnectorStatus.DOWN);
                }
            }
        }

        private void processHybi(int aVersion, WebSocketEngine aEngine) {

            String lFrom = getRemoteHost() + ":" + getRemotePort() + " (" + getId() + ")";
            while (WebSocketConnectorStatus.UP == getStatus()) {
                try {
                    WebSocketPacket lPacket = WebSocketProtocolAbstraction.protocolToRawPacket(aVersion, mIn);
                    if (lPacket == null) {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("Processing client 'disconnect' from " + lFrom + "...");
                        }
                        mCloseReason = CloseReason.CLIENT;
                        setStatus(WebSocketConnectorStatus.DOWN);
                    } else if (WebSocketFrameType.TEXT.equals(lPacket.getFrameType())) {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("Processing 'text' frame from " + lFrom + "...");
                        }
                        aEngine.processPacket(mConnector, lPacket);
                        //Reading pending packets in the buffer (for high concurrency scenarios)
                        if (mIn.available() > 0) {
                            processHybi(aVersion, aEngine);
                        }
                    } else if (WebSocketFrameType.PING.equals(lPacket.getFrameType())) {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("Processing 'ping' frame from " + lFrom + "...");
                        }
                        WebSocketPacket lPong = new RawPacket("");
                        lPong.setFrameType(WebSocketFrameType.PONG);
                        sendPacket(lPong);
                    } else if (WebSocketFrameType.CLOSE.equals(lPacket.getFrameType())) {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("Processing 'close' frame from " + lFrom + "...");
                        }
                        mCloseReason = CloseReason.CLIENT;
                        setStatus(WebSocketConnectorStatus.DOWN);

                        // As per spec, server must respond to CLOSE with acknowledgment CLOSE (maybe
                        // this should be handled higher up in the hierarchy?)
                        WebSocketPacket lClose = new RawPacket("");
                        lClose.setFrameType(WebSocketFrameType.CLOSE);
                        sendPacket(lClose);
                        // the streams are closed in the run method
                    } else {
                        mLog.error("Unknown frame type '" + lPacket.getFrameType() + "', ignoring frame.");
                    }
                } catch (SocketTimeoutException lEx) {
                    mLog.error(lEx.getClass().getSimpleName() + " reading hybi (" + getId() + "): " + lEx.getMessage());
                    mCloseReason = CloseReason.TIMEOUT;
                    setStatus(WebSocketConnectorStatus.DOWN);
                } catch (Exception lEx) {
                    if (WebSocketConnectorStatus.UP == getStatus()) {
                        mLog.error(lEx.getClass().getSimpleName() + " reading hybi (" + getId() + "): " + lEx.getMessage());
                    }
                    mCloseReason = CloseReason.SERVER;
                    setStatus(WebSocketConnectorStatus.DOWN);
                }
            } // while up, if exception occurs the status is set to DOWN
        }
    }

    @Override
    public String generateUID() {
        String lUID = mClientSocket.getInetAddress().getHostAddress()
                + "@" + mClientSocket.getPort();
        return lUID;
    }

    @Override
    public int getRemotePort() {
        return mClientSocket.getPort();
    }

    @Override
    public InetAddress getRemoteHost() {
        return mClientSocket.getInetAddress();
    }

    @Override
    public String toString() {
        // TODO: Show proper IPV6 if used
        String lRes = getId() + " (" + getRemoteHost().getHostAddress()
                + ":" + getRemotePort();
        String lUsername = getUsername();
        if (lUsername != null) {
            lRes += ", " + lUsername;
        }
        return lRes + ")";
    }
}
