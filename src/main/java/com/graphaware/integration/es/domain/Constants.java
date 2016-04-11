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

package com.graphaware.integration.es.domain;

public final class Constants {

    public static final String INDEX_LOGGER_NAME = "index.graph-aided-search";
    public static final String INDEX_GA_ES_NEO4J_ENABLED = "index.gas.enable";
    public static final String INDEX_MAX_RESULT_WINDOW = "max_result_window";
    public static final String INDEX_GA_ES_NEO4J_HOST = "index.gas.neo4j.hostname";

    public static final String GAS_REQUEST = "_gas";
    public static final String GAS_BOOSTER_CLAUSE = "gas-booster";
    public static final String GAS_FILTER_CLAUSE = "gas-filter";

    public static final String QUERY = "query";
    public static final String SCORE_NAME = "scoreName";
    public static final String IDENTIFIER = "identifier";
    public static final String CYPHER_ENDPOINT = "/db/data/transaction/commit";
    public static final String RESULTS = "results";
    public static final String DATA = "data";
    public static final String COLUMNS = "columns";
    public static final String ERRORS = "errors";
    public static final String ROW = "row";

    public static final String SIZE = "size";
    public static final String FROM = "from";
    public static final String MAX_RESULT_SIZE = "maxResultSize";
    public static final String OPERATOR = "operator";
    public static final String MULTIPLY = "*";
    public static final String DIVIDE = "/";
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String REPLACE = "replace";
    public static final String DEFAULT_SCORE_OPERATOR = MULTIPLY;

    public static final String DEFAULT_KEY_PROPERTY = "uuid";
    public static final String RECO_TARGET = "recoTarget";
    public static final String KEY_PROPERTY = "keyProperty";
    public static final String NEO4J_ENDPOINT = "neo4j.endpoint";
    public static final String DEFAULT_REST_ENDPOINT = "/graphaware/recommendation/filter";
    public static final String LIMIT = "limit";
    public static final String IDS = "ids";

    public static final String DEFAULT_SCORE_RESULT_NAME = "score";
    public static final String DEFAULT_ID_RESULT_NAME = "id";

    public static final String CLAUSE_FILTER_EXCLUDE = "exclude";
    public static final String CLAUSE_TYPE_NAME = "name";
    public static final String TRUE = "true";
    public static final String QUERY_BINARY = "query_binary";
    
    public static final int DEFAULT_MAX_RESULT_WINDOW = 10000;

    private Constants() {
    }
}
