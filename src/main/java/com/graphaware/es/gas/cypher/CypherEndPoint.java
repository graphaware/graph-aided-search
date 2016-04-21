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
import java.util.Map;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public abstract class CypherEndPoint {

    protected final ESLogger logger;
    protected final String neo4jHost;

    public CypherEndPoint(Settings settings, String neo4jHost) {
        this.neo4jHost = neo4jHost;
        this.logger = Loggers.getLogger(IndexInfo.INDEX_LOGGER_NAME, settings);
    }

    public abstract CypherResult executeCypher(String cypherQuery, Map<String, Object> parameters);

    public abstract CypherResult executeCypher(String cypherQuery);

}
