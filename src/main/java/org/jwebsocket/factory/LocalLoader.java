//        ---------------------------------------------------------------------------
//        jWebSocket - Copyright (c) 2010 jwebsocket.org
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
package org.jwebsocket.factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLClassLoader;
import org.apache.log4j.Logger;
import org.jwebsocket.logging.Logging;
import org.xeustechnologies.jcl.ClasspathResources;

/**
 * ClassLoader that reloads locally the classes from the jars. Plugins and Filters
 * all configured via jWebSocket.xml file is loaded using this class.
 *
 * @author Marcos Antonio González Huerta (markos0886, UCI)
 */
public class LocalLoader extends ClassLoader {
        
        private static Logger mLog = Logging.getLogger();
        protected final ClasspathResources mClasspathResources;
        private char mClassNameReplacementChar;
        private URLClassLoader mParent;

        public LocalLoader(URLClassLoader aParent) {
                mClasspathResources = new ClasspathResources();
                this.mParent = aParent;
        }
        
        public char getClassNameReplacementChar() {
        return mClassNameReplacementChar;
    }

    public void setClassNameReplacementChar(char aClassNameReplacementChar) {
        this.mClassNameReplacementChar = aClassNameReplacementChar;
    }

        public void loadJar(String aJarFile){
                mClasspathResources.loadJar(aJarFile);
        }
        
        @Override
        public Class loadClass(String aClassName, boolean aResolveIt) throws ClassNotFoundException {
                Class lResult = null;
                byte[] lClassBytes;

                lClassBytes = loadClassBytes(aClassName);
                if (lClassBytes == null) {
                        return mParent.loadClass(aClassName);
                }

                lResult = defineClass(aClassName, lClassBytes, 0, lClassBytes.length);

                if (lResult == null) {
                        return null;
                }

                /*
                 * Preserve package name.
                 */
                if (lResult.getPackage() == null) {
                        String lPackageName = aClassName.substring(0, aClassName.lastIndexOf('.'));
                        definePackage(lPackageName, null, null, null, null, null, null, null);
                }

                if (aResolveIt) {
                        resolveClass(lResult);
                }

                if (mLog.isTraceEnabled()) {
                        mLog.trace("Return new local loaded class " + aClassName);
                }
                return lResult;
        }

        public InputStream loadResource(String aName) {
                byte[] lArr = mClasspathResources.getResource(aName);
                if (lArr != null) {
                        if (mLog.isTraceEnabled()) {
                                mLog.trace("Returning newly loaded resource " + aName);
                        }

                        return new ByteArrayInputStream(lArr);
                }

                return null;
        }
        
        /**
     * Reads the class bytes from different local and remote resources using
     * ClasspathResources
     * 
     * @param aClassName
     * @return byte[]
     */
    protected byte[] loadClassBytes(String aClassName) {
        aClassName = formatClassName( aClassName );

        return mClasspathResources.getResource( aClassName );
    }
        
        /**
     * @param aClassName
     * @return String
     */
    protected String formatClassName(String aClassName) {
        aClassName = aClassName.replace( '/', '~' );

        if( mClassNameReplacementChar == '\u0000' ) {
            // '/' is used to map the package to the path
            aClassName = aClassName.replace( '.', '/' ) + ".class";
        } else {
            // Replace '.' with custom char, such as '_'
            aClassName = aClassName.replace( '.', mClassNameReplacementChar ) + ".class";
        }

        aClassName = aClassName.replace( '~', '/' );
        return aClassName;
    }
}
