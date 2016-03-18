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

package com.graphaware.integration.es.plugin.util;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import org.reflections.Reflections;

public class GASServiceLoader
{
  
  protected final static Logger logger = Logger.getLogger(GASServiceLoader.class.getName());

  private static final Reflections reflections = new Reflections("com.graphaware.integration.es");

  public static <T, A extends Annotation> HashMap<String, Class<T>> loadClass(Class<T> type, Class<A> annotation)
  {
    return loadClassByAnnotation(type, annotation);
  }

  private static <T, A extends Annotation> HashMap<String, Class<T>> loadClassByAnnotation(Class<T> type, Class<A> annotation)
  {
    HashMap<String, Class<T>> loader = new HashMap<>();
    Set<Class<?>> providers = reflections.getTypesAnnotatedWith(annotation);
    for (Class<?> item : providers)
      loader.put(item.getName(), (Class<T>) item);
    return loader;
  }
}
