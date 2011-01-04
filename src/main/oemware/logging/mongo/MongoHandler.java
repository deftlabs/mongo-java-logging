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

// Mongo
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.DBAddress;
import com.mongodb.ServerAddress;
import com.mongodb.MongoOptions;
import com.mongodb.MongoException;
import com.mongodb.DBCollection;

// Java
import java.util.logging.Level;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.LogManager;
import java.util.logging.Handler;
import java.util.logging.ErrorManager;
import java.net.UnknownHostException;

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
 * oemware.logging.mongo.MongoHandler.level specifies the default level for the Handler (defaults to Level.ALL).
 * oemware.logging.mongo.MongoHandler.filter specifies the name of a Filter class to use (defaults to no Filter).
 * oemware.logging.mongo.MongoHandler.encoding the name of the character set encoding to use (defaults to platform default).
 * oemware.logging.mongo.MongoHandler.mongoUsername The optional username (not optional if secured).
 * oemware.logging.mongo.MongoHandler.mongoPasswod The optional password (not optional if secured).
 * oemware.logging.mongo.MongoHandler.mongoHost The required mongo host/server name or address (default is localhost).
 * oemware.logging.mongo.MongoHandler.mongoPort The required mongo server port (default is 27017).
 * oemware.logging.mongo.MongoHandler.autoConnectRetry true if mongo should try and reconnect (default is true).
 * oemware.logging.mongo.MongoHandler.connectionsPerHost true max number of connections per host (default is 10).
 * oemware.logging.mongo.MongoHandler.connectTimeout the connection timeout in ms (default is 10 seconds || 10,000 ms).
 * oemware.logging.mongo.MongoHandler.socketTimeout the socket timeout in ms (default is 10 seconds || 10,000 ms).
 * oemware.logging.mongo.MongoHandler.maxWaitTime the max wait time (default is 5 seconds || 5,000 ms).
 * oemware.logging.mongo.MongoHandler.threadsAllowedToBlockForConnectionMultiplier see mongo docs (default is 5).
 * oemware.logging.mongo.MongoHandler.databaseName The name of the Mongo database (defaults to mongo-java-logging).
 * oemware.logging.mongo.MongoHandler.collectionName The name of the Mongo collection (defaults to log).
 * </pre>
 *
 */
public class MongoHandler extends Handler {

    public MongoHandler() { super(); configureHandler(); }

    public void publish(final LogRecord pRecord) {
        if (!isLoggable(pRecord)) return;

        try {
            final DBCollection col = getCollection();

            System.out.println("--- this is the message");

        } catch (final UnknownHostException uhe) {
            uhe.printStackTrace();
            getErrorManager().error(uhe.getMessage(), uhe, ErrorManager.OPEN_FAILURE);

        } catch (final MongoException me) {
            me.printStackTrace();
            getErrorManager().error(me.getMessage(), me, ErrorManager.WRITE_FAILURE);
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

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

    private void configureHandler() {
        final LogManager manager = LogManager.getLogManager();

        final String clazz = getClass().getName();

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
        setLevel(getLevelProp(clazz + ".level", Level.ALL));

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

    public void close() { }

    public void flush() { }

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
    private Integer _maxWaitTime;
    private Integer _threadsAllowedToBlockForConnectionMultiplier;

    private static final Object sMutex = new Object();
}

