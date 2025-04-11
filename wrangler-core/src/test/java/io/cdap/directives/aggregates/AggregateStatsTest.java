/*
 *  Copyright u00a9 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingPipelineContext;
import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link AggregateStats} directive.
 */
public class AggregateStatsTest {

  @Test
  public void testBasicAggregation() throws Exception {
    // Create sample data
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_transfer_size", "10KB");
    row1.add("response_time", "100ms");
    rows.add(row1);
    
    Row row2 = new Row();
    row2.add("data_transfer_size", "20KB");
    row2.add("response_time", "150ms");
    rows.add(row2);
    
    Row row3 = new Row();
    row3.add("data_transfer_size", "30KB");
    row3.add("response_time", "200ms");
    rows.add(row3);
    
    // Define the recipe
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec"
    };
    
    // Execute the recipe
    TestingPipelineContext context = new TestingPipelineContext();
    List<Row> results = TestingRig.execute(recipe, rows, context);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);
    
    // Expected values: 60KB = 60 * 1024 bytes = 61440 bytes = 0.0586 MB
    // Expected time: 450ms = 0.45 seconds
    double totalSizeMB = (double) resultRow.getValue("total_size_mb");
    double totalTimeSec = (double) resultRow.getValue("total_time_sec");
    
    Assert.assertEquals(60 * 1024 / (1024.0 * 1024), totalSizeMB, 0.001);
    Assert.assertEquals(450 / 1000.0, totalTimeSec, 0.001);
  }
  
  @Test
  public void testCustomOutputUnits() throws Exception {
    // Create sample data
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_transfer_size", "1MB");
    row1.add("response_time", "1s");
    rows.add(row1);
    
    Row row2 = new Row();
    row2.add("data_transfer_size", "2MB");
    row2.add("response_time", "2s");
    rows.add(row2);
    
    // Define the recipe with custom output units
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time :total_size_kb :total_time_ms 'KB' 'ms'"
    };
    
    // Execute the recipe
    TestingPipelineContext context = new TestingPipelineContext();
    List<Row> results = TestingRig.execute(recipe, rows, context);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);
    
    // Expected values: 3MB = 3 * 1024 KB
    // Expected time: 3s = 3000 ms
    double totalSizeKB = (double) resultRow.getValue("total_size_kb");
    double totalTimeMs = (double) resultRow.getValue("total_time_ms");
    
    Assert.assertEquals(3 * 1024, totalSizeKB, 0.001);
    Assert.assertEquals(3000, totalTimeMs, 0.001);
  }
  
  @Test
  public void testMixedUnitsInput() throws Exception {
    // Create sample data with mixed units
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_transfer_size", "1KB");
    row1.add("response_time", "100ms");
    rows.add(row1);
    
    Row row2 = new Row();
    row2.add("data_transfer_size", "1MB");
    row2.add("response_time", "1s");
    rows.add(row2);
    
    Row row3 = new Row();
    row3.add("data_transfer_size", "1GB");
    row3.add("response_time", "1m");
    rows.add(row3);
    
    // Define the recipe
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec"
    };
    
    // Execute the recipe
    TestingPipelineContext context = new TestingPipelineContext();
    List<Row> results = TestingRig.execute(recipe, rows, context);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);
    
    // Expected values: 1KB + 1MB + 1GB = 1025MB + 1024MB = 1025 + 1024 = 2049MB
    // Expected time: 100ms + 1s + 1m = 0.1s + 1s + 60s = 61.1s
    double totalSizeMB = (double) resultRow.getValue("total_size_mb");
    double totalTimeSec = (double) resultRow.getValue("total_time_sec");
    
    Assert.assertEquals(1 + 1024 + 1, totalSizeMB, 1.0); // Allow some floating point error
    Assert.assertEquals(0.1 + 1 + 60, totalTimeSec, 0.1); // Allow some floating point error
  }
  
  @Test(expected = DirectiveParseException.class)
  public void testInvalidOutputUnit() throws Exception {
    // Create sample data
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("data_transfer_size", "10KB");
    row.add("response_time", "100ms");
    rows.add(row);
    
    // Define the recipe with invalid output unit
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec 'XB' 's'"
    };
    
    // This should throw a DirectiveParseException
    TestingRig.execute(recipe, rows);
  }
  
  @Test
  public void testEmptyInput() throws Exception {
    // Create empty input
    List<Row> rows = new ArrayList<>();
    
    // Define the recipe
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec"
    };
    
    // Execute the recipe
    TestingPipelineContext context = new TestingPipelineContext();
    List<Row> results = TestingRig.execute(recipe, rows, context);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);
    
    // Expected values: 0 bytes = 0 MB, 0 nanoseconds = 0 seconds
    double totalSizeMB = (double) resultRow.getValue("total_size_mb");
    double totalTimeSec = (double) resultRow.getValue("total_time_sec");
    
    Assert.assertEquals(0.0, totalSizeMB, 0.001);
    Assert.assertEquals(0.0, totalTimeSec, 0.001);
  }
  
  @Test
  public void testMissingValues() throws Exception {
    // Create data with missing values
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_transfer_size", "10KB");
    // Missing response_time
    rows.add(row1);
    
    Row row2 = new Row();
    // Missing data_transfer_size
    row2.add("response_time", "100ms");
    rows.add(row2);
    
    Row row3 = new Row();
    row3.add("data_transfer_size", "20KB");
    row3.add("response_time", "200ms");
    rows.add(row3);
    
    // Define the recipe
    String[] recipe = new String[] {
      "aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec"
    };
    
    // Execute the recipe
    TestingPipelineContext context = new TestingPipelineContext();
    List<Row> results = TestingRig.execute(recipe, rows, context);
    
    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);
    
    // Expected values: 30KB = 30 * 1024 bytes = 30720 bytes = 0.0293 MB
    // Expected time: 300ms = 0.3 seconds
    double totalSizeMB = (double) resultRow.getValue("total_size_mb");
    double totalTimeSec = (double) resultRow.getValue("total_time_sec");
    
    Assert.assertEquals(30 * 1024 / (1024.0 * 1024), totalSizeMB, 0.001);
    Assert.assertEquals(300 / 1000.0, totalTimeSec, 0.001);
  }
}
