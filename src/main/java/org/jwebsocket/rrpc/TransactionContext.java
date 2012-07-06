//  ---------------------------------------------------------------------------
//  jWebSocket - EventsPlugIn
//  Copyright (c) 2010 Innotrade GmbH, jWebSocket.org
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
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.api.WebSocketServer;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.token.Token;

/**
 * The Transaction context is a collection of resources used to success
 * back to the target client the response on a S2C call
 *
 * @author kyberneees
 */
public class TransactionContext {

    private TokenServer server;
    private Token request;
    private Map<String, Object> resources;
    private double processingTime;
    private double elapsedTime;
    private WebSocketConnector senderConnector;

    public TransactionContext(TokenServer aServer, WebSocketConnector aSenderConnector,
            Token aRequest, Map<String, Object> aResources) {
        server = aServer;
        request = aRequest;
        resources = aResources;
        senderConnector = aSenderConnector;
    }

    public Token getRequest() {
        return request;
    }

    public void setRequest(Token request) {
        this.request = request;
    }

    public Map<String, Object> getResources() {
        return resources;
    }

    public void setResources(Map<String, Object> resources) {
        this.resources = resources;
    }

    public WebSocketServer getServer() {
        return server;
    }

    public void setServer(TokenServer server) {
        this.server = server;
    }

    public WebSocketConnector getSenderConnector() {
        return senderConnector;
    }

    public void setSenderConnector(WebSocketConnector senderConnector) {
        this.senderConnector = senderConnector;
    }

    /**
     * Notify the sender connector about the success transaction
     * 
     * @param response The response from the target client
     */
    public void success(Object response) {
        Token r = server.createResponse(request);
        if (null != response) {
            r.getMap().put("response", response);
        }
        r.setDouble("processingTime", getProcessingTime());
        r.setDouble("elapsedTime", getElapsedTime());

        server.sendToken(senderConnector, r);
    }

    /**
     * Notify the sender client about the success transaction
     */
    public void success() {
        success(null);
    }

    /**
     * Notify the sender client about the failure transaction
     * 
     * @param reason Failure reason
     * @param message Custom failure message
     */
    public void failure(FailureReason reason, String message) {
        Token r = server.createErrorToken(request, -1, message);
        r.setString("reason", reason.name());
        r.setDouble("elapsedTime", getElapsedTime());

        server.sendToken(senderConnector, r);
    }

    /**
     * @return Time required by the client to process the event
     * <p>
     * Time unit in nanoseconds or milliseconds depending of the client
     */
    public double getProcessingTime() {
        return processingTime;
    }

    /**
     * @param processingTime Time required by the client to process the event
     */
    public void setProcessingTime(double processingTime) {
        this.processingTime = processingTime;
    }

    /**
     * @return The complete time in nanoseconds passed from the "sent" time mark to 
     * the "response received" time mark
     */
    public double getElapsedTime() {
        return elapsedTime;
    }

    /**
     * @param elapsedTime The complete time in nanoseconds passed from the "sent" 
     * time mark to the "response received" time mark
     */
    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }
}
