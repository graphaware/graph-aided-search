
/*
 * Copyright (c) 2015 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.filter.GraphAidedSearchFilter;
import com.graphaware.integration.es.plugin.module.GraphAidedSearchModule;
import static com.graphaware.integration.es.plugin.query.GraphAidedSearch.*;
import java.util.ArrayList;
import java.util.Collection;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.cluster.ClusterModule;
import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

public class GraphAidedSearchPlugin extends Plugin {

    public static final String INDEX_LOGGER_NAME = "index.graph-aided-search";

    @Override
    public String name() {
        return "GraphAidedSearchPlugin";
    }

    @Override
    public String description() {
        return "This is Graphaware Graph Aided Search Plugin for Neo4j.";
    }

    public void onModule(final ActionModule module) {
        module.registerFilter(GraphAidedSearchFilter.class);
    }

    public void onModule(final ClusterModule module) {
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_ENABLED, Validator.BOOLEAN);
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_HOST, Validator.EMPTY);
    }


    @Override
    public Collection<Module> nodeModules() {
        final Collection<Module> modules = new ArrayList<>();
        modules.add(new GraphAidedSearchModule());
        return modules;
    }

}
