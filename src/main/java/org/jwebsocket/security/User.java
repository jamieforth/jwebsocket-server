//    ---------------------------------------------------------------------------
//    jWebSocket - User Class
//    Copyright (c) 2010 jWebSocket.org, Alexander Schulze, Innotrade GmbH
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
package org.jwebsocket.security;

import java.util.Set;
import org.apache.log4j.Logger;
import org.jwebsocket.util.Tools;

/**
 * implements a user with all its data fields and roles.
 * @author aschulze
 */
public class User {

    private static Logger log = Logger.getLogger(User.class);
    /**
     * The maximum number of login tries until the account gets locked.
     */
    public final static int MAX_PWD_FAIL_COUNT = 3;
    /**
     * The state of the user is unknown. This state is used only as default
     * when instantiating a new user. This value should not be saved.
     */
    public final static int ST_UNKNOWN = -1;
    /**
     * The user is already registered but not activated.
     * A user needs to get activated to get access to the system.
     */
    public final static int ST_REGISTERED = 0;
    /**
     * The user is activated and has access to the system according to his 
     * rights and roles.
     */
    public final static int ST_ACTIVE = 1;
    /**
     * The user is (temporarily) inactive.
     * He needs to get (re-)activated to get access to the system.
     */
    public final static int ST_INACTIVE = 2;
    /**
     * The user is (temporarily) locked, eg due to too much logins 
     * with wrong credentials.
     * He needs to gets unlocked again to get access to the system.
     */
    public final static int ST_LOCKED = 3;
    /**
     * The user is deleted, he can't log in and is not reachable for others.
     * The row is kept in the database for reference purposes only and
     * to keep the database consistent (eg for logs, journal or transactions).
     * He can be activated again to get access to the system.
     */
    public final static int ST_DELETED = 4;
    private Integer mUserId = null;
    private String mUUID = null;
    private String mLoginname = null;
    private String mTitle = null;
    private String mCompany = null;
    private String mFirstname = null;
    private String mLastname = null;
    private String mPassword = null;
    private Integer mPwdFailCount = 0;
    private int mStatus = ST_UNKNOWN;
    private String mDefaultLocale = null;
    private String mCity = null;
    private String mAddress = null;
    private String mZipCode = null;
    private String mCountryCode = null;
    private String mEmail = null;
    private String mPhone = null;
    private String mFax = null;
    private int mSessionTimeout = 0;
    private Roles mRoles = new Roles();

    /**
     * creates a new user instance by loginname, firstname, lastname, password
     * and roles.
     * @param aLoginName
     * @param aFirstname
     * @param aLastname
     * @param aPassword
     * @param aRoles
     */
    public User(String aUUID, String aLoginName, String aFirstname, String aLastname, String aPassword, Roles aRoles) {
        // if no UUID is passed generate a temporary one
        if (null == aUUID) {
            mUUID = generateUUID();
        } else {
            mUUID = aUUID;
        }
        mLoginname = aLoginName;
        mFirstname = aFirstname;
        mLastname = aLastname;
        mPassword = aPassword;
        mRoles = aRoles;
    }

    /**
     * returns the id of the user. The id is supposed to be used for storing
     * users in a database. It's the primary key.
     * @return id of the user.
     */
    public Integer getUserId() {
        return mUserId;
    }

    /**
     * specifies the id of the user. The id is supposed to be used for storing
     * users in a database. It's the primary key.
     * @param userId
     */
    public void setUserId(Integer userId) {
        this.mUserId = userId;
    }

    /**
     * @return the UUID
     */
    public String getUUID() {
        return mUUID;
    }

    /**
     * @param aUUID the UUID to set
     */
    public void setUUID(String aUUID) {
        this.mUUID = aUUID;
    }

    public static String generateUUID() {
        String lData = Double.toString(Math.random()) + Double.toString(System.nanoTime());
        return Tools.getMD5(lData);
    }

    /**
     * returns the login name of the user. The login name needs to be unique
     * for each user. It's the key to identify a user within jWebSocket.
     * @return
     */
    public String getLoginname() {
        return mLoginname;
    }

    /**
     * specifies the login name of the user. The login name needs to be unique
     * for each user. It's the key to identify a user within jWebSocket.
     * @param aLoginName
     */
    public void setLoginname(String aLoginName) {
        this.mLoginname = aLoginName;
    }

    /**
     * returns the title of the user (e.g. mr./mrs.).
     * @return the title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * specifies the title of the user (e.g. mr./mrs.).
     * @param aTitle
     */
    public void setTitle(String aTitle) {
        this.mTitle = aTitle;
    }

    /**
     * returns the company of the user.
     * @return the company
     */
    public String getCompany() {
        return mCompany;
    }

    /**
     * specifies the company of the user.
     * @param company the company to set
     */
    public void setCompany(String company) {
        this.mCompany = company;
    }

    /**
     * returns the firstname of the user.
     * @return
     */
    public String getFirstname() {
        return mFirstname;
    }

    /**
     * specifies the firstname of the user.
     * @param firstName
     */
    public void setFirstname(String firstName) {
        this.mFirstname = firstName;
    }

    /**
     * returns the lastname of the user.
     * @return
     */
    public String getLastname() {
        return mLastname;
    }

    /**
     * specifies the lastname of the user.
     * @param lastName
     */
    public void setLastname(String lastName) {
        this.mLastname = lastName;
    }

    /**
     * checks if the given password matches the user password, it is not
     * possible to obtain the password for the user. If the password is not
     * correct the fail counter is incremented. If the fail counter exceeds
     * the configured maximum the account gets locked. If the password is
     * correct the password fail counter is reseted.
     * @param aPassword
     * @return
     */
    public boolean checkPassword(String aPassword, String aEncoding) {
        String lPassword = mPassword;
        if ("md5".equalsIgnoreCase(aEncoding)) {
            lPassword = Tools.getMD5(lPassword);
        }
        boolean lOk = (aPassword != null && aPassword.equals(lPassword));
        if (lOk) {
            resetPwdFailCount();
        } else {
            incPwdFailCount();
        }
        return lOk;

    }

    /**
     * changes the password of the user, to change it the caller needs to know
     * the original password. For initialization purposes e.g. during the
     * startup process the original password is null.
     * The password cannot be reset to null.
     * @param aOldPW original password or null of password is set first time.
     * @param aNewPW new password, nust not be <tt>null</tt>.
     * @return true if the password was changed successfully otherwise false.
     */
    public boolean changePassword(String aOldPW, String aNewPW) {
        if (aOldPW != null
                && aNewPW != null
                && mPassword.equals(aOldPW)) {
            mPassword = aNewPW;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return mLoginname + ": " + mFirstname + " " + mLastname;
    }

    // TODO: potential security hole: don't allow to change roles w/o a special permission!
    /**
     * specifies the roles of the user.
     * @param aRoles
     */
    public void setRoles(Roles aRoles) {
        this.mRoles = aRoles;
    }

    /**
     * returns the user's current status (one of the ST_XXX constants).
     * @return
     */
    public int getStatus() {
        return mStatus;
    }

    // TODO: potential security hole: don't allow to e.g. unlock a user w/o a special permission!
    /**
     * specifies the user's current status (one of the ST_XXX constants).
     * @param status
     */
    public void setStatus(int status) {
        this.mStatus = status;
    }

    /**
     * returns the user's current password fail counter. Please consider that
     * since currently the users are stored in memory only the fail counter
     * is reset after an application restart.
     * @return
     */
    public Integer getPwdFailCount() {
        return mPwdFailCount;
    }

    // TODO: potential security hole: don't allow to reset password fail counter w/o a special permission!
    /**
     * explicitly sets the password fail counter for the user.
     * @param aPwdFailCount
     */
    public void setPwdFailCount(Integer aPwdFailCount) {
        mPwdFailCount = aPwdFailCount;
    }

    // TODO: potential security hole: don't allow to roll over password fail counter!
    /**
     * increments the password fail counter. If the password fail counter
     * exceeds the maximum value the user gets locked.
     * This is called after the application passed an incorrect password to
     * the checkPassword method.
     * @return 
     */
    private Integer incPwdFailCount() {
        setPwdFailCount(mPwdFailCount + 1);
        if (mPwdFailCount >= MAX_PWD_FAIL_COUNT) {
            lock();
        }
        return mPwdFailCount;
    }

    // TODO: potential security hole: don't allow to reset password fail counter w/o a special permission!
    /**
     * resets the password fail counter and saves the user back to the database.
     * This is called after a successful authentication.
     */
    private void resetPwdFailCount() {
        setPwdFailCount(0);
    }

    /**
     * returns the default locale for the user.For future use in an
     * internationalized environment.
     * @return
     */
    public String getDefaultLocale() {
        return mDefaultLocale;
    }

    /**
     * specifies the default locale for the user. For future use in an
     * internationalized environment.
     * @param default_locale
     */
    public void setDefaultLocale(String default_locale) {
        this.mDefaultLocale = default_locale;
    }

    /**
     * returns the city of the user.
     * @return
     */
    public String getCity() {
        return mCity;
    }

    /**
     * specifies the city of the user.
     * @param city
     */
    public void setCity(String city) {
        this.mCity = city;
    }

    /**
     * returns the address of the user.
     * @return
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * specifies the address of the user.
     * @param address
     */
    public void setAddress(String address) {
        this.mAddress = address;
    }

    /**
     * returns the zip code of the user.
     * @return
     */
    public String getZipcode() {
        return mZipCode;
    }

    /**
     * specifies the zip code of the user.
     * @param zipcode
     */
    public void setZipcode(String zipcode) {
        this.mZipCode = zipcode;
    }

    /**
     * returns the country code of the user, either 2 digits like "DE" or
     * 5 digits like "EN-US".
     * @return
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * specifies the country code of the user, either 2 digits like "DE" or
     * 5 digits like "EN-US".
     * @param country_code
     */
    public void setCountryCode(String country_code) {
        this.mCountryCode = country_code;
    }

    /**
     * returns the phone number of the user.
     * @return
     */
    public String getPhone() {
        return mPhone;
    }

    /**
     * specifies the phone number of the user.
     * @param aPhone
     */
    public void setPhone(String aPhone) {
        this.mPhone = aPhone;
    }

    /**
     * returns the email address of the user.
     * @return
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * specifies the email address of the user.
     * @param aEmail
     */
    public void setEmail(String aEmail) {
        this.mEmail = aEmail;
    }

    /**
     * returns the fax number of the user.
     * @return
     */
    public String getFax() {
        return mFax;
    }

    /**
     * specifies the fax number of the user.
     * @param aFax
     */
    public void setFax(String aFax) {
        this.mFax = aFax;
    }

    /**
     * returns the individual session timeout for the user -
     * not yet supported.
     * @return
     */
    public int getSessionTimeout() {
        return mSessionTimeout;
    }

    /**
     * specifies the individual session timeout for the user -
     * not yet supported.
     * @param session_timeout
     */
    public void setSessionTimeout(int session_timeout) {
        this.mSessionTimeout = session_timeout;
    }

    /**
     * sets the user to locked state. He cannot login anymore after that.
     */
    public void lock() {
        this.setStatus(ST_LOCKED);
    }

    // TODO: potential security hole: don't allow to unlock account w/o a special permission!
    /**
     * Releases the user's locked state. He cannot login again after that.
     */
    public void unlock() {
        this.setStatus(ST_ACTIVE);
    }

    /**
     * checks if the user has a certain right. The right is passed as a string
     * which associates the key of the right.
     * @param aRight
     * @return
     */
    public boolean hasRight(String aRight) {
        return mRoles.hasRight(aRight);
    }

    /**
     * checks if the user has a certain role. The role is passed as a string
     * which associates the key of the role.
     * @param aRole
     * @return
     */
    public boolean hasRole(String aRole) {
        return mRoles.hasRole(aRole);
    }

    /**
     * returns the roles of the user.
     * @return
     */
    public Roles getRoles() {
        return mRoles;
    }

    /**
     * returns an unmodifiable set of rights for this user instance.
     * @return
     */
    public Rights getRights(String aNamespace) {
        // the getRights method of the Roles class already delivers an
        // unmodifiable set of rights
        Rights lRights = new Rights();
        for (Right lRight : mRoles.getRights()) {
            if (aNamespace == null || lRight.getId().startsWith(aNamespace)) {
                lRights.addRight(lRight);
            }
        }
        return lRights;
    }

    /**
     * returns an unmodifiable set of rights for this user instance.
     * @return
     */
    public Rights getRights() {
        return getRights(null);
    }

    /**
     * returns an unmodifiable set of role ids for this user instance.
     * @return
     */
    public Set<String> getRoleIdSet() {
        return mRoles.getRoleIdSet();
    }

    /**
     * returns an unmodifiable set of right ids for this user instance.
     * @return
     */
    public Set<String> getRightIdSet() {
        return getRights().getRightIdSet();
    }
}
