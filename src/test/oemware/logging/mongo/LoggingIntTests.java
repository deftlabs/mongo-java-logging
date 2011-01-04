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

package oemware.logging.mongo;

// JUnit
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

// Java
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

/**
 * Test logging. This integration test requires that mongo is running.
 */
public final class LoggingIntTests {

    private static final Logger LOG = Logger.getLogger(LoggingIntTests.class.getName());

    @BeforeClass 
    public static void setupTests() throws Exception {
        // In the current release of Java, the system does not look in the 
        // classpath for the logging.properties.
        LogManager.getLogManager().readConfiguration(LogConfigUtils.openClasspathResourceUrl("logging.properties"));

    }

    @Test
    public void testFinestMsgOnly() throws Exception {
        LOG.log(Level.WARNING, "this is a test");      
    }

    private Logger getLogger() {
       return Logger.getLogger(LoggingIntTests.class.getName());
    }
}

