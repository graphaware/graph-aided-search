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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.integration.es.reco.demo.engine.web;

import com.graphaware.reco.generic.config.MapBasedConfig;
import com.graphaware.reco.generic.context.Context;
import com.graphaware.reco.neo4j.engine.CypherEngine;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.Node;

/**
 *
 * @author ale
 */
public class CypherParametersEngine extends CypherEngine
{
  public CypherParametersEngine(String name, String query)
  {
    super(name, query);
  }
  
  protected Map<String, Object> buildParams(Node input, Context<Node, Node> context) {
        Map<String, Object> params = new HashMap<>();
        params.put(idParamName(), input.getId());
        params.put(idsParamName(), input.getId());  
        if (context.config() instanceof MapBasedConfig)
          params.put(limitParamName(), ((MapBasedConfig)context.config()).get("ids"));
        return params;
    }
  private String idsParamName()
  {
    return "ids";
  }
}
