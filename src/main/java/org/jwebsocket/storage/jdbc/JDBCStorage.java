//  ---------------------------------------------------------------------------
//  jWebSocket - JDBCStorage
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
package org.jwebsocket.storage.jdbc;

import java.beans.PropertyChangeSupport;
import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.Set;
import javolution.util.FastSet;
import org.jwebsocket.storage.BaseStorage;

/**
 * Implementation of the
 * <code>IBasicStorage</code> interface that stores jWebSocket component data in
 * the database.
 *
 * @author puran
 * @version $Id: JDBCStorage.java 1277 2011-01-02 15:09:35Z fivefeetfurther $
 */
public class JDBCStorage extends BaseStorage<Object, Object> {

        private String mName = null;
        /**
         * default connection url for the channels data store
         */
        private static final String CONNECTION_URL = "jdbc:mysql://127.0.0.1:3306/jwebsocketdb";
        /**
         * default connection user name for channels data store
         */
        private static final String CONNECTION_NAME = "jwebsocket";
        /**
         * default connection password for the channels data store
         */
        private static final String CONNECTION_PASSWORD = "";
        /**
         * default mysql driver name for channel store
         */
        private static final String DRIVER_NAME = "com.mysql.jdbc.Driver";
        /**
         * The descriptive information about this implementation.
         */
        protected static final String info = "JDBCStore/1.0";
        /**
         * Name to register for this Store, used for logging.
         */
        protected static String storeName = "JDBCStore";
        /**
         * Name to register for the background thread.
         */
        protected String threadName = "JDBCStore";
        /**
         * The connection username to use when trying to connect to the database.
         */
        private String connectionName = CONNECTION_NAME;
        /**
         * The connection URL to use when trying to connect to the database.
         */
        private String connectionPassword = CONNECTION_PASSWORD;
        /**
         * Connection string to use when connecting to the DB.
         */
        private String connectionURL = CONNECTION_URL;
        /**
         * The database connection.
         */
        private Connection dbConnection = null;
        /**
         * Instance of the JDBC Driver class we use as a connection factory.
         */
        protected Driver driver = null;
        /**
         * Driver to use.
         */
        protected String driverName = DRIVER_NAME;
        /**
         * Table to use
         */
        protected String tableName = null;
        /**
         * key column name
         */
        protected String keyColumnName = "store_key";
        /**
         * value column name
         */
        protected String valueColumnName = "store_value";
        /**
         * app column name
         */
        protected String appColumnName = "store_app_key";
        // ------------------------------------------------------------- SQL Variables
        /**
         * Variable to hold the
         * <code>getSize()</code> prepared statement.
         */
        protected PreparedStatement preparedSizeSql = null;
        /**
         * Variable to hold the
         * <code>keys()</code> prepared statement.
         */
        protected PreparedStatement preparedKeysSql = null;
        /**
         * Variable to hold the
         * <code>save()</code> prepared statement.
         */
        protected PreparedStatement preparedSaveSql = null;
        /**
         * Variable to hold the
         * <code>clear()</code> prepared statement.
         */
        protected PreparedStatement preparedClearSql = null;
        /**
         * Variable to hold the
         * <code>remove()</code> prepared statement.
         */
        protected PreparedStatement preparedRemoveSql = null;
        /**
         * Variable to hold the
         * <code>load()</code> prepared statement.
         */
        protected PreparedStatement preparedLoadSql = null;
        /**
         * The property change support for this component.
         */
        protected PropertyChangeSupport support = new PropertyChangeSupport(this);

        @Override
        public String getName() {
                return mName;
        }

        @Override
        public void setName(String aName) throws Exception {
                mName = aName;
        }

        /**
         * Set the driver for this Store.
         *
         * @param driverName The new driver
         */
        public void setDriverName(String driverName) {
                String oldDriverName = this.driverName;
                this.driverName = driverName;
                support.firePropertyChange("driverName", oldDriverName, this.driverName);
                this.driverName = driverName;
        }

        /**
         * Return the driver for this Store.
         *
         * @return
         */
        public String getDriverName() {
                return (this.driverName);
        }

        /**
         * Return the username to use to connect to the database.
         *
         *
         * @return
         */
        public String getConnectionName() {
                return connectionName;
        }

        /**
         * Set the username to use to connect to the database.
         *
         * @param connectionName Username
         */
        public void setConnectionName(String connectionName) {
                this.connectionName = connectionName;
        }

        /**
         * Return the password to use to connect to the database.
         *
         *
         * @return
         */
        public String getConnectionPassword() {
                return connectionPassword;
        }

        /**
         * Set the password to use to connect to the database.
         *
         * @param connectionPassword User password
         */
        public void setConnectionPassword(String connectionPassword) {
                this.connectionPassword = connectionPassword;
        }

        /**
         * Set the Connection URL for this Store.
         *
         * @param connectionURL The new Connection URL
         */
        public void setConnectionURL(String connectionURL) {
                String oldConnString = this.connectionURL;
                this.connectionURL = connectionURL;
                support.firePropertyChange("connectionURL", oldConnString, this.connectionURL);
        }

        /**
         * Return the Connection URL for this Store.
         *
         * @return
         */
        public String getConnectionURL() {
                return (this.connectionURL);
        }

        /**
         * Set the table for this Store.
         *
         * @param table The new table
         */
        public void setTableName(String table) {
                this.tableName = table;
        }

        /**
         * Return the table for this Store.
         *
         * @return
         */
        public String getTableName() {
                return this.tableName;
        }

        // --------------------------------------------------------- Public Methods
        /**
         * @return the keyColumnName
         */
        public String getKeyColumnName() {
                return keyColumnName;
        }

        /**
         * @param keyColumnName the keyColumnName to set
         */
        public void setKeyColumnName(String keyColumnName) {
                this.keyColumnName = keyColumnName;
        }

        /**
         * @return the appColumnName
         */
        public String getAppColumnName() {
                return appColumnName;
        }

        /**
         * @param appColumnName the appColumnName to set
         */
        public void setAppColumnName(String appColumnName) {
                this.appColumnName = appColumnName;
        }

        /**
         * @return the valueColumnName
         */
        public String getValueColumnName() {
                return valueColumnName;
        }

        /**
         * @param valueColumnName the valueColumnName to set
         */
        public void setValueColumnName(String valueColumnName) {
                this.valueColumnName = valueColumnName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set keySet() {
                ResultSet lRst = null;
                Set lKey = new FastSet();
                synchronized (this) {
                        int numberOfTries = 2;
                        while (numberOfTries > 0) {

                                Connection _conn = getConnection();
                                if (_conn == null) {
                                        return null;
                                }
                                try {
                                        if (preparedKeysSql == null) {
                                                String keysSql =
                                                                "SELECT " + keyColumnName
                                                                + " FROM " + tableName
                                                                + " WHERE " + appColumnName + " = ?";
                                                preparedKeysSql = _conn.prepareStatement(keysSql);
                                        }

                                        preparedKeysSql.setString(1, getAppColumnName());
                                        lRst = preparedKeysSql.executeQuery();
                                        if (lRst != null) {
                                                while (lRst.next()) {
                                                        lKey.add(lRst.getString(1));
                                                }
                                        }
                                        // Break out after the finally block
                                        numberOfTries = 0;
                                } catch (SQLException e) {
                                        // TODO: LOG ERROR
                                        // Close the connection so that it gets reopened next time
                                        if (dbConnection != null) {
                                                close(dbConnection);
                                        }
                                } finally {
                                        try {
                                                if (lRst != null) {
                                                        lRst.close();
                                                }
                                        } catch (SQLException e) {
                                                // Ignore
                                        }
                                        release(_conn);
                                }
                                numberOfTries--;
                        }
                }
                return lKey;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
                int size = 0;
                ResultSet rst = null;
                synchronized (this) {
                        int numberOfTries = 2;
                        while (numberOfTries > 0) {
                                Connection _conn = getConnection();
                                if (_conn == null) {
                                        return (size);
                                }
                                try {
                                        if (preparedSizeSql == null) {
                                                String sizeSql = "SELECT COUNT(" + keyColumnName + ") FROM " + tableName + " WHERE " + appColumnName + " = ?";
                                                preparedSizeSql = _conn.prepareStatement(sizeSql);
                                        }

                                        preparedSizeSql.setString(1, getAppColumnName());
                                        rst = preparedSizeSql.executeQuery();
                                        if (rst.next()) {
                                                size = rst.getInt(1);
                                        }
                                        // Break out after the finally block
                                        numberOfTries = 0;
                                } catch (SQLException e) {
                                        // TODO: log error
                                        if (dbConnection != null) {
                                                close(dbConnection);
                                        }
                                } finally {
                                        try {
                                                if (rst != null) {
                                                        rst.close();
                                                }
                                        } catch (SQLException e) {
                                                // Ignore
                                        }

                                        release(_conn);
                                }
                                numberOfTries--;
                        }
                }
                return (size);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get(Object key) {
                ResultSet rst = null;
                ObjectInputStream ois = null;
                // TODO: Alex: see below
                // BufferedInputStream bis = null;
                Object returnObj = null;
                synchronized (this) {
                        int numberOfTries = 2;
                        while (numberOfTries > 0) {
                                Connection _conn = getConnection();
                                if (_conn == null) {
                                        return (null);
                                }
                                try {
                                        if (preparedLoadSql == null) {
                                                String loadSql =
                                                                "SELECT " + valueColumnName
                                                                + " FROM " + tableName
                                                                + " WHERE " + keyColumnName + " = ?"
                                                                + " AND " + appColumnName + " = ?";
                                                preparedLoadSql = _conn.prepareStatement(loadSql);
                                        }
                                        preparedLoadSql.setString(1, (String) key);
                                        preparedLoadSql.setString(2, getAppColumnName());
                                        rst = preparedLoadSql.executeQuery();
                                        if (rst.next()) {
                                                // TODO: Alex: This actually never could have worked ;-)
                                                // bis = new BufferedInputStream(rst.getBinaryStream(2));
                                                ByteArrayInputStream bais = new ByteArrayInputStream(rst.getBytes(1));
                                                ois = new ObjectInputStream(bais);
                                                returnObj = ois.readObject();
                                        }
                                        // Break out after the finally block
                                        numberOfTries = 0;
                                } catch (Exception e) {
                                        // TODO: log the error
                                        if (dbConnection != null) {
                                                close(dbConnection);
                                        }
                                } finally {
                                        try {
                                                if (rst != null) {
                                                        rst.close();
                                                }
                                        } catch (SQLException e) {
                                                // Ignore
                                        }
                                        if (ois != null) {
                                                try {
                                                        ois.close();
                                                } catch (IOException e) {
                                                        // Ignore
                                                }
                                        }
                                        release(_conn);
                                }
                                numberOfTries--;
                        }
                }

                return returnObj;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object remove(Object key) {
                synchronized (this) {
                        int numberOfTries = 2;
                        while (numberOfTries > 0) {
                                Connection _conn = getConnection();

                                if (_conn == null) {
                                        return null;
                                }

                                try {
                                        if (preparedRemoveSql == null) {
                                                String removeSql =
                                                                "DELETE FROM " + tableName
                                                                + " WHERE " + keyColumnName + " = ?"
                                                                + " AND " + appColumnName + " = ?";
                                                preparedRemoveSql = _conn.prepareStatement(removeSql);
                                        }

                                        preparedRemoveSql.setString(1, (String) key);
                                        preparedRemoveSql.setString(2, getAppColumnName());
                                        preparedRemoveSql.execute();
                                        // Break out after the finally block
                                        numberOfTries = 0;
                                } catch (SQLException e) {
                                        // TODO: log error please
                                        if (dbConnection != null) {
                                                close(dbConnection);
                                        }
                                } finally {
                                        release(_conn);
                                }
                                numberOfTries--;
                        }
                }
                return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear() {
                synchronized (this) {
                        int numberOfTries = 2;
                        while (numberOfTries > 0) {
                                Connection _conn = getConnection();
                                if (_conn == null) {
                                        return;
                                }
                                try {
                                        if (preparedClearSql == null) {
                                                String clearSql = "DELETE FROM " + tableName + " WHERE " + appColumnName + " = ?";
                                                preparedClearSql = _conn.prepareStatement(clearSql);
                                        }
                                        preparedClearSql.setString(1, getAppColumnName());
                                        preparedClearSql.execute();
                                        // Break out after the finally block
                                        numberOfTries = 0;
                                } catch (SQLException e) {
                                        // TODO: log error
                                        if (dbConnection != null) {
                                                close(dbConnection);
                                        }
                                } finally {
                                        release(_conn);
                                }
                                numberOfTries--;
                        }
                }
        }

        /**
         * Save a session to the Store.
         *
         *
         * @param aKey
         * @param aData
         */
        @Override
        public Object put(Object aKey, Object aData) {
                ObjectOutputStream oos = null;
                ByteArrayOutputStream bos;
                ByteArrayInputStream bis = null;
                InputStream in = null;
                boolean result = false;
                synchronized (this) {
                        int numberOfTries = 2;
                        while (numberOfTries > 0) {
                                Connection lConn = getConnection();
                                if (lConn == null) {
                                        return false;
                                }
                                // Check if ID exists in database and if so use UPDATE.
                                remove(aKey);
                                try {
                                        bos = new ByteArrayOutputStream();
                                        oos = new ObjectOutputStream(new BufferedOutputStream(bos));
                                        oos.writeObject(aData);
                                        oos.close();
                                        oos = null;
                                        byte[] obs = bos.toByteArray();
                                        // TODO: Alex: see below:
                                        // int size = obs.length;
                                        // bis = new ByteArrayInputStream(obs, 0, size);
                                        // in = new BufferedInputStream(bis, size);

                                        if (preparedSaveSql == null) {
                                                String saveSql =
                                                                "INSERT INTO " + tableName
                                                                + " (" + keyColumnName + ", " + valueColumnName + ", " + appColumnName + ") "
                                                                + "VALUES (?, ?, ?)";
                                                preparedSaveSql = lConn.prepareStatement(saveSql);
                                        }

                                        preparedSaveSql.setString(1, (String) aKey);
                                        // TODO: Alex: Why the complex stream conversions here?
                                        preparedSaveSql.setBytes(2, obs); // setBinaryStream(2, in, size);
                                        preparedSaveSql.setString(3, getAppColumnName());
                                        preparedSaveSql.execute();
                                        // Break out after the finally block
                                        numberOfTries = 0;
                                        result = true;
                                } catch (SQLException e) {
                                        // TODO: log error
                                        if (dbConnection != null) {
                                                close(dbConnection);
                                        }
                                } catch (/*
                                                 * IO
                                                 */Exception e) {
                                        // Ignore
                                        // System.out.println(e.getMessage());
                                } finally {
                                        try {
                                                if (oos != null) {
                                                        oos.close();
                                                }
                                                if (bis != null) {
                                                        bis.close();
                                                }
                                                if (in != null) {
                                                        in.close();
                                                }
                                        } catch (IOException e) {
                                                // nothing we can do just return false
                                                result = false;
                                        }
                                        release(lConn);
                                }
                                numberOfTries--;
                        }
                        return result;
                }
        }

        // --------------------------------------------------------- Protected Methods
        /**
         * Check the connection associated with this store, if it's
         * <code>null</code> or closed try to reopen it. Returns
         * <code>null</code> if the connection could not be established.
         *
         * @return
         * <code>Connection</code> if the connection succeeded
         */
        protected Connection getConnection() {
                try {
                        if (dbConnection == null || dbConnection.isClosed()) {
                                // TODO: log info level
                                open();
                                if (dbConnection == null || dbConnection.isClosed()) {
                                        // TODO: log info level fail
                                }
                        }
                } catch (SQLException ex) {
                        // TODO: log error
                }

                return dbConnection;
        }

        /**
         * Open (if necessary) and return a database connection for use by this
         * Realm.
         *
         * @return
         * @exception SQLException if a database error occurs
         */
        protected Connection open() throws SQLException {
                // Do nothing if there is a database connection already open
                if (dbConnection != null) {
                        return (dbConnection);
                }

                // Instantiate our database driver if necessary
                if (driver == null) {
                        try {
                                Class<?> clazz = Class.forName(driverName);
                                driver = (Driver) clazz.newInstance();
                        } catch (ClassNotFoundException ex) {
                                // TODO: log error
                        } catch (InstantiationException ex) {
                                // TODO: log error
                        } catch (IllegalAccessException ex) {
                                // TODO: log error
                        }
                }
                // Open a new connection
                Properties props = new Properties();
                if (connectionName != null) {
                        props.put("user", connectionName);
                }
                if (connectionPassword != null) {
                        props.put("password", connectionPassword);
                }
                dbConnection = driver.connect(connectionURL, props);
                dbConnection.setAutoCommit(true);
                return (dbConnection);

        }

        /**
         *
         * @param t
         */
        public void handleThrowable(Throwable t) {
                if (t instanceof ThreadDeath) {
                        throw (ThreadDeath) t;
                }
                if (t instanceof VirtualMachineError) {
                        throw (VirtualMachineError) t;
                }
                // All other instances of Throwable will be silently swallowed
        }

        /**
         * Close the specified database connection.
         *
         * @param dbConnection The connection to be closed
         */
        protected void close(Connection dbConnection) {
                // Do nothing if the database connection is already closed
                if (dbConnection == null) {
                        return;
                }

                // Close our prepared statements (if any)
                try {
                        preparedSizeSql.close();
                } catch (Throwable f) {
                        handleThrowable(f);
                }
                this.preparedSizeSql = null;

                try {
                        preparedKeysSql.close();
                } catch (Throwable f) {
                        handleThrowable(f);
                }
                this.preparedKeysSql = null;

                try {
                        preparedSaveSql.close();
                } catch (Throwable f) {
                        handleThrowable(f);
                }
                this.preparedSaveSql = null;

                try {
                        preparedClearSql.close();
                } catch (Throwable f) {
                        handleThrowable(f);
                }

                try {
                        preparedRemoveSql.close();
                } catch (Throwable f) {
                        handleThrowable(f);
                }
                this.preparedRemoveSql = null;

                try {
                        preparedLoadSql.close();
                } catch (Throwable f) {
                        handleThrowable(f);
                }
                this.preparedLoadSql = null;

                // Close this database connection, and log any errors
                try {
                        dbConnection.close();
                } catch (SQLException e) {
                        // TODO:log error here
                } finally {
                        this.dbConnection = null;
                }

        }

        /**
         * Release the connection, not needed here since the connection is not
         * associated with a connection pool.
         *
         * @param conn The connection to be released
         */
        protected void release(Connection conn) {
                // NOOP
        }

        @Override
        public boolean isEmpty() {
                return size() <= 0;
        }

        @Override
        public void initialize() {
        }
}
