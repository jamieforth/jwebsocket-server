//  ---------------------------------------------------------------------------
//  jWebSocket - RRPCManager
//  Copyright (c) 2011 Innotrade GmbH, jWebSocket.org
//  ---------------------------------------------------------------------------
//  This program is free software; you can redistribute it and/or modify it
//  under the terms of the GNU Lesser General Public License as published by the
//  Free Software Foundation; either version 3 of the License, or (at your
//  option) any later version.
//  This program is distributed in the hope that it will be useful, but WITHOUT
//  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
//  more details.
//  You should have received a copy of the GNU Lesser General Public License along
//  with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//  ---------------------------------------------------------------------------
package org.jwebsocket.rrpc;

import java.util.Map;
import java.util.Set;
import javolution.util.FastMap;
import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.kit.CloseReason;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jwebsocket.api.IInitializable;
import org.jwebsocket.api.IRRPCManager;
import org.jwebsocket.api.IRRPCOnResponseCallback;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.exception.MissingTokenSender;
import org.jwebsocket.kit.PlugInResponse;
import org.jwebsocket.plugins.TokenPlugIn;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;
import org.jwebsocket.util.Tools;

/**
 *
 * @author kyberneees
 */
public class RRPCManager extends TokenPlugIn implements IInitializable, IRRPCManager {

        private static Log mLogger = LogFactory.getLog(RRPCManager.class);
        //Unique procedure call identifier (UPCID)
        private Integer mUPCID = 0;
        private FastMap<String, FastMap<String, IRRPCOnResponseCallback>> callsMap;
        public static final String UPCID = "$pid";
        public static final String HAS_CALLBACK = "$hcb";
        public static final String PROCESSING_TIME = "$pt";
        public static final String RESPONSE = "$r";

        public RRPCManager(PluginConfiguration aConfiguration) {
                super(aConfiguration);

                setNamespace(JWebSocketServerConstants.NS_BASE + ".plugin.rrpc");
        }

        /**
         * Send a RRPC to a custom connector
         * 
         * @param aConnectorId
         * @param aRRPC
         * @throws MissingTokenSender 
         */
        @Override
        public void send(String aConnectorId, RRPC aRRPC) throws MissingTokenSender {
                send(aConnectorId, aRRPC, null);
        }

        /**
         * Send a RRPC to a custom connector 
         * 
         * @param aConnectorId
         * @param aRRPC
         * @param aOnResponse
         * @throws MissingTokenSender 
         */
        @Override
        public void send(String aConnectorId, RRPC aRRPC, IRRPCOnResponseCallback aOnResponse) throws MissingTokenSender {
                if (mLogger.isDebugEnabled()) {
                        mLogger.debug("Preparing RRPC ...");
                }

                if (!callsMap.containsKey(aConnectorId)) {
                        callsMap.put(aConnectorId, new FastMap<String, IRRPCOnResponseCallback>());
                }

                //Creating the token to send
                Token lToken = TokenFactory.createToken(getNamespace(), "rrpc");
                aRRPC.writeToToken(lToken);
                lToken.setString(UPCID, getNextUPCID());

                //Saving the callback
                if (null != aOnResponse) {
                        if (mLogger.isDebugEnabled()) {
                                mLogger.debug("Saving the OnResponseCallback for the RRPC '" + aRRPC.getProcedureName() + "'...");
                        }
                        //Saving the callback
                        aOnResponse.setRequiredType(aRRPC.getResponseType());
                        callsMap.get(aConnectorId).put(lToken.getString(UPCID), aOnResponse);
                        //Setting the send time
                        aOnResponse.setSentTime(System.nanoTime());

                        //2CEvent have a callback
                        lToken.setBoolean(HAS_CALLBACK, true);

                        //Registering timeout callbacks
                        if (aRRPC.getTimeout() > 0) {
                                Tools.getTimer().schedule(new TimeoutCallbackTask(aConnectorId, lToken.getString(UPCID), callsMap), aRRPC.getTimeout());
                        }
                } else {
                        //RRPC don't have a callback
                        lToken.setBoolean(HAS_CALLBACK, false);
                }

                //Sending the token
                if (mLogger.isDebugEnabled()) {
                        mLogger.debug("Sending RRPC notification to '" + aConnectorId + "' connector...");
                }

                //Getting the local WebSocketConnector instance if exists
                WebSocketConnector lConnector = getServer().getConnector(aConnectorId);

                if (null != lConnector) {
                        //Sending locally on the server
                        getServer().sendToken(lConnector, lToken);
                }
//                else if (getServer().isClusterNode() && getServer().
//                                getClusterNode().getAllConnectors().contains(aConnectorId)) {
//                        //Sending the token to the cluster network
//                        getServer().getClusterNode().sendToken(aConnectorId, token);
//                } else {
//                        throw new MissingTokenSender("Not engine or cluster detected to send "
//                                        + "the token to the giving connector: '" + aConnectorId + "'!");
//                }
        }

        /**
         * Send a RRPC to a custom connector 
         * 
         * @param aConnector
         * @param aRRPC
         * @param aOnResponse
         * @throws MissingTokenSender 
         */
        @Override
        public void send(WebSocketConnector aConnector, RRPC aRRPC, IRRPCOnResponseCallback aOnResponse) throws MissingTokenSender {
                send(aConnector.getId(), aRRPC, aOnResponse);
        }

        /**
         * Send a RRPC to a custom connector 
         * 
         * @param aConnector
         * @param aRRPC
         * @param aOnResponse
         * @throws MissingTokenSender 
         */
        @Override
        public void send(WebSocketConnector aConnector, RRPC aRRPC) throws MissingTokenSender {
                send(aConnector.getId(), aRRPC);
        }

        @Override
        public void processToken(PlugInResponse aResponse, WebSocketConnector aConnector, Token aToken) {
                if (aToken.getType().equals("rpne")) {
                        remoteProcedureNotExists(aConnector.getId(), aToken.getString(UPCID));
                } else if (aToken.getType().equals("resp")) {
                        processResponse(aConnector.getId(), aToken.getString(UPCID),
                                        aToken.getInteger(PROCESSING_TIME), aToken.getObject(RESPONSE));
                }
        }

        @Override
        public void connectorStopped(WebSocketConnector aConnector, CloseReason aCloseReason) {
                String lConnectorId = aConnector.getId();
                if (callsMap.containsKey(lConnectorId)) {
                        if (mLogger.isDebugEnabled()) {
                                mLogger.debug("Removing pending RRPC callbacks for '" + lConnectorId + "' connector...");
                        }

                        //Getting pending callbacks and removing
                        FastMap<String, IRRPCOnResponseCallback> lPendingCallbacks = callsMap.remove(lConnectorId);

                        double lCurrentTime = System.nanoTime();
                        for (Map.Entry<String, IRRPCOnResponseCallback> cb : lPendingCallbacks.entrySet()) {
                                //Updating the  elapsed time
                                cb.getValue().setElapsedTime(lCurrentTime - cb.getValue().getSentTime());
                                //Calling the failure method
                                cb.getValue().failure(FailureReason.CONNECTOR_STOPPED, lConnectorId);
                        }
                }
        }

        /**
         * Process RRPC response from the client
         * 
         * @param aConnectorId
         * @param aUPCID
         * @param aProcessingTime
         * @param aResponse 
         */
        private void processResponse(String aConnectorId, String aUPCID, Integer aProcessingTime, Object aResponse) {
                if (mLogger.isDebugEnabled()) {
                        mLogger.debug("Processing response (" + aUPCID
                                        + ") from '" + aConnectorId + "' connector...");
                }

                //If a callback is pending for this response
                if (callsMap.containsKey(aConnectorId) && callsMap.get(aConnectorId).containsKey(aUPCID)) {
                        //Getting the OnResponse callback
                        IRRPCOnResponseCallback lCallback = callsMap.get(aConnectorId).remove(aUPCID);

                        //Setting the processing time
                        lCallback.setProcessingTime(aProcessingTime);

                        //Cleaning if empty
                        if (callsMap.get(aConnectorId).isEmpty()) {
                                callsMap.remove(aConnectorId);
                        }

                        //Executing the validation process...
                        if (!lCallback.getRequiredType().equals("void")) {
                                try {
                                        //Validating the response
                                        Class lClass = Class.forName(Tools.getJavaClassnameFromGenericType(lCallback.getRequiredType()));
                                        if (lClass.isInstance(aResponse)
                                                        && lCallback.isValid(aResponse, aConnectorId)) {
                                                lCallback.setElapsedTime(System.nanoTime() - lCallback.getSentTime());
                                                lCallback.success(aResponse, aConnectorId);
                                        } else {
                                                lCallback.setElapsedTime(System.nanoTime() - lCallback.getSentTime());
                                                lCallback.failure(FailureReason.INVALID_RESPONSE, aConnectorId);
                                        }
                                } catch (ClassNotFoundException ex) {
                                        if (mLogger.isDebugEnabled()) {
                                                mLogger.debug("The generic type '" + lCallback.getRequiredType()
                                                                + "' does not exits or has not a java class associated!");
                                        }
                                }
                        } else {
                                lCallback.setElapsedTime(System.nanoTime() - lCallback.getSentTime());
                                lCallback.success(null, aConnectorId);
                        }
                } else {
                        if (mLogger.isDebugEnabled()) {
                                mLogger.debug("The RRPC(" + aUPCID
                                                + ") from '" + aConnectorId + "' has not pending callbacks!");
                        }
                }
        }

        /**
         * Process the client REMOTE_PROCEDURE_NOT_EXISTS notification
         * 
         * @param aConnectorId
         * @param aUPCID 
         */
        private void remoteProcedureNotExists(String aConnectorId, String aUPCID) {
                if (mLogger.isDebugEnabled()) {
                        mLogger.debug("Processing a REMOTE_PROCEDURE_NOT_EXISTS failure...");
                }

                //Removing only if a callback is pending
                if (callsMap.containsKey(aConnectorId)
                                && callsMap.get(aConnectorId).containsKey(aUPCID)) {
                        if (mLogger.isDebugEnabled()) {
                                mLogger.debug("Removing pending callback for RRPC: " + aUPCID + " ...");
                        }

                        //Getting the callback and removing
                        IRRPCOnResponseCallback lCallback = callsMap.get(aConnectorId).remove(aUPCID);

                        //Updating the elapsed time
                        lCallback.setElapsedTime(System.nanoTime() - lCallback.getSentTime());

                        //Calling the failure method
                        lCallback.failure(FailureReason.REMOTE_PROCEDURE_NOT_EXISTS, aConnectorId);
                }
        }

        /**
         *
         * @return The unique identifier to identify the token
         */
        synchronized private String getNextUPCID() {
                if (mUPCID.equals(Integer.MAX_VALUE)) {
                        mUPCID = 0;
                        return "0";
                }
                mUPCID += 1;

                //Adding the node identifier if in a cluster
//                String clusterNodeId = "";
//                if (getServer().isClusterNode()) {
//                        clusterNodeId = getServer().getClusterNode().getId();
//                }

//                return clusterNodeId + Integer.toString(upcid);
                return Integer.toString(mUPCID);
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public void initialize() throws Exception {
                //Purge cancelled on timeout callbacks every 5 minutes
                Tools.getTimer().scheduleAtFixedRate(new PurgeCancelledTimeoutsTask(Tools.getTimer()), 0, 300000);

                callsMap = new FastMap<String, FastMap<String, IRRPCOnResponseCallback>>().shared();
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public void shutdown() throws Exception {
                Set<String> lConnectorIds = callsMap.keySet();
                Set<String> lUPCIDs;
                FastMap<String, IRRPCOnResponseCallback> lCalls;
                for (String lConnectorId : lConnectorIds) {
                        lCalls = callsMap.get(lConnectorId);
                        lUPCIDs = lCalls.keySet();

                        for (String lKey : lUPCIDs) {
                                //Calling the failure method in every pending callback
                                lCalls.get(lKey).failure(FailureReason.SERVER_SHUTDOWN, null);
                        }
                }
        }
}
