
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
import com.graphaware.integration.es.wrap.GraphAidedSearchActionListenerWrapper;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;

public class GraphAidedSearch extends AbstractLifecycleComponent<GraphAidedSearch> {

    private final ActionListenerWrapper<?> wrapper;
    private final ActionFilters filters;

    @Inject
    public GraphAidedSearch(final Settings settings, final Client client, final ClusterService clusterService, final ScriptService scriptService, final ThreadPool threadPool, final ActionFilters filters) {
        super(settings);

        this.filters = filters;
        this.wrapper = new GraphAidedSearchActionListenerWrapper(settings, clusterService, client);

        initializeFilters();
    }

    private void initializeFilters() {
        for (final ActionFilter filter : filters.filters()) {
            if (filter instanceof GraphAidedSearchFilter) {
                ((GraphAidedSearchFilter) filter).setWrapper(wrapper);
                if (logger.isDebugEnabled()) {
                    logger.debug("Set GraphAidedSearch to " + filter);
                }
            }
        }
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }
}
