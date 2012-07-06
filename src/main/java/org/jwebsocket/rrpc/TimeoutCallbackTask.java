//  ---------------------------------------------------------------------------
//  jWebSocket - EventsPlugIn
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

import java.util.TimerTask;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jwebsocket.api.IRRPCOnResponseCallback;

/**
 *
 * @author kyberneees
 */
public class TimeoutCallbackTask extends TimerTask {

    private String connectorId;
    private String upcid;
    private FastMap<String, FastMap<String, IRRPCOnResponseCallback>> callsMap;
    private static Log logger = LogFactory.getLog(TimeoutCallbackTask.class);

    public TimeoutCallbackTask(String connectorId, String upcid, FastMap<String, FastMap<String, IRRPCOnResponseCallback>> callsMap) {
        this.connectorId = connectorId;
        this.upcid = upcid;
        this.callsMap = callsMap;
    }

    @Override
    public void run() {
        //Execute only if the OnResponse callback was not called before
        if (callsMap.containsKey(connectorId)
                && callsMap.get(connectorId).containsKey(upcid)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Calling the failure method because of a timeout reason."
                        + " Notification: " + connectorId + ":" + upcid);
            }

            //Getting the callback
            IRRPCOnResponseCallback aOnResponse = callsMap.get(connectorId).remove(upcid);

            //Cleaning if empty
            if (callsMap.get(connectorId).isEmpty()) {
                callsMap.remove(connectorId);
            }

            aOnResponse.failure(FailureReason.TIMEOUT, connectorId);
        }
    }
}
