//        ---------------------------------------------------------------------------
//        jWebSocket - WebSocket NIO Engine, DataFuture
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.async.IOFutureListener;
import org.jwebsocket.logging.Logging;

public class DataFuture implements IOFuture {

        private static Logger mLog = Logging.getLogger();
        private List<IOFutureListener> mListeners;
        private boolean mDone;
        private boolean mSuccess;
        private Throwable mCause;
        private WebSocketConnector mConnector;
        private ByteBuffer mData;

        public DataFuture(WebSocketConnector aConnector, ByteBuffer aData) {
                this.mConnector = aConnector;
                this.mData = aData;
                mListeners = new ArrayList<IOFutureListener>();
        }

        @Override
        public WebSocketConnector getConnector() {
                return mConnector;
        }

        @Override
        public boolean isDone() {
                return mDone;
        }

        @Override
        public boolean isCancelled() {
                return false;  // not implemented
        }

        @Override
        public boolean isSuccess() {
                return mSuccess;
        }

        @Override
        public Throwable getCause() {
                return mCause;
        }

        @Override
        public boolean cancel() {
                return false;  // not implemented
        }

        @Override
        public boolean setSuccess() {
                mSuccess = true;
                mDone = true;
                notifyListeners();
                return mSuccess;
        }

        @Override
        public boolean setFailure(Throwable cause) {
                if (!mSuccess && !mDone) {
                        this.mCause = cause;
                        mSuccess = false;
                        mDone = true;
                        notifyListeners();
                        return true;
                } else {
                        return false;
                }
        }

        @Override
        public boolean setProgress(long amount, long current, long total) {
                return false;  // not implemented
        }

        @Override
        public void addListener(IOFutureListener listener) {
                mListeners.add(listener);
        }

        @Override
        public void removeListener(IOFutureListener listener) {
                mListeners.remove(listener);
        }

        public ByteBuffer getData() {
                return mData;
        }

        public void setData(ByteBuffer aData) {
                mData = aData;
        }

        private void notifyListeners() {
                try {
                        for (IOFutureListener listener : mListeners) {
                                listener.operationComplete(this);
                        }
                } catch (Exception e) {
                        mLog.info("Exception while notifying IOFuture listener", e);
                }
        }
}
