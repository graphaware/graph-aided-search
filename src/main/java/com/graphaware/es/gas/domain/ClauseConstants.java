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

package com.graphaware.es.gas.domain;

public final class ClauseConstants {

    public static final String QUERY = "query";
    public static final String SCORE_NAME = "scoreName";
    public static final String IDENTIFIER = "identifier";

    public static final String SIZE = "size";
    public static final String FROM = "from";
    public static final String MAX_RESULT_SIZE = "maxResultSize";
    public static final String OPERATOR = "operator";
    public static final String MULTIPLY = "*";
    public static final String DIVIDE = "/";
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String REPLACE = "replace";

    public static final String TARGET = "target";
    public static final String KEY_PROPERTY = "keyProperty";
    public static final String NEO4J_ENDPOINT = "neo4j.endpoint";
    public static final String LIMIT = "limit";
    public static final String IDS = "ids";

    public static final String EXCLUDE = "exclude";
    public static final String TRUE = "true";
    public static final String QUERY_BINARY = "query_binary";
    public static final String NAME = "name";
    
    private ClauseConstants() {
        
    }
}
