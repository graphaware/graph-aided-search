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

import java.util.Map;

public abstract class CypherEndPoint {

    protected final String neo4jHost;

    protected final String neo4jUser;

    protected final String neo4jPassword;

    public CypherEndPoint(String neo4jHost, String neo4jUser, String neo4jPassword) {
        this.neo4jHost = neo4jHost;
        this.neo4jUser = neo4jUser;
        this.neo4jPassword = neo4jPassword;
    }

    public abstract CypherResult executeCypher(String cypherQuery, Map<String, Object> parameters);

    public abstract CypherResult executeCypher(String cypherQuery);

}
