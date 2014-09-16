/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.lib.math;

import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.annotation.OutputPortFieldAnnotation;
import com.datatorrent.api.annotation.Stateless;
import com.datatorrent.lib.algo.MatchMap;
import com.datatorrent.lib.util.UnifierHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A compare operation is done based on the property "key", "value", and "compare".&nbsp; 
 * If the tuple passed the test, it is emitted on the output port "compare".&nbsp; 
 * If the tuple fails it is emitted on port "except".&nbsp; The comparison is done by getting double value from the Number.
 * <p>
 * Both output ports are optional, but at least one has to be connected.
 * This module is a pass through<br>
 * <br>
 * <b>Ports</b>:<br>
 * <b>data</b>: expects Map&lt;K,V&gt;<br>
 * <b>compare</b>: emits HashMap&lt;K,V&gt;<br>
 * <b>except</b>: emits HashMap&lt;K,V&gt;<br>
 * <br>
 * <b>Properties</b>:<br>
 * <b>key</b>: The key on which compare is done<br>
 * <b>value</b>: The value to compare with<br>
 * <b>cmp</b>: The compare function. Supported values are "lte", "lt", "eq", "neq", "gt", "gte". Default is "eq"<br>
 * <br>
 * Compile time checks<br>
 * Key must be non empty<br>
 * Value must be able to convert to a "double"<br>
 * Compare string, if specified, must be one of "lte", "lt", "eq", "neq", "gt", "gte"<br>
 * <b>Specific run time checks</b>:<br>
 * Does the incoming HashMap have the key<br>
 * Is the value of the key a number<br>
 * <p>
 * <b>Benchmarks</b>: Blast as many tuples as possible in inline mode<br>
 * <table border="1" cellspacing=1 cellpadding=1 summary="Benchmark table for CompareExceptMap&lt;K,V extends Number&gt; operator template">
 * <tr><th>In-Bound</th><th>Out-bound</th><th>Comments</th></tr>
 * <tr><td><b>5 Million K,V pairs/s</b></td><td>Each tuple is emitted if emitError is set to true</td><td>In-bound rate determines performance as every tuple is emitted.
 * Immutable tuples were used in the benchmarking. If you use mutable tuples and have lots of keys, the benchmarks may be lower</td></tr>
 * </table><br>
 * <p>
 * <b>Function Table (K=String, V=Integer); emitError=true; key=a; value=3; cmp=eq)</b>:
 * <table border="1" cellspacing=1 cellpadding=1 summary="Function table for CompareExceptMap&lt;K,V extends Number&gt; operator template">
 * <tr><th rowspan=2>Tuple Type (api)</th><th>In-bound (process)</th><th colspan=2>Out-bound (emit)</th></tr>
 * <tr><th><i>data</i>(HashMap&lt;K,V&gt;)</th><th><i>compare</i>(HashMap&lt;K,V&gt;)</th><th><i>except</i>(HashMap&lt;K,V&gt;)</th></tr>
 * <tr><td>Begin Window (beginWindow())</td><td>N/A</td><td>N/A</td><td>N/A</td></tr>
 * <tr><td>Data (process())</td><td>{a=2,b=20,c=1000}</td><td></td><td>{a=2,b=20,c=1000}</td></tr>
 * <tr><td>Data (process())</td><td>{a=3,b=40,c=2}</td><td>{a=3,b=40,c=2}</td><td></td></tr>
 * <tr><td>Data (process())</td><td>{a=10,b=5}</td><td></td><td>{a=10,b=5}</td></tr>
 * <tr><td>Data (process())</td><td>{d=55,b=12}</td><td></td><td>{d=55,b=12}</td></tr>
 * <tr><td>Data (process())</td><td>{d=22,a=4}</td><td></td><td>{d=22,a=4}</td></tr>
 * <tr><td>Data (process())</td><td>{d=4,a=3,g=5,h=44}</td><td>{d=4,a=3,g=5,h=44}</td><td></td></tr>
 * <tr><td>End Window (endWindow())</td><td>N/A</td><td>N/A</td><td>N/A</td></tr>
 * </table>
 * <br>
 * <br>
 * @displayname: Compare Except Map
 * @category: lib.math
 * @tags: comparison, key value, number, HashMap
 * @since 0.3.2
 */
@Stateless
public class CompareExceptMap<K, V extends Number> extends MatchMap<K, V>
{
  @OutputPortFieldAnnotation(name = "compare", optional=true)
  public final transient DefaultOutputPort<HashMap<K, V>> compare = match;

  @OutputPortFieldAnnotation(name = "expect", optional=true)
  public final transient DefaultOutputPort<HashMap<K, V>> except = new DefaultOutputPort<HashMap<K, V>>()
  {
    @Override
    public Unifier<HashMap<K, V>> getUnifier()
    {
      return new UnifierHashMap<K, V>();
    }
  };

  /**
   * Emits if compare port is connected
   * @param tuple
   */
  @Override
  public void tupleMatched(Map<K, V> tuple)
  {
    if (compare.isConnected()) {
      compare.emit(cloneTuple(tuple));
    }
  }

  /**
   * Emits if except port is connected
   * @param tuple
   */
  @Override
  public void tupleNotMatched(Map<K, V> tuple)
  {
    if (except.isConnected()) {
      except.emit(cloneTuple(tuple));
    }
  }
}
