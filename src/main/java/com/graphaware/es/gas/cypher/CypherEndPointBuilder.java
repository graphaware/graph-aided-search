/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.es.gas.cypher;

import com.graphaware.es.gas.domain.IndexInfo;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author ale
 */
public class CypherEndPointBuilder {

    private Settings settings;
    private String neo4jHost;
    private IndexInfo indexInfo;
    private final CypherEndPointType type;
    private String neo4jUsername;
    private String neo4jPassword;

    public CypherEndPointBuilder(CypherEndPointType type) {
        this.type = type;
    }

    public CypherEndPointBuilder(String type) {
        this.type = CypherEndPointType.getEnum(type);
    }

    public CypherEndPointBuilder indexInfo(IndexInfo indexInfo) {
        this.indexInfo = indexInfo;
        return this;
    }

    public CypherEndPointBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public CypherEndPointBuilder neo4jHostname(String neo4jHost) {
        this.neo4jHost = neo4jHost;
        return this;
    }

    public CypherEndPointBuilder username(String neo4jUsername) {
        this.neo4jUsername = neo4jHost;
        return this;
    }

    public CypherEndPointBuilder password(String neo4jPassword) {
        this.neo4jPassword = neo4jHost;
        return this;
    }

    public CypherEndPoint build() {
        switch (type) {
            case HTTP:
                return new CypherHttpEndPoint(settings,
                        neo4jHost != null ? neo4jHost : indexInfo != null ? indexInfo.getNeo4jHost() : null,
                        neo4jUsername != null ? neo4jUsername : indexInfo != null ? indexInfo.getNeo4jUsername() : null,
                        neo4jPassword != null ? neo4jPassword : indexInfo != null ? indexInfo.getNeo4jPassword() : null
                );
            case BOLT:
                return new CypherBoltHttpEndPoint(settings,
                        neo4jHost != null ? neo4jHost : indexInfo != null ? indexInfo.getNeo4jBoltHost() : null);
        }
        throw new RuntimeException("Type " + type + " not supported");
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
