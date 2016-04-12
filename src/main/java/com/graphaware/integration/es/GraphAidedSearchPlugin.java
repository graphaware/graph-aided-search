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

import com.graphaware.integration.es.domain.Constants;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.cluster.ClusterModule;
import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;
import java.util.Collections;

import static com.graphaware.integration.es.domain.Constants.INDEX_GA_ES_NEO4J_ENABLED;
import static com.graphaware.integration.es.domain.Constants.INDEX_GA_ES_NEO4J_HOST;
import static com.graphaware.integration.es.domain.Constants.INDEX_GA_ES_NEO4J_USER;
import static com.graphaware.integration.es.domain.Constants.INDEX_GA_ES_NEO4J_PWD;

public class GraphAidedSearchPlugin extends Plugin {

    @Override
    public String name() {
        return "GraphAidedSearchPlugin";
    }

    @Override
    public String description() {
        return "GraphAware Graph-Aided Search Plugin for Neo4j.";
    }

    public void onModule(final ActionModule module) {
        module.registerFilter(GraphAidedSearchFilter.class);
    }

    public void onModule(final ClusterModule module) {
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_ENABLED, Validator.BOOLEAN);
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_HOST, Validator.EMPTY);
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_USER, Validator.EMPTY);
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_PWD, Validator.EMPTY);
    }

    @Override
    public Collection<Module> nodeModules() {
        return Collections.<Module>singleton(new GraphAidedSearchModule());
    }

}
