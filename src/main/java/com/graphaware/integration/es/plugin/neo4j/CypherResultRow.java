package com.graphaware.integration.es.plugin.neo4j;

import java.util.HashMap;
import java.util.Map;

public class CypherResultRow {

    private Map<String, Object> values = new HashMap<>();

    public CypherResultRow(){}

    public CypherResultRow(String key, Object value) {
        values.put(key, value);
    }

    public Map<String, Object> values() {
        return values;
    }
}
