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

package com.graphaware.es.gas.util;

import com.google.common.io.BaseEncoding;

public class UrlUtil {

    public static String buildUrlFromParts(String... parts) {
        boolean isFirst = true;
        String url = "";
        for (String part : parts) {
            while (part.endsWith("/")) {
                part = part.substring(0, part.length()-1);
            }
            while (part.startsWith("/")) {
                part = part.substring(1, part.length());
            }
            if (!isFirst) {
                url += "/";
            }
            url += part;
            isFirst = false;
        }

        return url;
    }

    private UrlUtil() {
    }
    
    public static String getAuthorizationHeaderValue(String username, String password) {
        String value = username + ":" + password;

        return "Basic " + BaseEncoding.base64().encode(value.getBytes());
    }
}
