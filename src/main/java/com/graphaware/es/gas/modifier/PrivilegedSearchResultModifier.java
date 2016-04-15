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

package com.graphaware.es.gas.modifier;

import org.elasticsearch.search.internal.InternalSearchHits;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

public class PrivilegedSearchResultModifier implements SearchResultModifier {

    private final SearchResultModifier delegate;

    public PrivilegedSearchResultModifier(SearchResultModifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public InternalSearchHits modify(final InternalSearchHits hits) {
        return AccessController.doPrivileged(new PrivilegedAction<InternalSearchHits>() {
            @Override
            public InternalSearchHits run() {
                return delegate.modify(hits);
            }
        });
    }

    @Override
    public void parseRequest(Map<String, Object> sourceAsMap) {
        delegate.parseRequest(sourceAsMap);
    }
}
