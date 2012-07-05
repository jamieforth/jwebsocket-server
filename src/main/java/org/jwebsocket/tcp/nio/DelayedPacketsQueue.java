//        ---------------------------------------------------------------------------
//        jWebSocket - DelayedPacketsQueue
//        Copyright (c) 2012 Innotrade GmbH, jWebSocket.org
//        ---------------------------------------------------------------------------
//        This program is free software; you can redistribute it and/or modify it
//        under the terms of the GNU Lesser General Public License as published by the
//        Free Software Foundation; either version 3 of the License, or (at your
//        option) any later version.
//        This program is distributed in the hope that it will be useful, but WITHOUT
//        ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//        FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
//        more details.
//        You should have received a copy of the GNU Lesser General Public License along
//        with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//        ---------------------------------------------------------------------------
package org.jwebsocket.tcp.nio;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javolution.util.FastMap;

/**
 *
 * @author kyberneees
 */
public class DelayedPacketsQueue {

        private final Map<NioTcpConnector, Queue<IDelayedPacketNotifier>> mDelayedPackets = new FastMap<NioTcpConnector, Queue<IDelayedPacketNotifier>>().shared();

        /**
         * Enqueue a delayed packets to be processed by the workers
         *
         * @param aDelayedPacket
         */
        public void addDelayedPacket(IDelayedPacketNotifier aDelayedPacket) {
                if (!mDelayedPackets.containsKey(aDelayedPacket.getConnector())) {
                        mDelayedPackets.put(aDelayedPacket.getConnector(), new LinkedBlockingQueue<IDelayedPacketNotifier>());
                }
                mDelayedPackets.get(aDelayedPacket.getConnector()).offer(aDelayedPacket);
        }

        /**
         *
         * @return The top available delayed packet to be processed by the workers
         */
        public synchronized IDelayedPacketNotifier take() throws InterruptedException {
                while (true) {
                        Iterator<NioTcpConnector> lKeys = mDelayedPackets.keySet().iterator();
                        while (lKeys.hasNext()) {
                                NioTcpConnector lConnector = lKeys.next();
                                if (lConnector.getWorkerId() == -1 && !mDelayedPackets.get(lConnector).isEmpty()) {
                                        try {
                                                IDelayedPacketNotifier lPacket = mDelayedPackets.get(lConnector).remove();
                                                lConnector.setWorkerId(Thread.currentThread().hashCode());

                                                return lPacket;
                                        } catch (Exception lEx) {
                                                // ignore it. the connector was stopped in the middle
                                        }
                                }
                        }
                        // CPU release
                        Thread.sleep(5);
                }
        }

        public Map<NioTcpConnector, Queue<IDelayedPacketNotifier>> getDelayedPackets() {
                return mDelayedPackets;
        }
}
