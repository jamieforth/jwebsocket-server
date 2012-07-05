//        ---------------------------------------------------------------------------
//        jWebSocket - WebSocket NIO Engine
//        Copyright (c) 2011 Innotrade GmbH, jWebSocket.org, Author: Jan Gnezda
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

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.connectors.BaseConnector;
import org.jwebsocket.kit.WebSocketProtocolAbstraction;
import org.jwebsocket.logging.Logging;

/**
 *
 * @author jang
 * @author kyberneees
 */
public class NioTcpConnector extends BaseConnector {

        private static Logger mLog = Logging.getLogger();
        private InetAddress mRemoteAddress;
        private int mRemotePort;
        private boolean mIsAfterWSHandshake;
        private int mWorkerId = -1;
        private boolean mIsAfterSSLHandshake;
        private SSLEngine mSSLEngine;

        public NioTcpConnector(NioTcpEngine aEngine, InetAddress aRemoteAddress,
                        int aRemotePort) {
                super(aEngine);

                this.mRemoteAddress = aRemoteAddress;
                this.mRemotePort = aRemotePort;
                mIsAfterWSHandshake = false;
                mWorkerId = -1;
        }

        public SSLEngine getSSLEngine() {
                return mSSLEngine;
        }

        public void setSSLEngine(SSLEngine aSSLEngine) {
                this.mSSLEngine = aSSLEngine;
        }

        @Override
        public void sendPacket(WebSocketPacket aPacket) {
                sendPacketAsync(aPacket); // nio engine works asynchronously by default
        }

        @Override
        public IOFuture sendPacketAsync(WebSocketPacket aPacket) {
                byte[] lProtocolPacket;
                if (isHixie()) {
                        lProtocolPacket = new byte[aPacket.getByteArray().length + 2];
                        lProtocolPacket[0] = 0x00;
                        System.arraycopy(aPacket.getByteArray(), 0, lProtocolPacket, 1, aPacket.getByteArray().length);
                        lProtocolPacket[lProtocolPacket.length - 1] = (byte) 0xFF;
                } else {
                        lProtocolPacket = WebSocketProtocolAbstraction.rawToProtocolPacket(getVersion(), aPacket);
                }

                DataFuture lFuture = new DataFuture(this, ByteBuffer.wrap(lProtocolPacket));
                ((NioTcpEngine) getEngine()).send(getId(), lFuture);
                return lFuture;
        }

        @Override
        public String getId() {
                return String.valueOf(hashCode());
        }

        @Override
        public String generateUID() {
                return mRemoteAddress.getHostAddress() + '@' + mRemotePort;
        }

        @Override
        public InetAddress getRemoteHost() {
                return mRemoteAddress;
        }

        @Override
        public int getRemotePort() {
                return mRemotePort;
        }

        public void wsHandshakeValidated() {
                mIsAfterWSHandshake = true;
        }

        public boolean isAfterWSHandshake() {
                return mIsAfterWSHandshake;
        }

        public boolean isAfterSSLHandshake() {
                return mIsAfterSSLHandshake;
        }

        public void sslHandshakeValidated() {
                mIsAfterSSLHandshake = true;
        }

        public void flushPacket(WebSocketPacket aPacket) {
                try {
                        getEngine().processPacket(this, aPacket);
                } catch (Exception e) {
                        mLog.error(e.getClass().getSimpleName()
                                        + " in processPacket of connector "
                                        + getClass().getSimpleName(), e);
                }
        }

        public int getWorkerId() {
                return mWorkerId;
        }

        public void setWorkerId(int aWorkerId) {
                this.mWorkerId = aWorkerId;
        }

        public void releaseWorker() throws IOException {
                mWorkerId = -1;
        }
}
