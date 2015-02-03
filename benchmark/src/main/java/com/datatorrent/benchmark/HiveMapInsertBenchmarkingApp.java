/*
 * Copyright (c) 2014 DataTorrent, Inc. ALL Rights Reserved.
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
package com.datatorrent.benchmark;

import org.apache.hadoop.conf.Configuration;

import com.datatorrent.lib.testbench.RandomEventGenerator;

import com.datatorrent.api.Context.PortContext;
import com.datatorrent.api.DAG;
import com.datatorrent.api.StreamingApplication;
import com.datatorrent.api.annotation.ApplicationAnnotation;
import com.datatorrent.contrib.hive.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application used to benchmark HIVE Map Insert operator
 * The DAG consists of random Event generator operator that is
 * connected to random map output operator that writes to a file in HDFS.&nbsp;
 * The map output operator is connected to hive Map Insert output operator which writes
 * the contents of HDFS file to hive tables.
 * <p>
 *
 */
@ApplicationAnnotation(name = "HiveMapInsertBenchmarkingApp")
public class HiveMapInsertBenchmarkingApp implements StreamingApplication
{
  Logger LOG = LoggerFactory.getLogger(HiveInsertBenchmarkingApp.class);

  @Override
  public void populateDAG(DAG dag, Configuration conf)
  {
    HiveStore store = new HiveStore();
    store.setDbUrl(conf.get("dt.application.HiveMapInsertBenchmarkingApp.operator.HiveOperator.store.dbUrl"));
    store.setConnectionProperties(conf.get("dt.application.HiveMapInsertBenchmarkingApp.operator.HiveOperator.store.connectionProperties"));
    store.setFilepath(conf.get("dt.application.HiveMapInsertBenchmarkingApp.operator.HiveOperator.store.filepath"));
    try {
      hiveInitializeMapDatabase(store, conf.get("dt.application.HiveMapInsertBenchmarkingApp.operator.HiveOperator.tablename"),":");
    }
    catch (SQLException ex) {
      LOG.debug(ex.getMessage());
    }
    dag.setAttribute(DAG.STREAMING_WINDOW_SIZE_MILLIS, 1000);
    RandomEventGenerator eventGenerator = dag.addOperator("EventGenerator", RandomEventGenerator.class);
    RandomMapOutput mapGenerator = dag.addOperator("MapGenerator", RandomMapOutput.class);
    dag.setAttribute(eventGenerator, PortContext.QUEUE_CAPACITY, 10000);
    dag.setAttribute(mapGenerator, PortContext.QUEUE_CAPACITY, 10000);
    HiveOperator hiveInsert = dag.addOperator("HiveOperator", new HiveOperator());
    hiveInsert.setStore(store);
    FSRollingMapTestImpl rollingMapFsWriter = dag.addOperator("RollingFsMapWriter", new FSRollingMapTestImpl());
    rollingMapFsWriter.setConverter(new MapConverter());
    rollingMapFsWriter.setFilePath(store.filepath);
    ArrayList<String> hivePartitionColumns = new ArrayList<String>();
    hivePartitionColumns.add("dt");
    hiveInsert.setHivePartitionColumns(hivePartitionColumns);
    dag.addStream("EventGenerator2Map", eventGenerator.integer_data, mapGenerator.input);
    dag.addStream("MapGenerator2HdfsOutput", mapGenerator.map_data, rollingMapFsWriter.input);
    dag.addStream("FsWriter2Hive", rollingMapFsWriter.outputPort, hiveInsert.input);

  }

  /*
   * User can create table and specify data columns and partition columns in this function.
   */
  public static void hiveInitializeMapDatabase(HiveStore hiveStore, String tablename, String delimiterMap) throws SQLException
  {
    hiveStore.connect();
    Statement stmt = hiveStore.getConnection().createStatement();
    stmt.execute("CREATE TABLE IF NOT EXISTS " + tablename + " (col1 map<string,int>) PARTITIONED BY(dt STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\n'  \n"
            + "MAP KEYS TERMINATED BY '" + delimiterMap + "' \n"
            + "STORED AS TEXTFILE ");
    hiveStore.disconnect();
  }

  private static class FSRollingMapTestImpl extends FSRollingOutputOperator<Map<String, Object>>
  {
    private final Random random = new Random();

    @Override
    public ArrayList<String> getHivePartition(Map<String, Object> tuple)
    {
      ArrayList<String> hivePartitions = new ArrayList<String>();
      hivePartitions.add("2014-12-1" + random.nextInt(10));
      return (hivePartitions);
    }

  }

}