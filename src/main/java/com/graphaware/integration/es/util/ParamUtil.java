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

package com.graphaware.integration.es.util;

import java.util.Map;

public final class ParamUtil {

    public static <T> T extractParameter(String name, Map<String, T> params) {
        T value = params.get(name);

        if (value == null) {
            throw new IllegalStateException("The " + name + " parameter must not be null");
        }

        return value;
    }

    public static <T> T extractParameter(String name, Map<String, T> params, T defaultValue) {
        T value = params.get(name);
        return value != null ? value : defaultValue;
    }

    private ParamUtil() {
    }
}
