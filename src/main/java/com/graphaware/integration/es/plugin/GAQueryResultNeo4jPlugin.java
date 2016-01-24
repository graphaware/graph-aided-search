
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

import com.graphaware.integration.es.plugin.filter.GAQueryResultNeo4jFilter;
import com.graphaware.integration.es.plugin.module.GAQueryResultNeo4jModule;
import static com.graphaware.integration.es.plugin.query.GAQueryResultNeo4j.INDEX_GA_ES_NEO4J_HOST;
import static com.graphaware.integration.es.plugin.query.GAQueryResultNeo4j.INDEX_GA_ES_NEO4J_REORDER_TYPE;
import java.util.ArrayList;
import java.util.Collection;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.cluster.ClusterModule;
import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;

//import org.codelibs.elasticsearch.extension.module.ExtensionModule;


public class GAQueryResultNeo4jPlugin extends Plugin
{
  public static final String REST_LOGGER_NAME = "rest.action.admin.ga-es-neo4j";

  public static final String INDEX_LOGGER_NAME = "index.es-neo4j";

  @Override
  public String name()
  {
    return "GAQueryResultNeo4jPlugin";
  }

  @Override
  public String description()
  {
    return "This is Graph Aware plugin for Neo4j.";
  }

  public void onModule(final ActionModule module)
  {
    module.registerFilter(GAQueryResultNeo4jFilter.class);
  }

//  public void onModule(final ExtensionModule module)
//  {
//    //module.registerEngineFilter(RefreshEngineFilter.class);
//  }

  public void onModule(final RestModule module)
  {
//    module.addRestAction(RestClearQRCacheAction.class);
//    module.addRestAction(RestStatsQRCacheAction.class);
  }
  public void onModule(final ClusterModule module) {
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_REORDER_TYPE, Validator.EMPTY);
        module.registerIndexDynamicSetting(INDEX_GA_ES_NEO4J_HOST, Validator.EMPTY);
    }
  
//   public void onModule(final ClusterModule module) {
//        module.registerIndexDynamicSetting(DynamicRanker.INDEX_DYNARANK_SCRIPT, Validator.EMPTY);
//        module.registerIndexDynamicSetting(DynamicRanker.INDEX_DYNARANK_SCRIPT_LANG, Validator.EMPTY);
//        module.registerIndexDynamicSetting(DynamicRanker.INDEX_DYNARANK_SCRIPT_TYPE, Validator.EMPTY);
//        module.registerIndexDynamicSetting(DynamicRanker.INDEX_DYNARANK_SCRIPT_PARAMS+"*", Validator.EMPTY);
//        module.registerIndexDynamicSetting(DynamicRanker.INDEX_DYNARANK_REORDER_SIZE, Validator.POSITIVE_INTEGER);
//        module.registerClusterDynamicSetting(DynamicRanker.INDICES_DYNARANK_REORDER_SIZE, Validator.POSITIVE_INTEGER);
//        module.registerClusterDynamicSetting(DynamicRanker.INDICES_DYNARANK_CACHE_EXPIRE, Validator.TIME);
//        module.registerClusterDynamicSetting(DynamicRanker.INDICES_DYNARANK_CACHE_CLEAN_INTERVAL, Validator.TIME);
//    }

//  public void onModule(final IndexDynamicSettingsModule module)
//  {
//    module.addDynamicSettings("index.ga-es-neo4j.enable.*");
//  }

  @Override
  public Collection<Module> nodeModules() {
  
    final Collection<Module> modules =new ArrayList<>();
    modules.add(new GAQueryResultNeo4jModule());
    return modules;
  }

}
