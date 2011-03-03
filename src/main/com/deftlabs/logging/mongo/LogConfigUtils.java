/**
 * Copyright 2011, Deft Labs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deftlabs.logging.mongo;

// Java
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

/**
 * The log config utils. Most of this source came from the original OEMware project.
 */
public final class LogConfigUtils {

    /**
     * Open a resource url input stream. Look at the loadResourceUrl method
     * for a description of how the resource is searched for.
     * @param pName The resource name.
     * @return The open input stream.
     * @throws IOException
     */
    public final static InputStream openClasspathResourceUrl(final String pName)
        throws IOException
    { return loadResourceUrlFromClasspath(pName).openStream(); }

    /**
     * Load a resource url. This looks at the current class loader first and if
     * the resource isn't found it looks at the system class loader. If it is
     * not found anywhere, an exception is thrown.
     * @param pName The resource name.
     * @return The resource url.
     */
    public final static URL loadResourceUrlFromClasspath(final String pName) {
        if (pName == null || pName.trim().equals(NADA))
        { throw new IllegalArgumentException("resource name not set"); }

        // Try the thread class loader first.
        ClassLoader classLoader
        = Thread.currentThread().getContextClassLoader();

        URL url = null;
        if (classLoader != null) {
            url = classLoader.getResource(pName);
            if (url != null) return url;
        }

        // We didn't have any luck in the parent. Try the system class loader.
        classLoader = ClassLoader.getSystemClassLoader();

        url = classLoader.getResource(pName);

        // The resource isn't found (anywhere). Throw an exception.
        if (url == null)
        { throw new IllegalStateException("resource not found in classpath - name: " + pName); }

        return url;
    }

    private static final String NADA = "";
}

