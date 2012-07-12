//    ---------------------------------------------------------------------------
//    jWebSocket - PurgeCancelledWriterTimeouts
//    Copyright (c) 2011 Alexander Schulze, Innotrade GmbH
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

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author kyberneees, aschulze
 */
public class PurgeCancelledWriterTasks extends TimerTask {

    private Timer mTimer;
    
    public PurgeCancelledWriterTasks(Timer aTimer) {
        mTimer = aTimer;
    }

    @Override
    public void run() {
        mTimer.purge(); // Keep the timer cleaned up
        /*
        int lCount = mTimer.purge(); // Keep the timer cleaned up
        if (lCount > 0 && mLog.isDebugEnabled()) {
            mLog.debug("Purged " + lCount + " cancelled TCP writer tasks.");
        }
         */
    }
}
