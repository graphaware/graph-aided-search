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
import org.elasticsearch.common.settings.Settings;

public class CypherSettingsReader {
    private final String neo4jHost;
    private final String neo4jBoltHost;
    private final String neo4jUsername;
    private final String neo4jPassword;
    private final int maxResultWindow;
    private final Settings settings;
    private final boolean secureBolt;
    
    public CypherSettingsReader(Settings settings, IndexInfo indexSettings) {
        this.settings = settings;
        this.neo4jHost = indexSettings.getNeo4jHost();
        this.neo4jBoltHost = indexSettings.getNeo4jBoltHost();
        this.neo4jUsername = indexSettings.getNeo4jUsername();
        this.neo4jPassword = indexSettings.getNeo4jPassword();
        this.maxResultWindow = indexSettings.getMaxResultWindow();
        this.secureBolt = indexSettings.isSecureBolt();
    }
    
    protected CypherEndPoint createCypherEndPoint(String protocol, Settings settings) {
        return new CypherEndPointBuilder(protocol)
                .settings(settings)
                .neo4jHostname(getNeo4jHost())
                .neo4jBoltHostname(getNeo4jBoltHost())
                .username(getNeo4jUsername())
                .password(getNeo4jPassword())
                .encryption(isSecureBolt())
                .build();
    }

    public String getNeo4jHost() {
        return neo4jHost;
    }

    public String getNeo4jUsername() {
        return neo4jUsername;
    }

    public String getNeo4jPassword() {
        return neo4jPassword;
    }

    public int getMaxResultWindow() {
        return maxResultWindow;
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean isSecureBolt() {
        return secureBolt;
    }

    public String getNeo4jBoltHost() {
        return neo4jBoltHost;
    }
    
}
