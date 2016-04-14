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

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.domain.SearchResultModifier;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.graphaware.integration.es.domain.ClauseConstants.NAME;

public class Instantiator {

    private final ESLogger logger;
    private final Settings settings;

    private final Map<Class<? extends SearchResultModifier>, Map<String, ?>> classCache = new ConcurrentHashMap<>();

    public Instantiator(Settings settings) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.settings = settings;
    }


    @SuppressWarnings("unchecked")
    public <T extends SearchResultModifier> T instantiate(String clause, Map<String, Object> source, IndexInfo indexInfo, final Class<T> clazz, final Class<? extends Annotation> annotationClass) {
        Map<String, String> params = (Map<String, String>) source.get(clause);
        if (params == null) {
            return null;
        }

        String name = params.get(NAME);
        if (name == null || name.length() < 1) {
            return null;
        }

        T result = instantiatePrivileged(name, indexInfo, clazz, annotationClass);

        if (result != null) {
            result.parseRequest(source);
        } else {
            logger.warn("No {} found with name {}", clazz.getName(), name);
        }

        source.remove(clause);
        return result;
    }

    private <T extends SearchResultModifier> T instantiatePrivileged(final String name, final IndexInfo indexSettings, final Class<T> clazz, final Class<? extends Annotation> annotationClass) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                return instantiate(name, indexSettings, clazz, annotationClass);
            }
        });
    }

    private <T extends SearchResultModifier> T instantiate(String name, IndexInfo indexSettings, Class<T> clazz, Class<? extends Annotation> annotationClass) {
        Class<T> cls = loadCachedClasses(clazz, annotationClass).get(name.toLowerCase());

        if (cls == null) {
            return null;
        }

        try {
            try {
                Constructor<T> constructor = cls.getConstructor(Settings.class, IndexInfo.class);
                return constructor.newInstance(settings, indexSettings);
            } catch (NoSuchMethodException ex) {
                logger.warn("No constructor with settings for class {}. Using default", cls.getName());
                return cls.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException ex) {
            logger.error("Error while initializing new {}", cls.getName(), ex);
        }

        return null;
    }

    private <T extends SearchResultModifier> Map<String, Class<T>> loadCachedClasses(Class<T> clazz, Class<? extends Annotation> annotationClass) {
        @SuppressWarnings("unchecked")
        Map<String, Class<T>> cachedMap = (Map<String, Class<T>>) classCache.get(clazz);

        if (cachedMap == null) {
            cachedMap = loadClasses(clazz, annotationClass);
            classCache.put(clazz, cachedMap);
        }

        return cachedMap;
    }

    private <T> Map<String, Class<T>> loadClasses(Class<T> clazz, Class<? extends Annotation> annotationClass) {
        Collection<Class<T>> classes = PluginClassLoader.loadClass(clazz, annotationClass).values();

        Map<String, Class<T>> result = new HashMap<>();

        for (Class<T> cls : classes) {
            Annotation annotation = cls.getAnnotation(annotationClass);
            try {
                Method nameMethod = annotationClass.getDeclaredMethod("name");
                result.put(((String) nameMethod.invoke(annotation)).toLowerCase(), cls);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }
}
