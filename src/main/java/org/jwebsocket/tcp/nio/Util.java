//        ---------------------------------------------------------------------------
//        jWebSocket - Util
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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.security.KeyStore;
import javax.net.ssl.*;
import org.jwebsocket.config.JWebSocketConfig;

/**
 *
 * @author kyberneees
 */
public class Util {

        /**
         * Creates a SSLContext
         *
         * @param aKeyStore
         * @param aKeyStorePassword
         * @return
         * @throws The SSLContext instance
         */
        public static SSLContext createSSLContext(String aKeyStore, String aKeyStorePassword) throws Exception {
                SSLContext lSSLContext = SSLContext.getInstance("TLS");
                KeyManagerFactory lKMF = KeyManagerFactory.getInstance("SunX509");
                KeyStore lKeyStore = KeyStore.getInstance("JKS");

                String lKeyStorePath = JWebSocketConfig.expandEnvAndJWebSocketVars(aKeyStore);
                if (lKeyStorePath != null) {
                        char[] lPassword = aKeyStorePassword.toCharArray();
                        URL lURL = JWebSocketConfig.getURLFromPath(lKeyStorePath);
                        lKeyStore.load(new FileInputStream(lURL.getPath()), lPassword);
                        lKMF.init(lKeyStore, lPassword);

                        lSSLContext.init(lKMF.getKeyManagers(), null, new java.security.SecureRandom());
                }

                return lSSLContext;
        }

        /**
         * Creates a ServerSocketChannel
         *
         * @param aPort
         * @return The ServerSocketChannel instance
         * @throws IOException
         */
        public static ServerSocketChannel createServerSocketChannel(int aPort) throws IOException {
                ServerSocketChannel lServer = ServerSocketChannel.open();
                lServer.configureBlocking(false);
                lServer.socket().bind(new InetSocketAddress(aPort));

                return lServer;
        }

        public static ByteBuffer wrap(ByteBuffer aIn, SSLEngine aEngine, int aBufferSize) throws Exception {
                ByteBuffer lOut = ByteBuffer.allocate(aBufferSize);
                aIn.flip();
                SSLEngineResult lWrapResult = aEngine.wrap(aIn, lOut);
                aIn.compact();

                switch (lWrapResult.getStatus()) {
                        case OK:
                                if (lOut.position() > 0) {
                                        lOut.flip();
                                }
                                return lOut;
                        case BUFFER_UNDERFLOW:
                                break;
                        case BUFFER_OVERFLOW:
                                break;
                        case CLOSED:
                                throw new SSLException("SSL connection closed!");
                }

                throw new IllegalStateException("Failed to wrap!");
        }

        public static ByteBuffer unwrap(ByteBuffer aIn, SSLEngine aEngine, int aBufferSize) throws Exception {
                ByteBuffer lOut = ByteBuffer.allocate(aBufferSize);
                aIn = aIn.slice();
                SSLEngineResult lUnwrapResult = aEngine.unwrap(aIn, lOut);
                aIn.compact();
                lOut = lOut.slice();
                lOut.compact();

                switch (lUnwrapResult.getStatus()) {
                        case OK:
                                if (lOut.position() > 0) {
                                        lOut.flip();
                                }
                                return lOut;
                        case BUFFER_UNDERFLOW:
                                break;
                        case BUFFER_OVERFLOW:
                                break;
                        case CLOSED:
                                throw new SSLException("SSL connection closed!");
                }

                throw new IllegalStateException("Failed to unwrap!");
        }
}
