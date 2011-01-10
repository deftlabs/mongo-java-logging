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

/**
 * The log message format.
 */
public enum LogMsg {


    LEVEL("l"),
    MSG("m"),
    LOGGER("lo"),
    MSG_SEQ("ms"),
    THREAD("t"),
    RES_BUNDLE("rb"),
    TIMESTAMP("ts"),
    SRC_CLASS("sc"),
    SRC_METHOD("sm"),
    THROWN("th"),
    NODE_NAME("nn"),
    APP_PID("pid");

    public final String field;
    private LogMsg(final String pField) { field = pField; }
    public String getField() { return field; }
}

