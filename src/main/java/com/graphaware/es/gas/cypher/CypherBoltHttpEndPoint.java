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

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.settings.Settings;
import org.neo4j.driver.v1.Config;
import static org.neo4j.driver.v1.Config.EncryptionLevel.NONE;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.util.Pair;

public class CypherBoltHttpEndPoint extends CypherEndPoint {

    private boolean encryption = true;

    CypherBoltHttpEndPoint(Settings settings, String neo4jUrl, boolean encryption) {
        super(settings, neo4jUrl);
        this.encryption = encryption;
    }

    public CypherResult executeCypher(String cypherQuery) {
        return executeCypher(cypherQuery, new HashMap<String, Object>());
    }

    public CypherResult executeCypher(String cypherQuery, Map<String, Object> parameters) {
        try (Driver driver = encryption ? GraphDatabase.driver(neo4jHost) : GraphDatabase.driver(neo4jHost, Config.build().withEncryptionLevel(NONE).toConfig());
                Session session = driver.session()) {
            StatementResult response = session.run(cypherQuery, parameters);
            return buildResult(response);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private CypherResult buildResult(StatementResult response) {
        CypherResult result = new CypherResult();
        while (response.hasNext()) {
            Record record = response.next();
            ResultRow resultRow = new ResultRow();
            for (Pair<String, Value> fieldInRecord : record.fields()) {
                resultRow.add(fieldInRecord.key(), fieldInRecord.value());
            }
            result.addRow(resultRow);
        }
        return result;
    }
}
