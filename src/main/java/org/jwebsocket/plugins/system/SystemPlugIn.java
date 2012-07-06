//    ---------------------------------------------------------------------------
//    jWebSocket - The jWebSocket System Plug-In
//    Copyright (c) 2010, 2011 Alexander Schulze, Innotrade GmbH
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
package org.jwebsocket.plugins.system;

import java.util.List;
import java.util.Map;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import org.jwebsocket.api.ISessionManager;
import org.jwebsocket.api.IUserUniqueIdentifierContainer;
import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.connectors.BaseConnector;
import org.jwebsocket.kit.BroadcastOptions;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.kit.PlugInResponse;
import org.jwebsocket.kit.WebSocketSession;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.plugins.TokenPlugIn;
import org.jwebsocket.security.SecurityFactory;
import org.jwebsocket.security.User;
import org.jwebsocket.server.TokenServer;
import org.jwebsocket.session.SessionManager;
import org.jwebsocket.storage.httpsession.HttpSessionStorage;
import org.jwebsocket.token.BaseToken;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;
import org.jwebsocket.util.Tools;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * implements the jWebSocket system tokens like login, logout, send, broadcast
 * etc...
 *
 * @author aschulze
 */
public class SystemPlugIn extends TokenPlugIn {

    private static Logger mLog = Logging.getLogger();
    // specify name space for system plug-in
    private static final String NS_SYSTEM = JWebSocketServerConstants.NS_BASE + ".plugins.system";
    // specify token types processed by system plug-in
    private static final String TT_SEND = "send";
    private static final String TT_RESPOND = "respond";
    private static final String TT_BROADCAST = "broadcast";
    private static final String TT_WELCOME = "welcome";
    private static final String TT_GOODBYE = "goodBye";
    private static final String TT_HEADER = "header";
    // old future deprecated
    private static final String TT_LOGIN = "login";
    private static final String TT_LOGOUT = "logout";
    // new spring based auth
    private static final String TT_LOGON = "logon";
    private static final String TT_LOGOFF = "logoff";
    private static final String TT_GET_AUTHORITIES = "getAuthorities";
    private static final String TT_CLOSE = "close";
    private static final String TT_GETCLIENTS = "getClients";
    private static final String TT_PING = "ping";
    private static final String TT_ECHO = "echo";
    private static final String TT_WAIT = "wait";
    private static final String TT_ALLOC_CHANNEL = "alloc";
    private static final String TT_DEALLOC_CHANNEL = "dealloc";
    // specify shared connector variables
    private static final String VAR_GROUP = NS_SYSTEM + ".group";
    private static boolean BROADCAST_OPEN = true;
    private static final String BROADCAST_OPEN_KEY = "broadcastOpenEvent";
    private static boolean BROADCAST_CLOSE = true;
    private static final String BROADCAST_CLOSE_KEY = "broadcastCloseEvent";
    private static boolean BROADCAST_LOGIN = true;
    private static final String BROADCAST_LOGIN_KEY = "broadcastLoginEvent";
    private static boolean BROADCAST_LOGOUT = true;
    private static final String BROADCAST_LOGOUT_KEY = "broadcastLogoutEvent";
    private static String ALLOW_ANONYMOUS_KEY = "allowAnonymousLogin";
    private static String ANONYMOUS_USER = "anonymous";
    private static boolean ALLOW_ANONYMOUS_LOGIN = false;
    private static String ALLOW_AUTO_ANONYMOUS_KEY = "allowAutoAnonymous";
    private static boolean ALLOW_AUTO_ANONYMOUS = false;
    private AuthenticationProvider mAuthProv;
    private ProviderManager mAuthProvMgr;
    private ISessionManager mSessionManager;
    /**
     *
     */
    public static final String USERNAME = "$username";
    /**
     *
     */
    public static final String AUTHORITIES = "$authorities";
    /**
     *
     */
    public static final String UUID = "$uuid";
    /**
     *
     */
    public static final String IS_AUTHENTICATED = "$is_authenticated";
    private static ApplicationContext mBeanFactory;
    /*
     * static { Logging.addLogger(SystemPlugIn.class); }
     */

    /**
     * Constructor with configuration object
     *
     * @param aConfiguration
     */
    public SystemPlugIn(PluginConfiguration aConfiguration) {
        super(aConfiguration);
        if (mLog.isDebugEnabled()) {
            mLog.debug("Instantiating system plug-in...");
        }
        // specify default name space for system plugin
        this.setNamespace(NS_SYSTEM);
        mGetSettings();

        try {
            mBeanFactory = getConfigBeanFactory();
            if (null == mBeanFactory) {
                mLog.error("No or invalid spring configuration for system plug-in, some features may not be available.");
            } else {
                mAuthProvMgr = (ProviderManager) mBeanFactory.getBean("authManager");
                List<AuthenticationProvider> lProviders = mAuthProvMgr.getProviders();
                mAuthProv = lProviders.get(0);

                // sessionManager bean is not used in embedded mode and should not
                // be declared in this case
                if (mBeanFactory.containsBean("sessionManager")) {
                    mSessionManager = (SessionManager) mBeanFactory.getBean("sessionManager");
                }

                // give a success message to the administrator
                if (mLog.isInfoEnabled()) {
                    mLog.info("System plug-in successfully instantiated.");
                }
            }
        } catch (Exception lEx) {
            mLog.error(Logging.getSimpleExceptionMessage(lEx, "instantiating system plug-in"));
        }
    }

    /**
     *
     * @return
     */
    public AuthenticationProvider getAuthProvider() {
        return mAuthProv;
    }

    /**
     *
     * @param aAuthMgr
     */
    public void setAuthManager(AuthenticationProvider aAuthMgr) {
        mAuthProv = aAuthMgr;
    }

    private void mGetSettings() {
        // load global settings, default to "true"
        BROADCAST_OPEN = "true".equals(getString(BROADCAST_OPEN_KEY, "true"));
        BROADCAST_CLOSE = "true".equals(getString(BROADCAST_CLOSE_KEY, "true"));
        BROADCAST_LOGIN = "true".equals(getString(BROADCAST_LOGIN_KEY, "true"));
        BROADCAST_LOGOUT = "true".equals(getString(BROADCAST_LOGOUT_KEY, "true"));
        ALLOW_ANONYMOUS_LOGIN = "true".equals(getString(ALLOW_ANONYMOUS_KEY, "false"));
        ALLOW_AUTO_ANONYMOUS = "true".equals(getString(ALLOW_AUTO_ANONYMOUS_KEY, "false"));
        SecurityFactory.setAutoAnonymous(ALLOW_AUTO_ANONYMOUS);
    }

    @Override
    public void processToken(PlugInResponse aResponse,
            WebSocketConnector aConnector, Token aToken) {
        String lType = aToken.getType();
        String lNS = aToken.getNS();

        if (lType != null && getNamespace().equals(lNS)) {
            if (lType.equals(TT_SEND)) {
                send(aConnector, aToken);
            } else if (lType.equals(TT_RESPOND)) {
                respond(aConnector, aToken);
            } else if (lType.equals(TT_HEADER)) {
                getHeaders(aConnector, aToken);
            } else if (lType.equals(TT_BROADCAST)) {
                broadcast(aConnector, aToken);
            } else if (lType.equals(TT_LOGIN)) {
                login(aConnector, aToken);
            } else if (lType.equals(TT_LOGOUT)) {
                logout(aConnector, aToken);
            } else if (lType.equals(TT_LOGON)) {
                logon(aConnector, aToken);
            } else if (lType.equals(TT_LOGOFF)) {
                logoff(aConnector, aToken);
            } else if (lType.equals(TT_GET_AUTHORITIES)) {
                getAuthorities(aConnector, aToken);
            } else if (lType.equals(TT_CLOSE)) {
                close(aConnector, aToken);
            } else if (lType.equals(TT_GETCLIENTS)) {
                getClients(aConnector, aToken);
            } else if (lType.equals(TT_PING)) {
                ping(aConnector, aToken);
            } else if (lType.equals(TT_ECHO)) {
                echo(aConnector, aToken);
            } else if (lType.equals(TT_WAIT)) {
                wait(aConnector, aToken);
            } else if (lType.equals(TT_ALLOC_CHANNEL)) {
                allocChannel(aConnector, aToken);
            } else if (lType.equals(TT_DEALLOC_CHANNEL)) {
                deallocChannel(aConnector, aToken);
            }
            aResponse.abortChain();
        }
    }

    @Override
    public void connectorStarted(WebSocketConnector aConnector) {
        // Setting the session only if a session manager is defined,
        // ommitting if the session storage was previously setted (embedded mode)
        if (null != mSessionManager && null == aConnector.getSession().getStorage()) {
            try {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Creating the WebSocketSession persistent storage "
                            + "for connector '" + aConnector.getId() + "'...");
                }
                aConnector.getSession().
                        setStorage((Map<String, Object>) (mSessionManager.getSession(aConnector.getSession().getSessionId())));

                //Setting the username if exists in the connector instance
                Map<String, Object> lSessionParams = aConnector.getSession().getStorage();
                if (lSessionParams.containsKey(USERNAME)) {
                    aConnector.setUsername(lSessionParams.get(USERNAME).toString());
                }
            } catch (Exception lEx) {
                mLog.error(Logging.getSimpleExceptionMessage(lEx, "initializing connector session"), lEx);
            }
        }

        if (ALLOW_ANONYMOUS_LOGIN && null == aConnector.getUsername()) {
            setUsername(aConnector, ANONYMOUS_USER);
        }

        // sending the welcome token
        sendWelcome(aConnector);

        // if new connector is active broadcast this event to then network
        broadcastConnectEvent(aConnector);
    }

    @Override
    public void connectorStopped(WebSocketConnector aConnector, CloseReason aCloseReason) {
        // Allowing all connectors for a reconnection
        // Ommitting if running in embedded mode like a servlet container
        WebSocketSession lSession = aConnector.getSession();
        if (lSession.getStorage() != null && !(lSession.getStorage() instanceof HttpSessionStorage)
                && mSessionManager != null) {
            String lSessionId =
                    lSession.getSessionId();
            if (mLog.isDebugEnabled()) {
                mLog.debug("Putting the session: " + lSessionId
                        + ", in reconnection mode...");
            }
            synchronized (this) {
                //Removing the local cached  storage instance. 
                //Free space if the client never gets reconnected
                if (mSessionManager instanceof SessionManager) {
                    ((SessionManager) mSessionManager).getSessionsReferences().remove(lSessionId);
                }
                mSessionManager.getReconnectionManager().putInReconnectionMode(lSessionId);
            }
        }

        // notify other clients that client disconnected
        broadcastDisconnectEvent(aConnector);
    }

    private String getGroup(WebSocketConnector aConnector) {
        return aConnector.getString(VAR_GROUP);
    }

    private void setGroup(WebSocketConnector aConnector, String aGroup) {
        aConnector.setString(VAR_GROUP, aGroup);
    }

    private void removeGroup(WebSocketConnector aConnector) {
        aConnector.removeVar(VAR_GROUP);
    }

    /**
     *
     *
     * @param aConnector
     */
    public void broadcastConnectEvent(WebSocketConnector aConnector) {
        // only broadcast if corresponding global plugin setting is "true"
        if (BROADCAST_OPEN) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Broadcasting connect...");
            }
            // broadcast connect event to other clients of the jWebSocket network
            Token lConnect = TokenFactory.createToken(NS_SYSTEM, BaseToken.TT_EVENT);
            lConnect.setString("name", "connect");
            lConnect.setString("sourceId", aConnector.getId());
            // if a unique node id is specified for the client include that
            String lNodeId = aConnector.getNodeId();
            if (lNodeId != null) {
                lConnect.setString("unid", lNodeId);
            }
            lConnect.setInteger("clientCount", getConnectorCount());

            // broadcast to all except source
            broadcastToken(aConnector, lConnect);
        }
    }

    /**
     *
     *
     * @param aConnector
     */
    public void broadcastDisconnectEvent(WebSocketConnector aConnector) {
        // only broadcast if corresponding global plugin setting is "true"
        if (BROADCAST_CLOSE
                && !aConnector.getBool("noDisconnectBroadcast")) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Broadcasting disconnect...");
            }
            // broadcast connect event to other clients of the jWebSocket network
            Token lDisconnect = TokenFactory.createToken(NS_SYSTEM, BaseToken.TT_EVENT);
            lDisconnect.setString("name", "disconnect");
            lDisconnect.setString("sourceId", aConnector.getId());
            // if a unique node id is specified for the client include that
            String lNodeId = aConnector.getNodeId();
            if (lNodeId != null) {
                lDisconnect.setString("unid", lNodeId);
            }
            lDisconnect.setInteger("clientCount", getConnectorCount());

            // broadcast to all except source
            broadcastToken(aConnector, lDisconnect);
        }
    }

    private void sendWelcome(WebSocketConnector aConnector) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Sending welcome...");
        }
        // send "welcome" token to client
        Token lWelcome = TokenFactory.createToken(NS_SYSTEM, TT_WELCOME);
        lWelcome.setString("vendor", JWebSocketCommonConstants.VENDOR);
        lWelcome.setString("version", JWebSocketServerConstants.VERSION_STR);
        lWelcome.setString("sourceId", aConnector.getId());
        // if a unique node id is specified for the client include that
        String lNodeId = aConnector.getNodeId();
        if (lNodeId != null) {
            lWelcome.setString("unid", lNodeId);
        }
        lWelcome.setInteger("timeout", aConnector.getEngine().getConfiguration().getTimeout());
        String lUsername = getUsername(aConnector);
        if (lUsername != null) {
            lWelcome.setString("username", lUsername);
        }
        if (lNodeId != null) {
            lWelcome.setString("unid", lNodeId);
        }
        // to let the client know about the negotiated protocol
        lWelcome.setInteger("protocolVersion", aConnector.getVersion());
        // and negotiated sub protocol
        // TODO: The client does not get anything here!
        lWelcome.setString("subProtocol", aConnector.getSubprot());

        // if anoymous user allowed send corresponding flag for 
        // clarification that auto anonymous may have been applied.
        if (ALLOW_ANONYMOUS_LOGIN && ALLOW_AUTO_ANONYMOUS) {
            lWelcome.setBoolean(
                    "anonymous",
                    null != ANONYMOUS_USER
                    && ANONYMOUS_USER.equals(lUsername));
        }
        sendToken(aConnector, aConnector, lWelcome);
    }

    /**
     *
     */
    private void broadcastLoginEvent(WebSocketConnector aConnector) {
        // only broadcast if corresponding global plugin setting is "true"
        if (BROADCAST_LOGIN) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Broadcasting login event...");
            }
            // broadcast login event to other clients of the jWebSocket network
            Token lLogin = TokenFactory.createToken(NS_SYSTEM, BaseToken.TT_EVENT);
            lLogin.setString("name", "login");
            lLogin.setString("username", getUsername(aConnector));
            lLogin.setInteger("clientCount", getConnectorCount());
            lLogin.setString("sourceId", aConnector.getId());
            // if a unique node id is specified for the client include that
            String lNodeId = aConnector.getNodeId();
            if (lNodeId != null) {
                lLogin.setString("unid", lNodeId);
            }
            // broadcast to all except source
            broadcastToken(aConnector, lLogin);
        }
    }

    /**
     *
     */
    private void broadcastLogoutEvent(WebSocketConnector aConnector) {
        // only broadcast if corresponding global plugin setting is "true"
        if (BROADCAST_LOGOUT) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Broadcasting logout event...");
            }
            // broadcast login event to other clients of the jWebSocket network
            Token lLogout = TokenFactory.createToken(NS_SYSTEM, BaseToken.TT_EVENT);
            lLogout.setString("name", "logout");
            lLogout.setString("username", getUsername(aConnector));
            lLogout.setInteger("clientCount", getConnectorCount());
            lLogout.setString("sourceId", aConnector.getId());
            // if a unique node id is specified for the client include that
            String lNodeId = aConnector.getNodeId();
            if (lNodeId != null) {
                lLogout.setString("unid", lNodeId);
            }
            // broadcast to all except source
            broadcastToken(aConnector, lLogout);
        }
    }

    /**
     *
     * @param aConnector
     * @param aCloseReason
     */
    private void sendGoodBye(WebSocketConnector aConnector, CloseReason aCloseReason) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Sending good bye...");
        }
        // send "goodBye" token to client
        Token lGoodBye = TokenFactory.createToken(TT_GOODBYE);
        lGoodBye.setString("ns", getNamespace());
        lGoodBye.setString("vendor", JWebSocketCommonConstants.VENDOR);
        lGoodBye.setString("version", JWebSocketServerConstants.VERSION_STR);
        lGoodBye.setString("sourceId", aConnector.getId());
        if (aCloseReason != null) {
            lGoodBye.setString("reason", aCloseReason.toString().toLowerCase());
        }

        // don't send session-id on good bye, neither required nor desired
        sendToken(aConnector, aConnector, lGoodBye);
    }

    private void login(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        String lUsername = aToken.getString("username");
        // TODO: Add authentication and password check
        String lPassword = aToken.getString("password");
        String lEncoding = aToken.getString("encoding");

        String lGroup = aToken.getString("group");
        Boolean lReturnRoles = aToken.getBoolean("getRoles", Boolean.FALSE);
        Boolean lReturnRights = aToken.getBoolean("getRights", Boolean.FALSE);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'login' (username='" + lUsername
                    + "', group='" + lGroup
                    + "') from '" + aConnector + "'...");
        }

        if (lUsername != null) {

            // try to get user from security factory
            User lUser = SecurityFactory.getUser(lUsername);
            if (null == lUser && ALLOW_AUTO_ANONYMOUS) {
                // if user not found and auto anonymous user selected, 
                // try to pick anonymous user
                lUser = SecurityFactory.getUser(ANONYMOUS_USER);
            }

            // check if user exists and if password matches
            if (lUser != null && lUser.checkPassword(lPassword, lEncoding)) {
                lResponse.setString("username", lUsername);
                // if previous session id was passed to continue an aborted session
                // return the session-id to notify client about acceptance
                lResponse.setString("sourceId", aConnector.getId());
                // set shared variables
                setUsername(aConnector, lUsername);
                setGroup(aConnector, lGroup);

                if (lUser != null) {
                    if (lReturnRoles) {
                        lResponse.setList("roles", new FastList(lUser.getRoleIdSet()));
                    }
                    if (lReturnRights) {
                        lResponse.setList("rights", new FastList(lUser.getRightIdSet()));
                    }
                }
                if (mLog.isInfoEnabled()) {
                    mLog.info("User '" + lUsername
                            + "' successfully logged in from "
                            + aConnector.getRemoteHost() + " ("
                            + aConnector.getId() + ").");
                }
            } else {
                mLog.warn("Attempt to login with invalid credentials, username '" + lUsername + "'.");
                lResponse.setInteger("code", -1);
                lResponse.setString("msg", "Invalid credentials");
                // reset username to not send login event, see below
                lUsername = null;
            }
        } else {
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", "Missing arguments for 'login' command");
        }

        // send response to client
        sendToken(aConnector, aConnector, lResponse);

        // if successfully logged in...
        if (lUsername != null) {
            // broadcast "login event" to other clients
            broadcastLoginEvent(aConnector);
        }
    }

    private void logout(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'logout' (username='"
                    + getUsername(aConnector)
                    + "') from '" + aConnector + "'...");
        }

        String lUsername = getUsername(aConnector);
        if (null != lUsername) {
            // send normal answer token, good bye is for close!
            // if anoymous user allowed send corresponding flag for 
            // clarification that auto anonymous may have been applied.
            if (ALLOW_ANONYMOUS_LOGIN && ALLOW_AUTO_ANONYMOUS) {
                lResponse.setBoolean(
                        "anonymous",
                        null != ANONYMOUS_USER
                        && ANONYMOUS_USER.equals(lUsername));
            }
            sendToken(aConnector, aConnector, lResponse);
            // send good bye token as response to client
            // sendGoodBye(aConnector, CloseReason.CLIENT);

            // and broadcast the logout event
            broadcastLogoutEvent(aConnector);
            // resetting the username is the only required signal for logout
            lResponse.setString("sourceId", aConnector.getId());
            removeUsername(aConnector);
            removeGroup(aConnector);

            // log successful logout operation
            if (mLog.isInfoEnabled()) {
                mLog.info("User '" + lUsername
                        + "' successfully logged out from "
                        + aConnector.getRemoteHost() + " ("
                        + aConnector.getId() + ").");
            }
        } else {
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", "not logged in");
            sendToken(aConnector, aConnector, lResponse);
        }
    }

    private void send(WebSocketConnector aConnector, Token aToken) {
        // check if user is allowed to run 'send' command
        if (!SecurityFactory.hasRight(getUsername(aConnector), NS_SYSTEM + ".send")) {
            sendToken(aConnector, aConnector, createAccessDenied(aToken));
            return;
        }
        Token lResponse = createResponse(aToken);

        WebSocketConnector lTargetConnector;
        String lTargetId = aToken.getString("unid");
        String lTargetType;
        if (lTargetId != null) {
            lTargetConnector = getNode(lTargetId);
            lTargetType = "node-id";
        } else {
            // get the target
            lTargetId = aToken.getString("targetId");
            lTargetConnector = getConnector(lTargetId);
            lTargetType = "client-id";
        }

        /*
         * if (getUsername(aConnector) != null) {
         */
        if (lTargetConnector != null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Processing 'send' (username='"
                        + getUsername(aConnector)
                        + "') from '" + aConnector
                        + "' to " + lTargetId + "...");
            }

            aToken.setString("sourceId", aConnector.getId());
            sendToken(aConnector, lTargetConnector, aToken);
        } else {
            String lMsg = "No target connector with "
                    + lTargetType + " '"
                    + lTargetId + "' found.";
            mLog.warn(lMsg);
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", lMsg);
            sendToken(aConnector, aConnector, lResponse);
        }
    }

    private void respond(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        WebSocketConnector lTargetConnector;
        String lTargetId = aToken.getString("unid");
        String lTargetType;
        if (lTargetId != null) {
            lTargetConnector = getNode(lTargetId);
            lTargetType = "node-id";
        } else {
            // get the target
            lTargetId = aToken.getString("targetId");
            lTargetConnector = getConnector(lTargetId);
            lTargetType = "client-id";
        }

        if (lTargetConnector != null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Processing 'respond' (username='"
                        + getUsername(aConnector)
                        + "') from '" + aConnector
                        + "' to " + lTargetId + "...");
            }
            aToken.setType("response");
            aToken.setString("sourceId", aConnector.getId());
            sendToken(aConnector, lTargetConnector, aToken);
        } else {
            String lMsg = "No target connector with "
                    + lTargetType + " '"
                    + lTargetId + "' found.";
            mLog.warn(lMsg);
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", lMsg);
            sendToken(aConnector, aConnector, lResponse);
        }
    }

    private void broadcast(WebSocketConnector aConnector, Token aToken) {

        // check if user is allowed to run 'broadcast' command
        if (!SecurityFactory.hasRight(getUsername(aConnector), NS_SYSTEM + ".broadcast")) {
            sendToken(aConnector, aConnector, createAccessDenied(aToken));
            return;
        }

        Token lResponse = createResponse(aToken);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'broadcast' (username='"
                    + getUsername(aConnector)
                    + "') from '" + aConnector + "'...");
        }
        /*
         * if (getUsername(aConnector) != null) {
         */
        aToken.setString("sourceId", aConnector.getId());
        // keep senderIncluded beging false as default, apps rely on this!
        Boolean lIsSenderIncluded = aToken.getBoolean("senderIncluded", false);
        Boolean lIsResponseRequested = aToken.getBoolean("responseRequested", true);

        // remove further non target related fields
        aToken.remove("senderIncluded");
        aToken.remove("responseRequested");

        // broadcast the token
        broadcastToken(aConnector, aToken,
                new BroadcastOptions(lIsSenderIncluded, lIsResponseRequested));

        // check if response was requested
        if (lIsResponseRequested) {
            sendToken(aConnector, aConnector, lResponse);
        }
        /*
         * } else { lResponse.put("code", -1); lResponse.put("msg", "not logged
         * in"); sendToken(aConnector, lResponse); }
         */
    }

    private void close(WebSocketConnector aConnector, Token aToken) {
        int lTimeout = aToken.getInteger("timeout", 0);

        Boolean lNoGoodBye =
                aToken.getBoolean("noGoodBye", false);
        Boolean lNoLogoutBroadcast =
                aToken.getBoolean("noLogoutBroadcast", false);
        Boolean lNoDisconnectBroadcast =
                aToken.getBoolean("noDisconnectBroadcast", false);

        // only send a good bye message if timeout is > 0 and not to be noed
        if (lTimeout > 0 && !lNoGoodBye) {
            sendGoodBye(aConnector, CloseReason.CLIENT);
        }
        // if logged in...
        if (getUsername(aConnector) != null && !lNoLogoutBroadcast) {
            // broadcast the logout event.
            broadcastLogoutEvent(aConnector);
        }
        // reset the username, we're no longer logged in
        removeUsername(aConnector);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Closing client " + (lTimeout > 0 ? "with timeout " + lTimeout + "ms" : "immediately") + "...");
        }

        // don't send a response here! We're about to close the connection!
        // broadcasts disconnect event to other clients
        // if not explicitely noed
        aConnector.setBoolean("noDisconnectBroadcast", lNoDisconnectBroadcast);
        aConnector.stopConnector(CloseReason.CLIENT);
    }

    /**
     *
     * @param aToken
     */
    private void echo(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        String lData = aToken.getString("data");
        if (lData != null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("echo " + lData);
            }
            lResponse.setString("data", lData);
        } else {
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", "missing 'data' argument for 'echo' command");
        }
        
        /*
        String lString = "{\"data\":{\"presetIdentifier\":\"DefaultPreset\", \"context\":\"tuner\", \"presetEntries\":[{\"entryIdentifier\":\"100.3 MHz\", \"imageIdentifier\":\"100.3 MHz\", \"entryName\":\"FFN\"}, null]}}";
        Token lTestToken = JSONProcessor.jsonStringToToken(lString);
        sendToken(aConnector, aConnector, lTestToken);
        
        
        List lTestList = new FastList();
        Map lTestMap = new FastMap();
        lTestMap.put("field1", "test1");
        lTestMap.put("field2", "null");
        lTestList.add(lTestMap);
        lTestList.add(null);
        lResponse.setMap("data", lTestMap);
        lResponse.setList("list", lTestList);

        Token lToken = TokenFactory.createToken();
        lToken.setMap("data", lTestMap);
        lToken.setList("list", lTestList);
        sendToken(aConnector, aConnector, lToken);
        */

        sendToken(aConnector, aConnector, lResponse);
    }

    /**
     *
     * @param aConnector
     * @param aToken
     */
    public void ping(WebSocketConnector aConnector, Token aToken) {
        Boolean lEcho = aToken.getBoolean("echo", Boolean.TRUE);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'Ping' (echo='" + lEcho
                    + "') from '" + aConnector + "'...");
        }

        if (lEcho) {
            Token lResponse = createResponse(aToken);
            // TODO: here could we optionally send a time stamp
            // TODO: implement response time on client!
            // lResponseToken.put("","");
            sendToken(aConnector, aConnector, lResponse);
        }
    }

    /**
     * simply waits for a certain amount of time and does not perform any _
     * operation. This feature is used for debugging and simulation purposes _
     * only and is not related to any business logic.
     *
     * @param aToken
     */
    private void wait(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        Integer lDuration = aToken.getInteger("duration", 0);
        Boolean lIsResponseRequested = aToken.getBoolean("responseRequested", true);
        if (lDuration != null && lDuration >= 0) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("duration " + lDuration);
            }
            try {
                Thread.sleep(lDuration);
            } catch (Exception lEx) {
                // ignore potential exception here!
            }
            lResponse.setInteger("duration", lDuration);
        } else {
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", "missing or invalid 'duration' argument for 'wait' command");
        }

        // for test purposes we need to optionally suppress a response
        // to simulate this error condition
        if (lIsResponseRequested) {
            sendToken(aConnector, aConnector, lResponse);
        }
    }

    /**
     * Gets the client headers and put them into connector variables
     *
     * @param aConnector
     * @param aToken
     */
    private void getHeaders(WebSocketConnector aConnector, Token aToken) {
        aConnector.setVar("clientType", aToken.getString("clientType"));
        aConnector.setVar("clientName", aToken.getString("clientName"));
        aConnector.setVar("clientVersion", aToken.getString("clientVersion"));
        aConnector.setVar("clientInfo", aToken.getString("clientInfo"));
        aConnector.setVar("jwsType", aToken.getString("jwsType"));
        aConnector.setVar("jwsVersion", aToken.getString("jwsVersion"));
        aConnector.setVar("jwsInfo", aToken.getString("jwsInfo"));
        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'getHeaders' from connector '"
                    + aConnector.getId() + "'...");
        }
    }

    /**
     *
     * @param aConnector
     * @param aToken
     */
    public void getClients(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'getClients' from '"
                    + aConnector + "'...");
        }

        if (getUsername(aConnector) != null) {
            String lGroup = aToken.getString("group");
            Integer lMode = aToken.getInteger("mode", 0);
            FastMap lFilter = new FastMap();
            lFilter.put(BaseConnector.VAR_USERNAME, ".*");
            List<String> listOut = new FastList<String>();
            for (WebSocketConnector lConnector : getServer().selectConnectors(lFilter).values()) {
                listOut.add(getUsername(lConnector) + "@" + lConnector.getId());
            }
            lResponse.setList("clients", listOut);
            lResponse.setInteger("count", listOut.size());
        } else {
            lResponse.setInteger("code", -1);
            lResponse.setString("msg", "not logged in");
        }

        sendToken(aConnector, aConnector, lResponse);
    }

    /**
     * allocates a "non-interruptable" communication channel between two
     * clients.
     *
     * @param aConnector
     * @param aToken
     */
    public void allocChannel(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'allocChannel' from '"
                    + aConnector + "'...");
        }
    }

    /**
     * deallocates a "non-interruptable" communication channel between two
     * clients.
     *
     * @param aConnector
     * @param aToken
     */
    public void deallocChannel(WebSocketConnector aConnector, Token aToken) {
        Token lResponse = createResponse(aToken);

        if (mLog.isDebugEnabled()) {
            mLog.debug("Processing 'deallocChannel' from '"
                    + aConnector + "'...");
        }
    }

    /**
     * Logon a user given the username and password by using the Spring Security
     * module
     *
     * @param aConnector
     * @param aToken The token with the username and password
     */
    void logon(WebSocketConnector aConnector, Token aToken) {
        TokenServer lServer = getServer();
        if (SecurityHelper.isUserAuthenticated(aConnector)) {
            lServer.sendToken(aConnector,
                    lServer.createErrorToken(
                    aToken, -1, "Is authenticated already, logoff first!"));
            return;
        }

        String lUsername = aToken.getString("username");
        String lPassword = aToken.getString("password");

        if (mLog.isDebugEnabled()) {
            mLog.debug("Starting authentication ...");
        }
        
        Authentication lAuthRequest = new UsernamePasswordAuthenticationToken(lUsername, lPassword);
        Authentication lAuthResult;
        try {
            AuthenticationProvider lAuthProvider = getAuthProvider();
            lAuthResult = lAuthProvider.authenticate(lAuthRequest);
        } catch (Exception ex) {
            String lMsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            Token lResponse = getServer().createErrorToken(aToken, -1, lMsg);
            sendToken(aConnector, aConnector, lResponse);
            if (mLog.isDebugEnabled()) {
                mLog.debug(lMsg);
            }
            return; // Stop the execution flow
        }

        if (mLog.isDebugEnabled()) {
            mLog.debug("Authentication Successfully. Updating the user session...");
        }
        // Getting the session
        Map<String, Object> lSessionParms = aConnector.getSession().getStorage();

        // Setting the is_authenticated flag
        lSessionParms.put(IS_AUTHENTICATED, lAuthResult.isAuthenticated());

        // Setting the username
        lSessionParms.put(USERNAME, lUsername);
        aConnector.setUsername(lUsername);

        // Setting the uuid
        String lUUID;
        Object lDetails = lAuthResult.getDetails();
        if (null != lDetails && lDetails instanceof IUserUniqueIdentifierContainer) {
            lUUID = ((IUserUniqueIdentifierContainer) lDetails).getUUID();
        } else {
            lUUID = lUsername;
        }
        lSessionParms.put(UUID, lUUID);

        // Setting the authorities
        String lAuthorities = "";
        for (GrantedAuthority lGA : lAuthResult.getAuthorities()) {
            lAuthorities = lAuthorities.concat(lGA.getAuthority() + " ");
        }
        // Storing the user authorities as a string to avoid serialization problems
        lSessionParms.put(AUTHORITIES, lAuthorities);

        // Creating the response
        Token response = createResponse(aToken);
        response.setString("uuid", lUUID);
        response.setString("username", lUsername);
        response.setList("authorities", Tools.parseStringArrayToList(lAuthorities.split(" ")));

        // Sending the response
        sendToken(aConnector, aConnector, response);
        if (mLog.isDebugEnabled()) {
            mLog.debug("Logon process finished successfully!");
        }

        // if successfully logged in...
        if (lUsername != null) {
            // broadcast "login event" to other clients
            broadcastLoginEvent(aConnector);
        }
    }

    void logoff(WebSocketConnector aConnector, Token aToken) {
        if (!SecurityHelper.isUserAuthenticated(aConnector)) {
            getServer().sendToken(aConnector, getServer().createNotAuthToken(aToken));
            return;
        }

        //Cleaning the session
        aConnector.getSession().getStorage().clear();
        aConnector.removeUsername();

        //Sending the response
        getServer().sendToken(aConnector, createResponse(aToken));
        if (mLog.isDebugEnabled()) {
            mLog.debug("Logoff process finished successfully!");
        }
    }

    void getAuthorities(WebSocketConnector aConnector, Token aToken) {
        TokenServer lServer = getServer();
        if (!SecurityHelper.isUserAuthenticated(aConnector)) {
            sendToken(aConnector, aConnector, lServer.createNotAuthToken(aToken));
            return;
        }

        String lUsername = aConnector.getUsername();
        Map<String, Object> lSessionParams = aConnector.getSession().getStorage();
        String lAuthorities = (String) lSessionParams.get(AUTHORITIES);

        // Creating the response
        Token lResponse = createResponse(aToken);
        lResponse.setString("username", lUsername);
        lResponse.setList("authorities", Tools.parseStringArrayToList(lAuthorities.split(" ")));

        // Sending the response
        sendToken(aConnector, aConnector, lResponse);
    }
}
