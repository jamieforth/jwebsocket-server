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

import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author kyberneees
 */
public class PurgeCancelledTimeoutsTask extends TimerTask {

    private Timer t;
    private static Log logger = LogFactory.getLog(PurgeCancelledTimeoutsTask.class);

    public PurgeCancelledTimeoutsTask(Timer t) {
        this.t = t;
    }

    @Override
    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Purging the timeout callbacks queue...");
        }
        t.purge();
    }
}
