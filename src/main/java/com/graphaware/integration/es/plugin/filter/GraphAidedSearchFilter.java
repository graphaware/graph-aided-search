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

package com.graphaware.integration.es.plugin.filter;

import com.graphaware.integration.es.plugin.query.GraphAidedSearch;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public class GraphAidedSearchFilter extends AbstractComponent
        implements ActionFilter
{
  private static final String SEARCH_REQUEST_INVOKED = "filter.graphaware.neo4j.Invoked";

  protected final ESLogger logger;

  private int order;

  private final GraphAidedSearch graphAidedSearch;

  @Inject
  public GraphAidedSearchFilter(final Settings settings,
                                  final GraphAidedSearch graphAidedSearch)
  {
    super(settings);
    this.graphAidedSearch = graphAidedSearch;
    logger = Loggers.getLogger(GraphAidedSearchFilter.class.getName(), settings);
  }

  @Override
  public int order()
  {
    return order;
  }

  @Override
  public void apply(final String action,
                    @SuppressWarnings("rawtypes") final ActionRequest request,
                    @SuppressWarnings("rawtypes") final ActionListener listener,
                    final ActionFilterChain chain)
  {
    if (!SearchAction.INSTANCE.name().equals(action))
    {
      chain.proceed(action, request, listener);
      return;
    }

    final SearchRequest searchRequest = (SearchRequest) request;
    final Boolean invoked = searchRequest.getHeader(SEARCH_REQUEST_INVOKED);
    if (invoked != null && invoked)
    {
      @SuppressWarnings("unchecked")
      final ActionListener<SearchResponse> wrappedListener = graphAidedSearch
              .wrapActionListener(action, searchRequest, listener);
      chain.proceed(action, request,
              wrappedListener == null ? listener : wrappedListener);
    }
    else
    {
      searchRequest.putHeader(SEARCH_REQUEST_INVOKED, Boolean.TRUE);
      chain.proceed(action, request, listener);
    }

  }

  @Override
  public void apply(final String action, final ActionResponse response,
                    @SuppressWarnings("rawtypes") final ActionListener listener, final ActionFilterChain chain)
  {
    chain.proceed(action, response, listener);
  }

}
