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
package com.graphaware.integration.es.query;

import java.util.Map;

public class RetrySearchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private QueryRewriter rewriter;

    public RetrySearchException(QueryRewriter rewriter) {
        super();
        this.rewriter = rewriter;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }

    public Map<String, Object> rewrite(Map<String, Object> source) {
        return rewriter.rewrite(source);
    }

    public interface QueryRewriter {

        Map<String, Object> rewrite(Map<String, Object> source);
    }
}
