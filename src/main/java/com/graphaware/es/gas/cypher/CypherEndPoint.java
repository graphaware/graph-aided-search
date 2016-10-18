/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.es.gas.cypher;

import com.graphaware.es.gas.domain.IndexInfo;
import java.util.HashMap;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public abstract class CypherEndPoint {

    protected final ESLogger logger;
    private final String neo4jHost;
    private final String neo4jPassword;
    private final String neo4jUsername;


    public CypherEndPoint(Settings settings, String neo4jHost) {
        this(settings, neo4jHost, null, null);
    }

    public CypherEndPoint(Settings settings, String neo4jHost, String neo4jUsername, String neo4jPassword) {
        this.neo4jHost = neo4jHost;
        this.neo4jUsername = neo4jUsername;
        this.neo4jPassword = neo4jPassword;
        if (settings != null) {
            this.logger = Loggers.getLogger(IndexInfo.INDEX_LOGGER_NAME, settings);
        } else {
            this.logger = Loggers.getLogger(IndexInfo.INDEX_LOGGER_NAME, Settings.EMPTY);
        }
    }

    public String getNeo4jPassword() {
        return neo4jPassword;
    }

    public String getNeo4jUsername() {
        return neo4jUsername;
    }

    public String getNeo4jHost() {
        return neo4jHost;
    }
    
    public abstract CypherResult executeCypher(String query, HashMap<String, Object> parameters);

    public abstract CypherResult executeCypher(HashMap<String, String> headers, String query, HashMap<String, Object> parameters);

}
