//    ---------------------------------------------------------------------------
//    jWebSocket - WebSocket NIO Engine
//    Copyright (c) 2011 Innotrade GmbH, jWebSocket.org, Author: Jan Gnezda
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
package org.jwebsocket.tcp.nio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import org.apache.log4j.Logger;
import org.jwebsocket.api.EngineConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.engines.BaseEngine;
import org.jwebsocket.kit.*;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.tcp.EngineUtils;
import org.jwebsocket.util.Tools;

/**
 * <p> Tcp engine that uses java non-blocking io api to bind to listening port
 * and handle incoming/outgoing packets. There's one 'selector' thread that is
 * responsible only for handling socket operations. Therefore, every packet that
 * should be sent will be firstly queued into concurrent queue, which is
 * continuously processed by selector thread. Since the queue is concurrent,
 * there's no blocking and a call to send method will return immediately. </p>
 * <p> All packets that are received from remote clients are processed in
 * separate worker threads. This way it's possible to handle many clients
 * simultaneously with just a few threads. Add more worker threads to handle
 * more clients. </p> <p> Before making any changes to this source, note this:
 * it is highly advisable to read from (or write to) a socket only in selector
 * thread. Ignoring this advice may result in strange consequences (threads
 * locking or spinning, depending on actual scenario). </p>
 *
 * @author jang
 * @author kyberneees (bug fixes, session identifier cookie support and
 * performance improvements)
 */
public class NioTcpEngine extends BaseEngine {

    private static Logger mLog = Logging.getLogger();
    private static final String NUM_WORKERS_CONFIG_KEY = "workers";
    private static final int DEFAULT_NUM_WORKERS = 100;
    private Selector mPlainSelector;
    private Selector mSSLSelector;
    private ServerSocketChannel mPlainServer;
    private boolean mIsRunning;
    private Map<String, Queue<DataFuture>> mPendingWrites; // <connector id, data queue>
    private ExecutorService mExecutorService;
    private Map<String, SocketChannel> mConnectorToChannelMap; // <connector id, socket channel>
    private Map<SocketChannel, String> mChannelToConnectorMap; // <socket channel, connector id>
    private ByteBuffer mReadBuffer;
    private Thread mPlainSelectorThread;
    private final DelayedPacketsQueue mDelayedPacketsQueue = new DelayedPacketsQueue();
    private SSLContext mSSLContext;

    public NioTcpEngine(EngineConfiguration aConfiguration) {
        super(aConfiguration);
    }

    @Override
    public void startEngine() throws WebSocketException {
        try {
            mPendingWrites = new ConcurrentHashMap<String, Queue<DataFuture>>();
            mConnectorToChannelMap = new ConcurrentHashMap<String, SocketChannel>();
            mChannelToConnectorMap = new ConcurrentHashMap<SocketChannel, String>();
            mReadBuffer = ByteBuffer.allocate(getConfiguration().getMaxFramesize());
            mPlainSelector = SelectorProvider.provider().openSelector();
//            mSSLSelector = SelectorProvider.provider().openSelector();

            mPlainServer = Util.createServerSocketChannel(getConfiguration().getPort());
            mPlainServer.register(mPlainSelector, SelectionKey.OP_ACCEPT);
            if (mLog.isDebugEnabled()) {
                mLog.debug("Non-SSL server running at port: " + getConfiguration().getPort() + "...");
            }

            // creating the SSL server only if required
//            if (getConfiguration().getSSLPort() > 0) {
//                mSSLContext = Util.createSSLContext(getConfiguration().getKeyStore(),
//                        getConfiguration().getKeyStorePassword());
//                if (mLog.isDebugEnabled()) {
//                    mLog.debug("SSLContext created with key-store: " + getConfiguration().getKeyStore() + "...");
//                }
//                mSSLServer = Util.createServerSocketChannel(getConfiguration().getSSLPort());
//                mSSLServer.register(mSSLSelector, SelectionKey.OP_ACCEPT);
//                if (mLog.isDebugEnabled()) {
//                    mLog.debug("SSL server running at port: " + getConfiguration().getSSLPort() + "...");
//                }
//            }

            mIsRunning = true;

            // start worker threads
            Integer lNumWorkers = DEFAULT_NUM_WORKERS;
            if (getConfiguration().getSettings().containsKey(NUM_WORKERS_CONFIG_KEY)) {
                lNumWorkers = Integer.parseInt(getConfiguration().
                        getSettings().
                        get(NUM_WORKERS_CONFIG_KEY).
                        toString());
            }
            mExecutorService = Executors.newFixedThreadPool(lNumWorkers);
            for (int lIdx = 0; lIdx < lNumWorkers; lIdx++) {
                // give an index to each worker thread
                mExecutorService.submit(new ReadWorker(lIdx));
            }

            // start plain selector thread
            mPlainSelectorThread = new Thread(new SelectorThread(mPlainSelector));
            mPlainSelectorThread.start();

//            if (getConfiguration().getSSLPort() > 0) {
//                // start SSL selector thread
//                mSSLSelectorThread = new Thread(new SelectorThread(mSSLSelector));
//                mSSLSelectorThread.start();
//            }

            if (mLog.isDebugEnabled()) {
                mLog.debug("NioTcpEngine started successfully with '" + lNumWorkers + "' workers!");
            }
        } catch (ClosedChannelException e) {
            throw new WebSocketException(e.getMessage(), e);
        } catch (IOException e) {
            throw new WebSocketException(e.getMessage(), e);
        }
    }

    @Override
    public void stopEngine(CloseReason aCloseReason) throws WebSocketException {
        super.stopEngine(aCloseReason);
        if (mPlainSelector != null) {
            try {
                mIsRunning = false;
                mPlainSelectorThread.join();
                mPlainSelector.wakeup();
                mPlainServer.close();
                mPlainSelector.close();
                mPendingWrites.clear();
                mExecutorService.shutdown();
                mLog.info("NIO engine stopped.");
            } catch (InterruptedException lEx) {
                throw new WebSocketException(lEx.getMessage(), lEx);
            } catch (IOException lEx) {
                throw new WebSocketException(lEx.getMessage(), lEx);
            }
        }
    }

    public void send(String aConnectorId, DataFuture aFuture) {
        try {
            if (mPendingWrites.containsKey(aConnectorId)) {
                NioTcpConnector lConnector = (NioTcpConnector) getConnectors().get(aConnectorId);
                mPendingWrites.get(aConnectorId).add(aFuture);
                if (lConnector.isSSL()) {
                    aFuture.setData(Util.wrap(
                            aFuture.getData(),
                            lConnector.getSSLEngine(),
                            getConfiguration().getMaxFramesize()));
                    mSSLSelector.wakeup();
                } else {
                    mPlainSelector.wakeup();
                }
            } else {
                aFuture.setFailure(new Exception("Discarding packet for unattached socket channel..."));
            }
        } catch (Exception lEx) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Data could not be sent!", lEx);
            }
            aFuture.setFailure(lEx);
        }
    }

    @Override
    public void connectorStopped(WebSocketConnector aConnector, CloseReason aCloseReason) {
        if (mConnectorToChannelMap.containsKey(aConnector.getId())) {
            mPendingWrites.remove(aConnector.getId());
            SocketChannel lChannel = mConnectorToChannelMap.remove(aConnector.getId());
            try {
                lChannel.close();
                lChannel.socket().close();
            } catch (IOException lEx) {
                //Ignore it. Channel has been closed previously!
            }
            mChannelToConnectorMap.remove(lChannel);
        }

        if (((NioTcpConnector) aConnector).isAfterWSHandshake()) {
            super.connectorStopped(aConnector, aCloseReason);
        }
    }

    /**
     * Socket operations are permitted only via this thread. Strange behavior
     * will occur if anything is done to the socket outside of this thread.
     */
    private class SelectorThread implements Runnable {

        Selector mSelector;

        public SelectorThread(Selector aSelector) {
            this.mSelector = aSelector;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("jWebSocket NIO-Engine SelectorThread");

            engineStarted();

            while (mIsRunning && mSelector.isOpen()) {
                boolean lWrite = false;
                for (Iterator<String> lIterator = mPendingWrites.keySet().iterator(); lIterator.hasNext();) {
                    String lConnectorId = lIterator.next();
                    try {
                        SelectionKey lKey = mConnectorToChannelMap.get(lConnectorId).keyFor(mSelector);
                        if (null != lKey && !mPendingWrites.get(lConnectorId).isEmpty()) {
                            lKey.interestOps(SelectionKey.OP_WRITE);
                            lWrite = true;
                        }
                    } catch (Exception lEx) {
                        // just ignore it. client disconnect too fast
                    }
                }
                if (lWrite) {
                    mSelector.wakeup();
                }

                try {
                    // Waits for 500ms for any data from connected clients or for new client connections.
                    // We could have indefinite wait (selector.wait()), but it is good to check for 'running' variable
                    // fairly often.
                    if (mSelector.select(500) > 0 && mIsRunning) {
                        Iterator<SelectionKey> lKeys = mSelector.selectedKeys().iterator();
                        while (lKeys.hasNext()) {
                            SelectionKey lKey = lKeys.next();
                            lKeys.remove();
                            try {
                                if (lKey.isAcceptable()) {
                                    accept(lKey, mSelector);
                                } else if (lKey.isReadable()) {
                                    read(lKey);
                                } else if (lKey.isValid() && lKey.isWritable()) {
                                    write(lKey, mSelector);
                                }
                            } catch (CancelledKeyException lCKEx) {
                                // ignore, key was cancelled an instant after isValid() returned true,
                                // most probably the client disconnected just at the wrong moment
                            }
                        }
                    }
                } catch (Exception lEx) {
                    // something happened during socket operation (select, read or write), just log it
                    mLog.error("Error during socket operation", lEx);
                }
            }

            engineStopped();
        }
    }

    private void write(SelectionKey aKey, Selector aSelector) throws IOException {
        SocketChannel lSocketChannel = (SocketChannel) aKey.channel();
        Queue<DataFuture> lQueue = mPendingWrites.get(mChannelToConnectorMap.get(lSocketChannel));
        if (!lQueue.isEmpty()) {
            DataFuture future = lQueue.element();
            try {
                ByteBuffer lData = future.getData();
                lSocketChannel.write(lData);
                if (lData.remaining() > 0) {
                    // socket's buffer is full, stop writing for now and leave the remaining
                    // data in queue for another round of writing
                    return;
                } else {
                    lQueue.remove();
                    future.setSuccess();
                }
            } catch (IOException lIOEx) {
                future.setFailure(lIOEx);
                // don't throw exception here
                // pending close packets are maybe in reading queue
                // some connectors could be not stopped yet
            }
        }
        aKey.interestOps(SelectionKey.OP_READ);
        if (!lQueue.isEmpty()) {
            aSelector.wakeup();
        }
    }

    private void accept(SelectionKey aKey, Selector aSelector) throws IOException {
        try {
            if (getConnectors().size() == getConfiguration().getMaxConnections()
                    && getConfiguration().getOnMaxConnectionStrategy().equals("close")) {
                aKey.channel().close();
                aKey.cancel();
                mLog.info("NIO client (" + ((ServerSocketChannel) aKey.channel()).socket().getInetAddress()
                        + ") not accepted due to max connections reached. Connection closed!");
            } else {
                SocketChannel lSocketChannel = ((ServerSocketChannel) aKey.channel()).accept();
                lSocketChannel.configureBlocking(false);
                lSocketChannel.register(aSelector, SelectionKey.OP_READ);
                int lSocketPort = lSocketChannel.socket().getPort();
                int lServerPort = lSocketChannel.socket().getLocalPort();
                NioTcpConnector lConnector = new NioTcpConnector(
                        this, lSocketChannel.socket().getInetAddress(),
                        lSocketPort);
                // proceed with SSL connector
                if (lServerPort == getConfiguration().getSSLPort()) {
                    lConnector.setSSL(true);
                    lConnector.setSSLEngine(mSSLContext.createSSLEngine());
                    lConnector.getSSLEngine().setUseClientMode(false);
                    lConnector.getSSLEngine().beginHandshake();
                }
                getConnectors().put(lConnector.getId(), lConnector);
                mPendingWrites.put(lConnector.getId(), new ConcurrentLinkedQueue<DataFuture>());
                mConnectorToChannelMap.put(lConnector.getId(), lSocketChannel);
                mChannelToConnectorMap.put(lSocketChannel, lConnector.getId());

                mLog.info("NIO " + ((lConnector.isSSL()) ? "(SSL)" : "(plain)")
                        + "client started. Address: " + lConnector.getRemoteHost()
                        + "@" + lConnector.getRemotePort());
            }
        } catch (IOException e) {
            mLog.warn("Could not start new client connection!");
            throw e;
        }
    }

    private void read(SelectionKey aKey) throws IOException {
        SocketChannel lSocketChannel = (SocketChannel) aKey.channel();
        mReadBuffer.clear();

        int lNumRead;
        try {
            lNumRead = lSocketChannel.read(mReadBuffer);
        } catch (IOException lIOEx) {
            // remote client probably disconnected uncleanly ?
            clientDisconnect(aKey);
            return;
        }
        if (lNumRead == -1) {
            // read channel closed, connection has ended
            clientDisconnect(aKey);
            return;
        }
        if (lNumRead > 0 && mChannelToConnectorMap.containsKey(lSocketChannel)) {
            String lConnectorId = mChannelToConnectorMap.get(lSocketChannel);
            final ReadBean lBean = new ReadBean(lConnectorId, Arrays.copyOf(mReadBuffer.array(), lNumRead));
            final NioTcpConnector lConnector = (NioTcpConnector) getConnectors().get(lBean.getConnectorId());
            mDelayedPacketsQueue.addDelayedPacket(new IDelayedPacketNotifier() {

                @Override
                public NioTcpConnector getConnector() {
                    return lConnector;
                }

                @Override
                public ReadBean getBean() {
                    return lBean;
                }
            });
        }
    }

    private void clientDisconnect(SelectionKey aKey) throws IOException {
        clientDisconnect(aKey, CloseReason.CLIENT);
    }

    private void clientDisconnect(SelectionKey aKey, CloseReason aReason) throws IOException {
        SocketChannel lChannel = (SocketChannel) aKey.channel();
        if (mChannelToConnectorMap.containsKey(lChannel)) {
            try {
                aKey.cancel();
                lChannel.socket().close();
                lChannel.close();
            } catch (IOException lEx) {
            }

            String lId = mChannelToConnectorMap.remove(lChannel);
            mPendingWrites.remove(lId);
            mConnectorToChannelMap.remove(lId);

            WebSocketConnector lConnector = getConnectors().get(lId);
            if (mDelayedPacketsQueue.getDelayedPackets().containsKey(lConnector)) {
                mDelayedPacketsQueue.getDelayedPackets().remove(lConnector);
            }

            connectorStopped(lConnector, aReason);
        }
    }

    private void clientDisconnect(WebSocketConnector aConnector) throws IOException {
        clientDisconnect(aConnector, CloseReason.CLIENT);
    }

    private void clientDisconnect(WebSocketConnector aConnector,
            CloseReason aReason) throws IOException {
        if (mConnectorToChannelMap.containsKey(aConnector.getId())) {
            clientDisconnect(mConnectorToChannelMap.get(aConnector.getId()).keyFor(mPlainSelector), aReason);
        }
    }

    private class ReadWorker implements Runnable {

        int mId = -1;

        public ReadWorker(int aId) {
            super();
            mId = aId;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("jWebSocket NIO-Engine ReadWorker " + this.mId);
            while (mIsRunning) {
                try {
                    IDelayedPacketNotifier lDelayedPacket;
                    lDelayedPacket = mDelayedPacketsQueue.take();

                    // processing SSL packets
//                    if (lDelayedPacket.getConnector().isSSL()) {
//                        boolean lContinue = false;
//                        try {
//                            lContinue = lDelayedPacket.getConnector().isAfterSSLHandshake();
//                            processSSLPacket(lDelayedPacket);
//                        } catch (Exception lEx) {
//                            if (mLog.isDebugEnabled()) {
//                                mLog.debug(Logging.getSimpleExceptionMessage(lEx, "processing SSL packet"));
//                            }
//                            clientDisconnect(lDelayedPacket.getConnector(), CloseReason.SERVER);
//                        }
//
//                        if (!lContinue) {
//                            continue; // ssl handshaking
//                        }
//                    }
                    // executing read operation
                    doRead(lDelayedPacket.getConnector(), lDelayedPacket.getBean());

                    lDelayedPacket.getConnector().releaseWorker();
                } catch (Exception e) {
                    // uncaught exception during packet processing - kill the worker (todo: think about worker restart)
                    mLog.error("Unexpected exception during incoming packet processing", e);
                    break;
                }
            }
        }

//        private void processSSLPacket(IDelayedPacketNotifier aDelayedPacket) throws Exception {
//            NioTcpConnector lConnector = aDelayedPacket.getConnector();
//            ByteBuffer lIn = ByteBuffer.wrap(aDelayedPacket.getBean().getData());
//            ByteBuffer lOut;
//
//            switch (lConnector.getSSLEngine().getHandshakeStatus()) {
//                case NOT_HANDSHAKING:
//                    lOut = Util.unwrap(
//                            lIn,
//                            lConnector.getSSLEngine(),
//                            getConfiguration().getMaxFramesize());
//                    aDelayedPacket.getBean().setData(lOut.array());
//                    break;
//                case NEED_WRAP:
//                    lOut = Util.wrap(
//                            lIn,
//                            lConnector.getSSLEngine(),
//                            getConfiguration().getMaxFramesize());
//                    aDelayedPacket.getBean().setData(lOut.array());
//
//                    mPendingWrites.get(lConnector.getId()).offer(new DataFuture(lConnector, lOut));
//                    mSSLSelector.wakeup();
//
//                    // checking if the SSL handshake is complete
//                    if (lConnector.getSSLEngine().getHandshakeStatus().equals(SSLEngineResult.HandshakeStatus.FINISHED)) {
//                        lConnector.sslHandshakeValidated();
//                    } else {
//                        processSSLPacket(aDelayedPacket);
//                    }
//
//                    break;
//                case NEED_UNWRAP:
//                    Util.unwrap(
//                            lIn,
//                            lConnector.getSSLEngine(),
//                            getConfiguration().getMaxFramesize());
//
//                    //aDelayedPacket.getBean().setData(lOut.array());
//                    processSSLPacket(aDelayedPacket);
//                    break;
//                case NEED_TASK:
//                    Runnable lTask;
//                    while ((lTask = lConnector.getSSLEngine().getDelegatedTask()) != null) {
//                        lTask.run();
//                    }
//                    processSSLPacket(aDelayedPacket);
//                    break;
//                case FINISHED:
//                    throw new IllegalStateException("SSL handshake FINISHED on connector: " + lConnector.generateUID());
//            }
//        }

        private void doRead(NioTcpConnector aConnector, ReadBean aBean) throws IOException {
            if (aConnector.isAfterWSHandshake()) {
                boolean lIsHixie = aConnector.isHixie();
                if (lIsHixie) {
                    readHixie(new ByteArrayInputStream(aBean.getData()), aConnector);
                } else {
                    readHybi(aConnector.getVersion(), new ByteArrayInputStream(aBean.getData()), aConnector);
                }
            } else {
                // checking if "max connnections" value has been reached
                if (getConnectors().size() > getConfiguration().getMaxConnections()) {
                    if (getConfiguration().getOnMaxConnectionStrategy().equals("reject")) {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("NIO client not accepted due to max connections reached."
                                    + " Connection rejected!");
                        }
                        clientDisconnect(aConnector, CloseReason.SERVER_REJECT_CONNECTION);
                    } else {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("NIO client not accepted due to max connections reached."
                                    + " Connection redirected!");
                        }
                        clientDisconnect(aConnector, CloseReason.SERVER_REDIRECT_CONNECTION);
                    }
                } else {
                    Map lReqMap = WebSocketHandshake.parseC2SRequest(aBean.getData(), false);

                    EngineUtils.parseCookies(lReqMap);
                    //Setting the session identifier cookie if not present previously
                    if (!((Map) lReqMap.get(RequestHeader.WS_COOKIES)).containsKey(JWebSocketCommonConstants.SESSIONID_COOKIE_NAME)) {
                        ((Map) lReqMap.get(RequestHeader.WS_COOKIES)).put(JWebSocketCommonConstants.SESSIONID_COOKIE_NAME, Tools.getMD5(UUID.randomUUID().toString()));
                    }

                    byte[] lResponse = WebSocketHandshake.generateS2CResponse(lReqMap);
                    RequestHeader lReqHeader = EngineUtils.validateC2SRequest(
                            getConfiguration().getDomains(), lReqMap, mLog);
                    if (lResponse == null || lReqHeader == null) {
                        if (mLog.isDebugEnabled()) {
                            mLog.warn("TCP-Engine detected illegal handshake.");
                        }
                        // disconnect the client
                        clientDisconnect(aConnector);
                    }

                    //Setting the session identifier
                    aConnector.getSession().setSessionId(lReqHeader.getCookies().get(JWebSocketCommonConstants.SESSIONID_COOKIE_NAME).toString());

                    send(aConnector.getId(), new DataFuture(aConnector, ByteBuffer.wrap(lResponse)));
                    int lTimeout = lReqHeader.getTimeout(getSessionTimeout());
                    if (lTimeout > 0) {
                        mConnectorToChannelMap.get(aBean.getConnectorId()).socket().setSoTimeout(lTimeout);
                    }
                    aConnector.wsHandshakeValidated();
                    aConnector.setHeader(lReqHeader);
                    aConnector.startConnector();
                }
            }
        }
    }

    private void readHybi(int aVersion, ByteArrayInputStream aIS, NioTcpConnector aConnector) throws IOException {
        try {
            WebSocketPacket lRawPacket;
            lRawPacket = WebSocketProtocolAbstraction.protocolToRawPacket(aVersion, aIS);

            if (lRawPacket.getFrameType() == WebSocketFrameType.PING) {
                // As per spec, server must respond to PING with PONG (maybe
                // this should be handled higher up in the hierarchy?)
                WebSocketPacket lPong = new RawPacket(lRawPacket.getByteArray());
                lPong.setFrameType(WebSocketFrameType.PONG);
                aConnector.sendPacket(lPong);
            } else if (lRawPacket.getFrameType() == WebSocketFrameType.CLOSE) {
                // As per spec, server must respond to CLOSE with acknowledgment CLOSE (maybe
                // this should be handled higher up in the hierarchy?)
                WebSocketPacket lClose = new RawPacket(lRawPacket.getByteArray());
                lClose.setFrameType(WebSocketFrameType.CLOSE);
                aConnector.sendPacket(lClose);
                clientDisconnect(aConnector, CloseReason.CLIENT);
            } else if (lRawPacket.getFrameType() == WebSocketFrameType.TEXT) {
                aConnector.flushPacket(lRawPacket);
            } else if (lRawPacket.getFrameType() == WebSocketFrameType.INVALID) {
                mLog.debug(getClass().getSimpleName() + ": Discarding invalid incoming packet... ");
            } else if (lRawPacket.getFrameType() == WebSocketFrameType.FRAGMENT
                    || lRawPacket.getFrameType() == WebSocketFrameType.BINARY) {
                mLog.debug(getClass().getSimpleName() + ": Discarding unsupported ('"
                        + lRawPacket.getFrameType().toString() + "')incoming packet... ");
            }

            //Reading pending packets in the buffer (for high concurrency scenarios)
            if (aIS.available() > 0) {
                readHybi(aVersion, aIS, aConnector);
            }
        } catch (Exception e) {
            mLog.error("(other) " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            clientDisconnect(aConnector, CloseReason.SERVER);
        }
    }

    private void readHixie(ByteArrayInputStream aIS, NioTcpConnector lConnector) throws IOException {
        ByteArrayOutputStream lBuff = new ByteArrayOutputStream();

        while (true) {
            try {
                int lByte = WebSocketProtocolAbstraction.read(aIS);
                // start of frame
                if (lByte == 0x00) {
                    lBuff.reset();
                    // end of frame
                } else if (lByte == 0xFF) {
                    RawPacket lPacket = new RawPacket(lBuff.toByteArray());
                    try {
                        lConnector.flushPacket(lPacket);
                        //Reading pending packets in the buffer (for high concurrency scenarios)
                        if (aIS.available() > 0) {
                            readHixie(aIS, lConnector);
                        }
                    } catch (Exception lEx) {
                        mLog.error(lEx.getClass().getSimpleName()
                                + " in processPacket of connector "
                                + lConnector.getClass().getSimpleName()
                                + ": " + lEx.getMessage());
                    }
                    break;
                } else {
                    lBuff.write(lByte);
                }
            } catch (Exception lEx) {
                mLog.error("Error while processing incoming packet", lEx);
                clientDisconnect(lConnector, CloseReason.SERVER);
                break;
            }
        }
    }
}
