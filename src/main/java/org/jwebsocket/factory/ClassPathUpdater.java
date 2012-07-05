//        ---------------------------------------------------------------------------
//        jWebSocket - Copyright (c) 2012 Innotrade GmbH, jWebSocket.org
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Allows programs to modify the classpath during runtime.
 */
public class ClassPathUpdater {

        /**
         * Used to find the method signature.
         */
        private static final Class[] PARAMETERS = new Class[]{URL.class};
        /**
         * Class containing the private addURL method.
         */
        private static final Class<?> CLASS_LOADER = URLClassLoader.class;

        /**
         * Adds a new path to the classloader. If the given string points to a file,
         * then that file's parent file (i.e. directory) is used as the directory to
         * add to the classpath. If the given string represents a directory, then
         * the directory is directly added to the classpath.
         *
         * @param aDir The directory to add to the classpath (or a file, which will
         * relegate to its directory).
         * @throws IOException 
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException  
         */
        public static void add(String aDir)
                        throws IOException, NoSuchMethodException, IllegalAccessException,
                        InvocationTargetException {
                add(new File(aDir));
        }

        /**
         * Adds a new path to the classloader. If the given file object is a file,
         * then its parent file (i.e., directory) is used as the directory to add to
         * the classpath. If the given string represents a directory, then the
         * directory it represents is added.
         *
         * @param aFile The directory (or enclosing directory if a file) to add to
         * the classpath.
         * @throws IOException 
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException  
         */
        public static void add(File aFile)
                        throws IOException, NoSuchMethodException, IllegalAccessException,
                        InvocationTargetException {
                aFile = aFile.isDirectory() ? aFile : aFile.getParentFile();
                add(aFile.toURI().toURL());
        }

        /**
         * 
         * @param aFile
         * @throws IOException
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public static void addJar(File aFile)
                        throws IOException, NoSuchMethodException, IllegalAccessException,
                        InvocationTargetException {
                add(aFile.toURI().toURL());
        }
        
        
        /**
         * Adds a new path to the classloader. The class must point to a directory,
         * not a file.
         *
         * @param aURL The path to include when searching the classpath.
         * @throws IOException 
         * @throws InvocationTargetException
         * @throws NoSuchMethodException 
         * @throws IllegalAccessException  
         */
        public static void add(URL aURL)
                        throws IOException, NoSuchMethodException, IllegalAccessException,
                        InvocationTargetException {
                Method lMethod = CLASS_LOADER.getDeclaredMethod("addURL", PARAMETERS);
                lMethod.setAccessible(true);
                lMethod.invoke(getClassLoader(), new Object[]{aURL});
        }
        
        /**
         * 
         * @param aURL
         * @param aClassLoader
         * @throws IOException
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public static void add(URL aURL, ClassLoader aClassLoader)
                        throws IOException, NoSuchMethodException, IllegalAccessException,
                        InvocationTargetException {
                Method lMethod = aClassLoader.getClass().getDeclaredMethod("addURL", PARAMETERS);
                lMethod.setAccessible(true);
                lMethod.invoke(aClassLoader, new Object[]{aURL});
        }
        
        private static URLClassLoader getClassLoader() {
                return (URLClassLoader) ClassLoader.getSystemClassLoader();
        }
/*        
        static{
                try{
                        Method lMethod = CLASS_LOADER.getDeclaredMethod("addURL", PARAMETERS);
                        lMethod.setAccessible(true);
                        lMethod.invoke((URLClassLoader) ClassLoader.getSystemClassLoader(), new Object[]{new URL("file:C:/svn/jWebSocketDev/rte/jWebSocket-1.0/libs")});
                        lMethod.invoke((URLClassLoader) Thread.currentThread().getContextClassLoader(), new Object[]{new URL("file:C:/svn/jWebSocketDev/rte/jWebSocket-1.0/libs")});
                } catch(Exception lEx) {
                        String lMsg = lEx.getMessage();
                        System.out.println(lMsg);
                }
        }
*/        
}