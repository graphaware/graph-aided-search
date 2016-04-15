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
package com.graphaware.integration.es.stubs;

import com.graphaware.integration.es.annotation.SearchBooster;
import com.graphaware.integration.es.booster.SearchResultExternalBooster;
import com.graphaware.integration.es.domain.ExternalResult;
import com.graphaware.integration.es.domain.IndexInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.settings.Settings;

@SearchBooster(name = "SearchResultTestBooster")
public class SearchResultTestBooster extends SearchResultExternalBooster {


    public SearchResultTestBooster(Settings settings, IndexInfo indexSettings) {
        super(settings, indexSettings);
    }

    @Override
    protected Map<String, ExternalResult> externalDoReorder(Set<String> keySet) {
        Map<String, ExternalResult> result = new HashMap<>();
        for (String id : keySet) {
            result.put(id, new ExternalResult(id, Integer.parseInt(id) * 1000));
        }
        return result;
    }
}
