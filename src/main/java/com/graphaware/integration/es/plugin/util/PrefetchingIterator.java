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

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class PrefetchingIterator<T> implements Iterator<T>
{

  boolean hasFetchedNext;
  T nextObject;

  /**
   * Tries to fetch the next item and caches it so that consecutive calls (w/o
   * an intermediate call to {@link #next()} will remember it and won't try to
   * fetch it again.
   *
   * @return {@code true} if there was a next item to return in the next call to
   * {@link #next()}.
   */
  @Override
  public boolean hasNext()
  {
    if (hasFetchedNext)
    {
      return getPrefetchedNextOrNull() != null;
    }

    T nextOrNull = fetchNextOrNull();
    hasFetchedNext = true;
    if (nextOrNull != null)
    {
      setPrefetchedNext(nextOrNull);
    }
    return nextOrNull != null;
  }

  /**
   * Uses {@link #hasNext()} to try to fetch the next item and returns it if
   * found, otherwise it throws a {@link NoSuchElementException}.
   *
   * @return the next item in the iteration, or throws
   * {@link NoSuchElementException} if there's no more items to return.
   */
  @Override
  public T next()
  {
    if (!hasNext())
    {
      throw new NoSuchElementException();
    }
    T result = getPrefetchedNextOrNull();
    setPrefetchedNext(null);
    hasFetchedNext = false;
    return result;
  }

  protected abstract T fetchNextOrNull();

  protected void setPrefetchedNext(T nextOrNull)
  {
    this.nextObject = nextOrNull;
  }

  protected T getPrefetchedNextOrNull()
  {
    return nextObject;
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException();
  }
}
