//        ---------------------------------------------------------------------------
//        jWebSocket - ReadBean
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

/**
 *
 * @author kyberneees
 */
public class ReadBean {

        private String mConnectorId;
        private byte[] mData;

        public ReadBean(String aConnectorId, byte[] aData) {
                this.mConnectorId = aConnectorId;
                this.mData = aData;
        }

        public String getConnectorId() {
                return mConnectorId;
        }

        public byte[] getData() {
                return mData;
        }

        /**
         * Utility setter for SSL transformation
         *
         * @param aData
         */
        public void setData(byte[] aData) {
                mData = aData;
        }
}
