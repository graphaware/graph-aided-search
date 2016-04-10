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
package com.graphaware.integration.es;

import com.graphaware.integration.es.wrap.ActionListenerWrapper;
import com.graphaware.integration.es.wrap.CannotWrapException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public class GraphAidedSearchFilter extends AbstractComponent implements ActionFilter {

    private static final String SEARCH_REQUEST_INVOKED = "filter.graphaware.neo4j.Invoked";

    private static final int DEFAULT_FILTER_ORDER = 10;

    protected final ESLogger logger;

    private final int order;

    private ActionListenerWrapper<?> wrapper;

    @Inject
    public GraphAidedSearchFilter(final Settings settings) {
        super(settings);
        logger = Loggers.getLogger(GraphAidedSearchFilter.class.getName(), settings);
        order = settings.getAsInt("indices.graphaware.filter.order", DEFAULT_FILTER_ORDER);
    }

    public void setWrapper(ActionListenerWrapper<?> wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public void apply(final String action, final ActionRequest request,  ActionListener listener, final ActionFilterChain chain) {
        if (SearchAction.INSTANCE.name().equals(action)) {
            if (Boolean.TRUE.equals(request.<Boolean>getHeader(SEARCH_REQUEST_INVOKED))) {
                try {
                    listener = wrapper.wrap((SearchRequest) request, listener);
                } catch (CannotWrapException e) {
                    //that's OK, will use the original unwrapped one and perform no Graph-Aided Search
                }
            } else {
                request.putHeader(SEARCH_REQUEST_INVOKED, Boolean.TRUE);
            }
        }

        chain.proceed(action, request, listener);
    }

    @Override
    public void apply(final String action, final ActionResponse response, final ActionListener listener, final ActionFilterChain chain) {
        chain.proceed(action, response, listener);
    }
}
