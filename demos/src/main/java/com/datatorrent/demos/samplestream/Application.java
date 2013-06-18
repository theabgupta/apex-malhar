/*
 *  Copyright (c) 2012-2013 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.datatorrent.demos.samplestream;

import com.datatorrent.api.StreamingApplication;
import com.datatorrent.api.DAG;
import com.datatorrent.lib.io.ConsoleOutputOperator;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.LoggerFactory;

/**
 * Yahoo Finanace Input Demo : <br>
 * This demo will output the stock market data from yahoo finance
 * 
 * Functional Description : <br>
 * This demo application simply reads yahoo ticker symbols from yahoo finance
 * every 500 ms and prints on output console. <br>
 * <br>
 * 
 * Run Sample Application : <br>
 * Please consult Application Developer guide <a href=
 * "https://docs.google.com/document/d/1WX-HbsGQPV_DfM1tEkvLm1zD_FLYfdLZ1ivVHqzUKvE/edit#heading=h.lfl6f68sq80m"
 * > here </a>.
 * <p>
 * Running Java Test or Main app in IDE:
 * 
 * <pre>
 * LocalMode.runApp(new Application(), 600000); // 10 min run
 * </pre>
 * 
 * Run Success : <br>
 * For successful deployment and run, user should see following output on
 * console: <br>
 * <pre>
 * 013-06-17 16:58:12,108 [1/input:YahooFinanceCSVInputOperator] INFO  stram.StramChild run - activating 1 in container container-0
 * 2013-06-17 16:58:12,119 [container-0] INFO  engine.WindowGenerator activate - Catching up from 1371513490000 to 1371513492119
 * [GOOG, 886.25, 6/17/2013, 4:00pm, +11.21, 879.23, 889.43, 878.28, 2147098]
 * [FB, 24.022, 6/17/2013, 4:00pm, +0.392, 23.92, 24.25, 23.75, 33646600]
 * [YHOO, 26.54, 6/17/2013, 4:00pm, +0.26, 26.32, 26.85, 26.235, 10289754]
 * </pre>
 * 
 * Scaling Options : None <br>
 * 
 *  Application DAG : <br>
 *   Yahoo Csv Input -> Console Output <br>
 * 
 * Streaming Window Size : 500ms 
 * Operator Details : <br>
 * <ul>
 *    <li> <b> The input operator : </b> makes http call to yahoo finance and emits result to appilcation. <br>
 *        Class : {@link com.datatorrent.demos.samplestream.YahooFinanceCSVInputOperator }  <br>
 *        State Less : YES, window count 1 <br>
 *    </li>
  *   <li><b>The operator Console: </b> This operator just outputs the input tuples to the console (or stdout). <br>
 *           if you need to change write to HDFS,HTTP .. instead of console, <br>
 *           Please refer to {@link com.datatorrent.lib.io.HttpOutputOperator} or  {@link com.datatorrent.lib.io.HdfsOutputOperator}.</li>
 * </ul>
 * 
 * @author Zhongjian Wang <zhongjian@malhar-inc.com>
 */
public class Application implements StreamingApplication
{
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Application.class);
  private final boolean allInline = false;

  @Override
  public void populateDAG(DAG dag, Configuration conf)
  {

    YahooFinanceCSVInputOperator input = dag.addOperator("input", new YahooFinanceCSVInputOperator());
    input.addSymbol("GOOG");
    input.addSymbol("FB");
    input.addSymbol("YHOO");
    input.addFormat(YahooFinanceCSVInputOperator.Symbol);
    input.addFormat(YahooFinanceCSVInputOperator.LastTrade);
    input.addFormat(YahooFinanceCSVInputOperator.LastTradeDate);
    input.addFormat(YahooFinanceCSVInputOperator.LastTradeTime);
    input.addFormat(YahooFinanceCSVInputOperator.Change);
    input.addFormat(YahooFinanceCSVInputOperator.Open);
    input.addFormat(YahooFinanceCSVInputOperator.DaysHigh);
    input.addFormat(YahooFinanceCSVInputOperator.DaysLow);
    input.addFormat(YahooFinanceCSVInputOperator.Volume);

    ConsoleOutputOperator consoleOperator = dag.addOperator("console", new ConsoleOutputOperator());
    dag.addStream("input-console", input.outputPort, consoleOperator.input).setInline(allInline);

  }
}
