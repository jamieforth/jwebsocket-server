//  ---------------------------------------------------------------------------
//  jWebSocket - OnResponseCallback
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

import org.jwebsocket.api.IRRPCOnResponseCallback;

/**
 *
 * @author kyberneees
 */
public class OnResponseCallback implements IRRPCOnResponseCallback {

    private Object context;
    private String requiredType;
    private double sentTime;
    private double processingTime;
    private double elapsedTime;

    /**
     *
     * @param aContext The context to use by the callbacks
     */
    public OnResponseCallback(Object aContext) {
        context = aContext;
    }

    /**
     * Callback used to handle the success response from the client
     * 
     * @param response The response returned by the client-side 
     * @param from The target client connector
     */
    @Override
    public void success(Object response, String from) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Callback used to handle the failure response from the client
     *
     * @param reason The reason of why the s2c call has failed 
     * @param from The target client connector
     */
    @Override
    public void failure(FailureReason reason, String from) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Execute custom validations in client responses 
     * 
     * @param response The response to validate
     * @param from The target client connector
     * @return
     */
    @Override
    public boolean isValid(Object response, String from) {
        return true;
    }

    /**
     * @return The context to use by the callbacks 
     */
    public Object getContext() {
        return context;
    }

    /**
     * @param context The context to use by the callbacks 
     */
    public void setContext(Object context) {
        this.context = context;
    }

    /**
     * @return The required response type
     */
    @Override
    public String getRequiredType() {
        return requiredType;
    }

    /**
     * @param requiredType The required response type to set
     */
    @Override
    public void setRequiredType(String requiredType) {
        this.requiredType = requiredType;
    }

    /**
     * @return The time in nanoseconds from the sent point
     */
    @Override
    public double getSentTime() {
        return sentTime;
    }

    /**
     * @param sentTime The time in nanoseconds from the sent point
     */
    @Override
    public void setSentTime(double sentTime) {
        this.sentTime = sentTime;
    }

    /**
     * @return Time required by the client to process the event
     * <p>
     * Time unit in nanoseconds or milliseconds depending of the client
     */
    @Override
    public double getProcessingTime() {
        return processingTime;
    }

    /**
     * @param processingTime Time required by the client to process the event
     */
    @Override
    public void setProcessingTime(double processingTime) {
        this.processingTime = processingTime;

        //TransactionContext support
        if (null != context && context instanceof TransactionContext) {
            ((TransactionContext) context).setProcessingTime(processingTime);
        }
    }

    /**
     * @return The complete time in nanoseconds passed from the "sent" time mark to 
     * the "response received" time mark
     */
    @Override
    public double getElapsedTime() {
        return elapsedTime;
    }

    /**
     * @param elapsedTime The complete time in nanoseconds passed from the "sent" 
     * time mark to the "response received" time mark
     */
    @Override
    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;

        //TransactionContext support
        if (null != context && context instanceof TransactionContext) {
            ((TransactionContext) context).setElapsedTime(elapsedTime);
        }
    }
}
