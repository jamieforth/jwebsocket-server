//    ---------------------------------------------------------------------------
//    jWebSocket - WebSocket Token Server (manages JSON, CSV and XML Tokens)
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import org.jwebsocket.api.*;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.filter.TokenFilterChain;
import org.jwebsocket.kit.*;
import org.jwebsocket.listener.WebSocketServerTokenEvent;
import org.jwebsocket.listener.WebSocketServerTokenListener;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.plugins.TokenPlugInChain;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;

/**
 * @author aschulze
 * @author jang
 */
public class TokenServer extends BaseServer {

    private static Logger mLog = Logging.getLogger(TokenServer.class);
    // specify name space for token server
    private static final String NS_TOKENSERVER = JWebSocketServerConstants.NS_BASE + ".tokenserver";
    // specify shared connector variables
    /**
     *
     */
    public static final String VAR_IS_TOKENSERVER = NS_TOKENSERVER + ".isTS";
    private volatile boolean mIsAlive = false;
    private static ExecutorService mCachedThreadPool;
    private final static int TIME_OUT_TERMINATION_THREAD = 10;
    private int mCorePoolSize;
    private int mMaximumPoolSize;
    private int mKeepAliveTime;
    private int mBlockingQueueSize;

    /**
     *
     * @param aServerConfig
     */
    public TokenServer(ServerConfiguration aServerConfig) {
        super(aServerConfig);
        mPlugInChain = new TokenPlugInChain(this);
        mFilterChain = new TokenFilterChain(this);
        
        mCorePoolSize = aServerConfig.getThreadPoolConfig().getCorePoolSize();
        mMaximumPoolSize = aServerConfig.getThreadPoolConfig().getMaximumPoolSize();
        mKeepAliveTime = aServerConfig.getThreadPoolConfig().getKeepAliveTime();
        mBlockingQueueSize = aServerConfig.getThreadPoolConfig().getBlockingQueueSize();
    }

    @Override
    public void startServer() throws WebSocketException {
        // Create the thread pool.
        mCachedThreadPool = new ThreadPoolExecutor(mCorePoolSize, mMaximumPoolSize, mKeepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(mBlockingQueueSize));
        mIsAlive = true;
        if (mLog.isInfoEnabled()) {
            mLog.info("Token server '" + getId() + "' started.");
        }
    }

    @Override
    public boolean isAlive() {
        // nothing special to do here.
        // Token server does not contain any thread or similar.
        return mIsAlive;
    }

    @Override
    public void stopServer() throws WebSocketException {
        mIsAlive = false;
        // Shutdown the thread pool
        if (mCachedThreadPool != null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Shutting down token server threadPool.");
            }
            mCachedThreadPool.shutdown(); // Disable new tasks from being
            // submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!mCachedThreadPool.awaitTermination(TIME_OUT_TERMINATION_THREAD, TimeUnit.SECONDS)) {
                    mCachedThreadPool.shutdownNow();
                    /*
                     * // Cancel currently // executing tasks // Wait a while
                     * for tasks to respond to being cancelled if
                     * (!mCachedThreadPool.awaitTermination(TIME_OUT_TERMINATION_THREAD,
                     * TimeUnit.SECONDS)) { mLog.error("Pool did not
                     * terminate"); mCachedThreadPool.shutdownNow(); }
                     */
                }
            } catch (InterruptedException lEx) {
                // (Re-)Cancel if current thread also interrupted
                mCachedThreadPool.shutdownNow();
            }
        }
        if (mLog.isInfoEnabled()) {
            mLog.info("Token server '" + getId() + "' stopped.");
        }
    }

    /**
     * removes a plug-in from the plug-in chain of the server.
     *
     * @param aPlugIn
     */
    public void removePlugIn(WebSocketPlugIn aPlugIn) {
        mPlugInChain.removePlugIn(aPlugIn);
    }

    @Override
    public void engineStarted(WebSocketEngine aEngine) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing engine '" + aEngine.getId() + "' started...");
        }
        mPlugInChain.engineStarted(aEngine);
    }

    @Override
    public void engineStopped(WebSocketEngine aEngine) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing engine '" + aEngine.getId() + "' stopped...");
        }
        mPlugInChain.engineStopped(aEngine);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void connectorStarted(WebSocketConnector aConnector) {
        String lFormat = aConnector.getHeader().getFormat();
        if ((lFormat != null)
                && (lFormat.equals(JWebSocketCommonConstants.WS_FORMAT_JSON)
                || lFormat.equals(JWebSocketCommonConstants.WS_FORMAT_XML)
                || lFormat.equals(JWebSocketCommonConstants.WS_FORMAT_CSV))) {

            aConnector.setBoolean(VAR_IS_TOKENSERVER, true);

            if (mLog.isDebugEnabled()) {
                mLog.debug("Processing connector '" + aConnector.getId() + "' started...");
            }
            // notify plugins that a connector has started,
            // i.e. a client was sconnected.
            mPlugInChain.connectorStarted(aConnector);
        }
        super.connectorStarted(aConnector);
    }

    @Override
    public void connectorStopped(WebSocketConnector aConnector, CloseReason aCloseReason) {
        // notify plugins that a connector has stopped,
        // i.e. a client was disconnected.
        if (aConnector.getBool(VAR_IS_TOKENSERVER)) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Processing connector '"
                        + aConnector.getId() + "' stopped...");
            }
            mPlugInChain.connectorStopped(aConnector, aCloseReason);
        }
        super.connectorStopped(aConnector, aCloseReason);
    }

    /**
     *
     * @param aConnector
     * @param aDataPacket
     * @return
     */
    public Token packetToToken(WebSocketConnector aConnector, WebSocketPacket aDataPacket) {
        String lFormat = aConnector.getHeader().getFormat();
        return TokenFactory.packetToToken(lFormat, aDataPacket);
    }

    /**
     *
     * @param aConnector
     * @param aToken
     * @return
     */
    public WebSocketPacket tokenToPacket(WebSocketConnector aConnector, Token aToken) {
        String lFormat = aConnector.getHeader().getFormat();
        return TokenFactory.tokenToPacket(lFormat, aToken);
    }

    /**
     *
     * @param aConnector
     * @param aToken
     */
    public void processFilteredToken(WebSocketConnector aConnector, Token aToken) {
        getPlugInChain().processToken(aConnector, aToken);
        // forward the token to the listener chain
        List<WebSocketServerListener> lListeners = getListeners();
        WebSocketServerTokenEvent lEvent = new WebSocketServerTokenEvent(aConnector, this);
        for (WebSocketServerListener lListener : lListeners) {
            if (lListener != null && lListener instanceof WebSocketServerTokenListener) {
                ((WebSocketServerTokenListener) lListener).processToken(lEvent, aToken);
            }
        }
    }

    private void processToken(WebSocketConnector aConnector, Token aToken) {
        // before forwarding the token to the plug-ins push it through filter
        // chain

        // TODO: Remove this temporary hack with final release 1.0
        // this was required to ensure upward compatibility from 0.10 to 0.11
        String lNS = aToken.getNS();
        if (lNS != null && lNS.startsWith("org.jWebSocket")) {
            aToken.setNS("org.jwebsocket" + lNS.substring(14));
        }

        FilterResponse lFilterResponse = getFilterChain().processTokenIn(aConnector, aToken);

        // only forward the token to the plug-in chain
        // if filter chain does not response "aborted"
        if (!lFilterResponse.isRejected()) {
            processFilteredToken(aConnector, aToken);
        }
    }

    @Override
    public void processPacket(WebSocketEngine aEngine,
            final WebSocketConnector aConnector, WebSocketPacket aDataPacket) {
        // is the data packet supposed to be interpreted as token?
        if (aConnector.getBool(VAR_IS_TOKENSERVER)) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Processing packet as token...");
            }

            final Token lToken = packetToToken(aConnector, aDataPacket);
            if (lToken != null) {
                boolean lRunReqInOwnThread = "true".equals(lToken.getString("spawnThread"));
                // TODO: create list of running threads and close all properly
                // on shutdown
                if (lRunReqInOwnThread) {
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Processing threaded token '"
                                + lToken.toString()
                                + "' from '" + aConnector + "'...");
                    }
                    mCachedThreadPool.execute(new Runnable() {

                        @Override
                        public void run() {
                            processToken(aConnector, lToken);
                        }
                    });
                } else {
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Processing token '" + lToken.toString()
                                + "' from '" + aConnector + "'...");
                    }
                    if ("org.jwebsocket.plugins.system".equals(lToken.getNS())
                            && "fragment".equals(lToken.getType())) {

                        Token llToken = FragmentedTokenBuilder.putFragment(
                                aConnector, lToken);
                        if (llToken != null) {
                            processToken(aConnector, llToken);
                        }
                    } else {
                        processToken(aConnector, lToken);
                    }
                }
            } else {
                mLog.error("Packet '" + aDataPacket.toString()
                        + "' could not be converted into token.");
            }
        } else {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Processing packet as custom packet...");
            }
        }
        super.processPacket(aEngine, aConnector, aDataPacket);
    }

    /**
     *
     * @param aSource
     * @param aTarget
     * @param aToken
     */
    public void sendToken(WebSocketConnector aSource,
            WebSocketConnector aTarget, Token aToken) {
        sendTokenData(aSource, aTarget, aToken, false);
    }

    /**
     *
     * @param aTarget
     * @param aToken
     */
    public void sendToken(WebSocketConnector aTarget, Token aToken) {
        sendToken(null, aTarget, aToken);
    }

    /**
     *
     * @param aTarget
     * @param aToken
     * @return
     */
    public IOFuture sendTokenAsync(WebSocketConnector aTarget, Token aToken) {
        return sendTokenData(null, aTarget, aToken, true);
    }

    /**
     *
     * @param aSource
     * @param aTarget
     * @param aToken
     * @return
     */
    public IOFuture sendTokenAsync(WebSocketConnector aSource,
            WebSocketConnector aTarget, Token aToken) {
        return sendTokenData(aSource, aTarget, aToken, true);
    }

    /**
     *
     * @param aEngineId
     * @param aConnectorId
     * @param aToken
     */
    public void sendToken(String aEngineId, String aConnectorId, Token aToken) {
        // TODO: return meaningful result here.
        WebSocketConnector lTargetConnector = getConnector(aEngineId, aConnectorId);
        if (lTargetConnector != null) {
            if (lTargetConnector.getBool(VAR_IS_TOKENSERVER)) {
                // before sending the token push it through filter chain
                FilterResponse lFilterResponse = getFilterChain().processTokenOut(null, lTargetConnector, aToken);
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Sending token '" + aToken + "' to '" + lTargetConnector + "'...");
                }
                sendPacketData(lTargetConnector, tokenToPacket(lTargetConnector, aToken), false);
            } else {
                mLog.warn("Connector not supposed to handle tokens.");
            }
        } else {
            mLog.warn("Target connector '" + aConnectorId + "' not found.");
        }
    }

    private IOFuture sendTokenData(WebSocketConnector aSource,
            WebSocketConnector aTarget, Token aToken, boolean aIsAsync) {
        if (null == aTarget) {
            mLog.error("Trying to send token to removed or closed connector: " + aToken.toString());
        } else if (aTarget.getBool(VAR_IS_TOKENSERVER)) {
            // before sending the token push it through filter chain
            FilterResponse lFilterResponse = getFilterChain().processTokenOut(
                    aSource, aTarget, aToken);

            // only forward the token to the plug-in chain
            // if filter chain does not response "aborted"
            if (!lFilterResponse.isRejected()) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Sending token '" + aToken
                            + "' to '" + aTarget + "'...");
                }
                WebSocketPacket lPacket = tokenToPacket(aTarget, aToken);
                return sendPacketData(aTarget, lPacket, false);
            } else {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("");
                }
            }
        } else {
            mLog.warn("Connector not supposed to handle tokens.");
        }
        return null;
    }

    private IOFuture sendPacketData(WebSocketConnector aTarget,
            WebSocketPacket aDataPacket, boolean aIsAsync) {
        if (aIsAsync) {
            return super.sendPacketAsync(aTarget, aDataPacket);
        } else {
            super.sendPacket(aTarget, aDataPacket);
            return null;
        }
    }

    /**
     * Broadcasts the passed token to all token based connectors of the
     * underlying engines that belong to the specified group.
     *
     * @param aToken - token to broadcast
     */
    public void broadcastGroup(Token aToken) {
        String lGroup = aToken.getString("group");
        // if the group is not specified in the token then noone gets the
        // message:
        if (lGroup == null || lGroup.length() <= 0) {
            mLog.debug("Token '" + aToken + "' has no group specified...");
            return;
        }
        broadcastFiltered(aToken, "group", lGroup);
    }

    /**
     * Broadcasts the passed token to all token based connectors of the
     * underlying engines that belong to the specified filter and its name.
     *
     * @param aToken
     * @param aFilterID
     * @param aFilterName
     */
    public void broadcastFiltered(Token aToken, String aFilterID, String aFilterName) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Broadcasting token '" + aToken + "' to all token based "
                    + "connectors that belong to the filter '" + aFilterID
                    + "' called '" + aFilterName + "'...");
        }
        FastMap<String, Object> lFilter = new FastMap<String, Object>();
        lFilter.put(aFilterID, aFilterName);
        broadcastFiltered(aToken, lFilter);
    }

    /**
     * Broadcasts the passed token to all token based connectors of the
     * underlying engines that belong to the specified filters.
     *
     * @param aToken
     * @param aFilter
     */
    public void broadcastFiltered(Token aToken, FastMap<String, Object> aFilter) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Broadcasting token '" + aToken + "' to all token based "
                    + "connectors that belong to the filters...");
        }

        // before sending the token push it through filter chain
        FilterResponse lFilterResponse = getFilterChain().processTokenOut(
                null, null, aToken);

        aFilter.put(VAR_IS_TOKENSERVER, true);

        // converting the token within the loop is removed in this method!
        WebSocketPacket lPacket;
        // lPackets maps protocol formats to appropriate converted packets:
        FastMap<String, WebSocketPacket> lPackets = new FastMap<String, WebSocketPacket>();
        String lFormat;
        for (WebSocketConnector lConnector : selectConnectors(aFilter).values()) {
            lFormat = lConnector.getHeader().getFormat();
            lPacket = lPackets.get(lFormat);
            // if there is no packet for this protocol format already, make one and
            // store it in the map
            if (lPacket == null) {
                lPacket = tokenToPacket(lConnector, aToken);
                lPackets.put(lFormat, lPacket);
            }
            sendPacket(lConnector, lPacket);
        }
    }

    /**
     * Broadcasts the passed token to all token based connectors of the
     * underlying engines.
     *
     * @param aToken
     */
    public void broadcastToken(Token aToken) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Broadcasting token '" + aToken + " to all token based connectors...");
        }

        // before sending the token push it through filter chain
        FilterResponse lFilterResponse = getFilterChain().processTokenOut(
                null, null, aToken);

        FastMap<String, Object> lFilter = new FastMap<String, Object>();
        lFilter.put(VAR_IS_TOKENSERVER, true);

        // converting the token within the loop is removed in this method!
        WebSocketPacket lPacket;
        // lPackets maps protocol formats to appropriate converted packets:
        FastMap<String, WebSocketPacket> lPackets = new FastMap<String, WebSocketPacket>();
        String lFormat;
        for (WebSocketConnector lConnector : selectConnectors(lFilter).values()) {
            lFormat = lConnector.getHeader().getFormat();
            lPacket = lPackets.get(lFormat);
            // if there is no packet for this protocol format already, make one and
            // store it in the map
            if (lPacket == null) {
                lPacket = tokenToPacket(lConnector, aToken);
                lPackets.put(lFormat, lPacket);
            }
            sendPacket(lConnector, lPacket);
        }
    }

    /**
     * Broadcasts to all connector, except the sender (aSource).
     *
     * @param aSource
     * @param aToken
     */
    public void broadcastToken(WebSocketConnector aSource, Token aToken) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Broadcasting token '" + aToken + " to all token based connectors...");
        }

        // before sending the token push it through filter chain
        FilterResponse lFilterResponse = getFilterChain().processTokenOut(aSource, null, aToken);

        Map<String, Object> lFilter = new FastMap<String, Object>();
        lFilter.put(VAR_IS_TOKENSERVER, true);

        // converting the token within the loop is removed in this method!
        WebSocketPacket lPacket;
        // optimization: lPackets maps protocol formats to appropriate converted packets:
        // only needs to convert packet once per protocol!
        Map<String, WebSocketPacket> lPackets = new FastMap<String, WebSocketPacket>();
        String lFormat;
        // interate through all connectors of all engines
        for (WebSocketConnector lConnector : selectConnectors(lFilter).values()) {
            if (!aSource.equals(lConnector) /*
                     * && WebSocketConnectorStatus.UP.equals(lConnector.getStatus())
                     */) {
                try {
                    RequestHeader lHeader = lConnector.getHeader();
                    if (null != lHeader) {
                        lFormat = lHeader.getFormat();
                        // try to get packet for protocol
                        lPacket = lPackets.get(lFormat);
                        // if there is no packet for this protocol format already, make one and
                        // store it in the map
                        if (null == lPacket) {
                            lPacket = tokenToPacket(lConnector, aToken);
                            lPackets.put(lFormat, lPacket);
                        }
                        lConnector.sendPacket(lPacket);
                    }
                } catch (RuntimeException ex) {
                    System.out.println(ex);
                }
            }
        }
    }

    /**
     * iterates through all connectors of all engines and sends the token to
     * each connector. The token format is considered for each connection
     * individually so that the application can broadcast a token to all kinds
     * of clients.
     *
     * @param aSource
     * @param aToken
     * @param aBroadcastOptions
     */
    public void broadcastToken(WebSocketConnector aSource, Token aToken, BroadcastOptions aBroadcastOptions) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Broadcasting token '" + aToken + " to all token based connectors...");
        }

        // before sending the token push it through filter chain
        FilterResponse lFilterResponse = getFilterChain().processTokenOut(aSource, null, aToken);

        Map<String, Object> lFilter = new FastMap<String, Object>();
        lFilter.put(VAR_IS_TOKENSERVER, true);

        // converting the token within the loop is removed in this method!
        WebSocketPacket lPacket;
        // lPackets maps protocol formats to appropriate converted packets:
        Map<String, WebSocketPacket> lPackets = new FastMap<String, WebSocketPacket>();
        String lFormat;
        for (WebSocketConnector lConnector : selectConnectors(lFilter).values()) {
            if (!aSource.equals(lConnector) || aBroadcastOptions.isSenderIncluded()) {
                lFormat = lConnector.getHeader().getFormat();
                lPacket = lPackets.get(lFormat);
                // if there is no packet for this protocol format already, make one and
                // store it in the map
                if (lPacket == null) {
                    lPacket = tokenToPacket(lConnector, aToken);
                    lPackets.put(lFormat, lPacket);
                }
                sendPacket(lConnector, lPacket);
            }
        }
    }

    /**
     * creates a standard response
     *
     * @param aInToken
     * @param aOutToken 
     */
    public void setResponseFields(Token aInToken, Token aOutToken) {
        Integer lTokenId = null;
        String lType = null;
        String lNS = null;
        if (aInToken != null) {
            lTokenId = aInToken.getInteger("utid", -1);
            lType = aInToken.getString("type");
            lNS = aInToken.getString("ns");
        }
        aOutToken.setType("response");

        // if code and msg are already part of outgoing token do not overwrite!
        aOutToken.setInteger("code", aOutToken.getInteger("code", 0));
        aOutToken.setString("msg", aOutToken.getString("msg", "ok"));

        if (lTokenId != null) {
            aOutToken.setInteger("utid", lTokenId);
        }
        if (lNS != null) {
            aOutToken.setString("ns", lNS);
        }
        if (lType != null) {
            aOutToken.setString("reqType", lType);
        }
    }

    /**
     * 
     * @param aInToken
     * @return
     */
    public Token createResponse(Token aInToken) {
        Token lResToken = TokenFactory.createToken();
        setResponseFields(aInToken, lResToken);
        return lResToken;
    }

    /**
     * creates an error token yet with a code and a message
     *
     * @param aInToken
     * @param aCode 
     * @param aMessage 
     * @return
     */
    public Token createErrorToken(Token aInToken, int aCode, String aMessage) {
        Token lResToken = createResponse(aInToken);
        lResToken.setInteger("code", aCode);
        lResToken.setString("msg", aMessage);
        return lResToken;
    }

    /**
     * creates a response token with the standard "not authenticated" message.
     *
     * @param aInToken
     * @return
     */
    public Token createNotAuthToken(Token aInToken) {

        Token lResToken = createErrorToken(aInToken, -1, "not authenticated");
        /*
         * Token lResToken = createResponse(aInToken);
         * lResToken.setInteger("code", -1); lResToken.setString("msg", "not
         * authenticated");
         */
        return lResToken;
    }

    /**
     * creates a response token with the standard ""access denied" message.
     *
     * @param aInToken
     * @return
     */
    public Token createAccessDenied(Token aInToken) {
        Token lResToken = createErrorToken(aInToken, -1, "access denied");
        /*
         * Token lResToken = createResponse(aInToken);
         * lResToken.setInteger("code", -1); lResToken.setString("msg", "access
         * denied");
         */
        return lResToken;
    }

    /**
     * creates an error response token based on
     *
     * @param aConnector
     * @param aInToken
     * @param aErrCode
     * @param aMessage
     */
    public void sendErrorToken(WebSocketConnector aConnector, Token aInToken,
            int aErrCode, String aMessage) {
        Token lToken = createResponse(aInToken);
        lToken.setInteger("code", aErrCode);
        lToken.setString("msg", aMessage);
        sendToken(aConnector, lToken);
    }

    /**
     * @return the mPlugInChain
     */
    @Override
    public TokenPlugInChain getPlugInChain() {
        return (TokenPlugInChain) mPlugInChain;
    }

    /**
     * @return the mFilterChain
     */
    @Override
    public TokenFilterChain getFilterChain() {
        return (TokenFilterChain) mFilterChain;
    }
}
