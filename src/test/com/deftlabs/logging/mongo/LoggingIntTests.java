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

// Mongo
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

// JUnit
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
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

    @Test public void testSimpleWarning() throws Exception {
        for (int idx=0; idx < 100; idx++) LOG.log(Level.WARNING, "this is a test");
        Thread.sleep(1000);
        assertEquals(100, getCollection().count());
    }

    @Test public void testWithOneParam() throws Exception {
        for (int idx=0; idx < 10; idx++) LOG.log(Level.INFO, "Hello {0}", "test");
        Thread.sleep(1000);
        assertEquals(10, getCollection().count());

        final BasicDBObject doc = (BasicDBObject)getCollection().findOne();
        assertEquals("Hello test", doc.getString("m"));
    }

    @Test public void testWithParams() throws Exception {
        final String [] params = { "zero", "one", "two" };
        for (int idx=0; idx < 10; idx++) LOG.log(Level.INFO, "Hello {0} {1} {2}", params);
        Thread.sleep(1000);
        assertEquals(10, getCollection().count());

        final BasicDBObject doc = (BasicDBObject)getCollection().findOne();
        assertEquals("Hello zero one two", doc.getString("m"));
    }

    @BeforeClass public static void setupLogger() throws Exception {
        // In the current release of Java, the system does not look in the
        // classpath for the logging.properties
        LogManager.getLogManager().readConfiguration(LogConfigUtils.openClasspathResourceUrl("logging.properties"));
    }

    @Before public void init() throws Exception {
        // Cleanup the test database
        _mongo = new Mongo(new MongoURI("mongodb://127.0.0.1:27017"));
        getCollection().remove(new BasicDBObject());
    }

    @After public void cleanup() { getCollection().remove(new BasicDBObject()); }

    private DBCollection getCollection()
    { return _mongo.getDB("mongo-java-logging").getCollection("log"); }

    private Mongo _mongo;

    private static final Logger LOG = Logger.getLogger(LoggingIntTests.class.getName());
}

