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
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.DBAddress;
import com.mongodb.ServerAddress;
import com.mongodb.MongoOptions;
import com.mongodb.MongoException;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;

// Java
import java.util.logging.Level;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.LogManager;
import java.util.logging.Handler;
import java.util.logging.ErrorManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.LinkedList;
import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * The Mongo logger for
 * <a href="http://en.wikipedia.org/wiki/Java_logging_framework">Java Logging</a>.
 *
 * <p>Most of this documentation came from the Java API docs.</p>
 *
 * <p>
 * <b>Configuration:</b> By default each MongoHandler is initialized using the
 * following LogManager configuration properties. If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * </p>
 * <pre>
 * com.deftlabs.logging.mongo.MongoHandler.level specifies the default level for the Handler (defaults to Level.ALL).
 * com.deftlabs.logging.mongo.MongoHandler.filter specifies the name of a Filter class to use (defaults to no Filter).
 * com.deftlabs.logging.mongo.MongoHandler.encoding the name of the character set encoding to use (else platform default).
 * com.deftlabs.logging.mongo.MongoHandler.mongoUsername The optional username (not optional if secured).
 * com.deftlabs.logging.mongo.MongoHandler.mongoPasswod The optional password (not optional if secured).
 * com.deftlabs.logging.mongo.MongoHandler.mongoHost The required mongo host/server name or address (default is localhost).
 * com.deftlabs.logging.mongo.MongoHandler.mongoPort The required mongo server port (default is 27017).
 * com.deftlabs.logging.mongo.MongoHandler.autoConnectRetry true if mongo should try and reconnect (default is true).
 * com.deftlabs.logging.mongo.MongoHandler.connectionsPerHost true max number of connections per host (default is 10).
 * com.deftlabs.logging.mongo.MongoHandler.connectTimeout the connection timeout in ms (default is 10 seconds || 10,000 ms).
 * com.deftlabs.logging.mongo.MongoHandler.socketTimeout the socket timeout in ms (default is 10 seconds || 10,000 ms).
 * com.deftlabs.logging.mongo.MongoHandler.maxWaitTime the max wait time (default is 5 seconds || 5,000 ms).
 * com.deftlabs.logging.mongo.MongoHandler.threadsAllowedToBlockForConnectionMultiplier see mongo docs (default is 5).
 * com.deftlabs.logging.mongo.MongoHandler.databaseName The name of the Mongo database (defaults to mongo-java-logging).
 * com.deftlabs.logging.mongo.MongoHandler.collectionName The name of the Mongo collection (defaults to log).
 * com.deftlabs.logging.mongo.MongoHandler.nodeName The optional name of the node. Otherwise, uses ip address(s).
 * com.deftlabs.logging.mongo.MongoHandler.maxQueueSize Set the max queue size. If the queue is full, new messages
 * are dropped. This was done to avoid memory issues and application blocking (defualt is 500).
 * com.deftlabs.logging.mongo.MongoHandler.closeSleepTime The optional (default is 500 ms) amount to sleep before stopping. This
 * allows log messsages in the queue/buffer a chance to be persisted to Mongo. This value is in milliseconds.
 * com.deftlabs.logging.mongo.MongoHandler.writerThreadCount The optional config param to increase writer threads (default is 1).
 *
 *
 * TODO: Configure rolling, time zone, etc?
 * </pre>
 *
 */
public class MongoHandler extends Handler {

    public MongoHandler() throws Exception { super(); configure(); }

    public void publish(final LogRecord pRcd) {
        if (!isLoggable(pRcd)) return;

        try {
            if (_queue == null) configure();

            final BasicDBObject msg = new BasicDBObject();
            msg.put(LogMsg.LEVEL.field, pRcd.getLevel().getName());
            msg.put(LogMsg.MSG.field, pRcd.getMessage());
            msg.put(LogMsg.LOGGER.field, pRcd.getLoggerName());
            msg.put(LogMsg.MSG_SEQ.field, pRcd.getSequenceNumber());
            msg.put(LogMsg.THREAD.field, pRcd.getThreadID());
            msg.put(LogMsg.RES_BUNDLE.field, pRcd.getResourceBundleName());
            msg.put(LogMsg.TIMESTAMP.field, pRcd.getMillis());
            msg.put(LogMsg.SRC_METHOD.field, pRcd.getSourceMethodName());
            msg.put(LogMsg.SRC_CLASS.field, pRcd.getSourceClassName());
            msg.put(LogMsg.THROWN.field, throwableToString(pRcd.getThrown()));
            msg.put(LogMsg.APP_PID.field, _pid);
            msg.put(LogMsg.NODE_NAME.field, _nodeName);

            if (!_queue.offer(msg))
            { getErrorManager().error("Queue is full", null, ErrorManager.WRITE_FAILURE); }

        } catch (final Exception e) {
            getErrorManager().error(e.getMessage(), e, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * Serialize the throwable to a string. If T is null, null is returned.
     */
    private static String throwableToString(final Throwable t) {
        if (t == null) return null;
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        t.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * Send the message to mongo (i.e., insert in the collection).
     */
    private void sendToMongo(final BasicDBObject pMsg) {
        try { getCollection().insert(pMsg);
        } catch (final Exception me) {
            getErrorManager().error(me.getMessage(), me, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * Returns the configured collection.
     */
    private DBCollection getCollection() throws UnknownHostException {
        if (_collection != null) return _collection;

        synchronized(sMutex) {
            if (_collection != null) return _collection;

            final MongoOptions mongoOptions = new MongoOptions();

            mongoOptions.autoConnectRetry = _autoConnectRetry;
            mongoOptions.connectionsPerHost = _connectionsPerHost;
            mongoOptions.connectTimeout = _connectTimeout;
            mongoOptions.socketTimeout = _socketTimeout;
            mongoOptions.maxWaitTime = _maxWaitTime;
            mongoOptions.threadsAllowedToBlockForConnectionMultiplier = _threadsAllowedToBlockForConnectionMultiplier;

            _mongo
            = new Mongo(new DBAddress(_mongoHost, _mongoPort, _databaseName), mongoOptions);

            final DB db = _mongo.getDB(_databaseName);

            if (_mongoUsername != null && !db.authenticate(_mongoUsername, _mongoPassword.toCharArray()))
            { throw new MongoException("Unable to authenticate user: " + _mongoUsername); }

            _collection = _mongo.getDB(_databaseName).getCollection(_collectionName);

            return _collection;
        }
    }

    public void flush() { }

    /**
     * Stop the thread.
     */
    public void close() {

        if (_closeSleepTime != null && _closeSleepTime > 0) {
            try { Thread.sleep(_closeSleepTime);
            } catch (final InterruptedException ie) { }
        }

        if (_msgWriterThreads == null) return;

        try { for (MsgWriterThread t : _msgWriterThreads) t.interrupt();
        } catch (final Throwable t) { }
    }

    private void configure() throws Exception {

        final LogManager manager = LogManager.getLogManager();

        final String clazz = getClass().getName();

        final String pid = ManagementFactory.getRuntimeMXBean().getName();
        _pid = pid.substring(0, pid.indexOf("@"));

        _nodeName = getStrProp(clazz + ".nodeName", null);

        if (_nodeName == null)
        { _nodeName  = InetAddress.getLocalHost().getHostName(); }

        _mongoHost = getStrProp(clazz + ".mongoHost", "127.0.0.1");
        _mongoPort = getIntProp(clazz + ".mongoPort", 27017);
        _mongoUsername = getStrProp(clazz + ".mongoUsername", null);
        _mongoPassword = getStrProp(clazz + ".mongoPassword", null);
        _databaseName = getStrProp(clazz + ".databaseName", "mongo-java-logging");
        _collectionName = getStrProp(clazz + ".collectionName", "log");
        _autoConnectRetry = getBoolProp(clazz + ".autoConnectRetry", true);
        _connectionsPerHost = getIntProp(clazz + ".connectionsPerHost", 10);
        _connectTimeout = getIntProp(clazz + ".connectTimeout", 10000);
        _socketTimeout = getIntProp(clazz + ".socketTimeout", 10000);
        _maxWaitTime = getIntProp(clazz + ".maxWaitTime", 5000);
        _threadsAllowedToBlockForConnectionMultiplier = getIntProp(clazz + ".threadsAllowedToBlockForConnectionMultiplier", 5);

        _closeSleepTime = getIntProp(clazz + ".closeSleepTime", 500);

        final int writerThreadCount = getIntProp(clazz + ".writerThreadCount", 1);

        setLevel(getLevelProp(clazz + ".level", Level.ALL));

        final int maxQueueSize = getIntProp(clazz + ".maxQueueSize", 500);

        if (_queue == null)
        { _queue = new LinkedBlockingQueue<BasicDBObject>(maxQueueSize); }

        if (_msgWriterThreads == null) {
            _msgWriterThreads = new MsgWriterThread[writerThreadCount];
            for (int idx=0; idx < writerThreadCount; idx++) {
                final MsgWriterThread msgWriterThread = new MsgWriterThread();
                msgWriterThread.start();
                msgWriterThread.setName("mongo-java-logging-" + idx);
                _msgWriterThreads[idx] = msgWriterThread;
            }
        }

        setFilter(getFilterProp(clazz + ".filter", null));
        try { setEncoding(getStrProp(clazz + ".encoding", null));
        } catch (final Exception e) { try { setEncoding(null); } catch (final Exception e1) { } }
    }

    private Filter getFilterProp(final String pName, final Filter pDefault) {
        final String v =  LogManager.getLogManager().getProperty(pName);
        try { return (v != null) ? (Filter)ClassLoader.getSystemClassLoader().loadClass(v).newInstance() : pDefault;
        } catch (final Exception e) { }
        return pDefault;
    }

    private Level getLevelProp(final String pName, final Level pDefault) {
        final String v =  LogManager.getLogManager().getProperty(pName);
        if (v == null)  return pDefault;
        try { return Level.parse(v.trim());
        } catch (final Exception e) { return pDefault; }
    }

    private String getStrProp(final String pName, final String pDefault) {
        String v =  LogManager.getLogManager().getProperty(pName);
        return (v == null) ? pDefault : v.trim();
    }

    private boolean getBoolProp(final String pName, final boolean pDefault) {
        String v =  LogManager.getLogManager().getProperty(pName);
        if (v == null) return pDefault;
        v = v.toLowerCase();
        if (v.equals("true") || v.equals("1")) return true;
        else if (v.equals("false") || v.equals("0")) return false;
        return pDefault;
    }

    private int getIntProp(final String pName, final int pDefault) {
        final String v =  LogManager.getLogManager().getProperty(pName);
        if (v == null) return pDefault;
        try { return Integer.parseInt(v.trim());
        } catch (final Exception e) { return pDefault; }
    }

    private Mongo _mongo;
    private DBCollection _collection;

    // Config
    private String _mongoUsername;
    private String _mongoPassword;
    private String _mongoHost;
    private Integer _mongoPort;
    private String _databaseName;
    private String _collectionName;
    private Boolean _autoConnectRetry;
    private Integer _connectionsPerHost;
    private Integer _connectTimeout;
    private Integer _socketTimeout;
    private Integer _closeSleepTime;
    private Integer _maxWaitTime;
    private Integer _threadsAllowedToBlockForConnectionMultiplier;
    private String _pid;
    private String _nodeName;

    private static final Object sMutex = new Object();

    private LinkedBlockingQueue<BasicDBObject> _queue;

    private MsgWriterThread []  _msgWriterThreads;

    /**
     * Read the messages from the queue and write to mongo.
     */
    private class MsgWriterThread extends Thread {

        public void run() {
            while (true) {
                try { sendToMongo(_queue.take());
                } catch (final InterruptedException ie) { break;
                } catch (final Throwable t) {
                    if (t instanceof Exception) {
                        getErrorManager().error(t.getMessage(), (Exception)t, ErrorManager.WRITE_FAILURE);
                    } else {
                        getErrorManager().error(t.getMessage(), null, ErrorManager.WRITE_FAILURE);
                    }
                }
            }
        }
    }
}

