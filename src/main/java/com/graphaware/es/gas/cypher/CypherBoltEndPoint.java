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
import org.elasticsearch.common.settings.Settings;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.util.Pair;

public class CypherBoltEndPoint extends CypherEndPoint {

    public CypherBoltEndPoint(Settings settings, String neo4jUrl, String neo4jUsername, String neo4jPassword) {
        super(settings, neo4jUrl, neo4jUsername, neo4jPassword);
    }

    public CypherResult executeCypher(HashMap<String, String> headers, String query, HashMap<String, Object> parameters) {
        try (Driver driver = GraphDatabase.driver(getUrl()); Session session = driver.session()) {
            StatementResult response = session.run(query, parameters);
            return buildResult(response);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private CypherResult buildResult(StatementResult response) {
        CypherResult result = new CypherResult();
        while (response.hasNext()) {
            Record record = response.next();
            for (Pair<String, Value> fieldInRecord : record.fields()) {
                ResultRow resultRow = new ResultRow();
                resultRow.add(fieldInRecord.key(), fieldInRecord.value());
                result.addRow(resultRow);
            }
        }
        return result;
    }
}
