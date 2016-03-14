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
package com.graphaware.integration.es.plugin.query;

/**
 *
 * @author alessandro@graphaware.com
 */
public class GASIndexInfo {

    protected final static GASIndexInfo NO_SCRIPT_INFO = new GASIndexInfo();
    private String neo4jHost;
    private boolean enabled;
    private int maxResultWindow;

    GASIndexInfo() {
        this.enabled = false;
    }

    GASIndexInfo(final String hostname, boolean enabled, int maxResultWindow) {
        this.neo4jHost = hostname;
        this.enabled = enabled;
        this.maxResultWindow = maxResultWindow;
    }

    @Override
    public String toString() {
        return "ScriptInfo ["
                + " neo4jHost=" + neo4jHost + ", enabled=" + enabled + ", maxResultWindow=" + maxResultWindow
                + "]";
    }

    public String getNeo4jHost() {
        return neo4jHost;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxResultWindow() {
        return maxResultWindow;
    }

    public void setMaxResultWindow(int maxResultWindow) {
        this.maxResultWindow = maxResultWindow;
    }
    
    
}
