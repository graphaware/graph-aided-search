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

import org.elasticsearch.common.settings.Settings;

public class CypherEndPointBuilder {

    private Settings settings;
    private String neo4jHost;
    private String neo4jBoltHost;
    private final CypherEndPointType protocol;
    private String neo4jUsername;
    private String neo4jPassword;
    private boolean encryption = true;

    public CypherEndPointBuilder(CypherEndPointType protocol) {
        this.protocol = protocol;
    }

    public CypherEndPointBuilder(String protocol) {
        this.protocol = CypherEndPointType.getEnum(protocol);
    }


    public CypherEndPointBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public CypherEndPointBuilder neo4jHostname(String neo4jHost) {
        this.neo4jHost = neo4jHost;
        return this;
    }
    
    public CypherEndPointBuilder neo4jBoltHostname(String neo4jBoltHost) {
        this.neo4jBoltHost = neo4jBoltHost;
        return this;
    }

    public CypherEndPointBuilder username(String neo4jUsername) {
        this.neo4jUsername = neo4jUsername;
        return this;
    }

    public CypherEndPointBuilder password(String neo4jPassword) {
        this.neo4jPassword = neo4jPassword;
        return this;
    }

    public CypherEndPointBuilder encryption(boolean encryption) {
        this.encryption = encryption;
        return this;
    }    

    public CypherEndPoint build() {
        checkNeo4jHost();
        switch (protocol) {
            case HTTP:
                return new CypherHttpEndPoint(settings,
                        neo4jHost,
                        neo4jUsername,
                        neo4jPassword
                );
            case BOLT:
                return new CypherBoltHttpEndPoint(settings, 
                        neo4jBoltHost,
                        neo4jUsername,
                        neo4jPassword,
                        encryption);
        }
        throw new RuntimeException("Type " + protocol + " not supported");
    }

    private void checkNeo4jHost() {
        if (neo4jHost == null) 
            throw new RuntimeException("No neo4j hosts specified for http connection in the index settings");
    }

    public enum CypherEndPointType {
        HTTP("http"),
        BOLT("bolt");

        String name;

        CypherEndPointType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static CypherEndPointType getEnum(String value) {
            if (HTTP.getName().equalsIgnoreCase(value)) {
                return HTTP;
            } else if (BOLT.getName().equalsIgnoreCase(value)) {
                return BOLT;
            }
            throw new RuntimeException("Type " + value + " not supported");
        }
    }
}
