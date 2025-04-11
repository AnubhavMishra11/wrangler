/*
 *  Copyright © 2017-2019 Cask Data, Inc.
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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * A directive that aggregates byte sizes and time durations and outputs the results in specified columns.
 */
@Plugin(type = Directive.TYPE)
@Name(AggregateStats.NAME)
@Categories(categories = {"aggregator"})
@Description("Aggregates byte sizes and time durations and outputs the results in specified columns.")
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  private static final String TOTAL_BYTES_KEY = "total_bytes";
  private static final String TOTAL_NANOSECONDS_KEY = "total_nanoseconds";
  private static final String COUNT_KEY = "count";
  
  private String byteSizeColumn;
  private String timeDurationColumn;
  private String totalBytesColumn;
  private String totalTimeColumn;
  private String byteSizeOutputUnit;
  private String timeOutputUnit;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("0", TokenType.COLUMN_NAME);
    builder.define("1", TokenType.COLUMN_NAME);
    builder.define("2", TokenType.COLUMN_NAME);
    builder.define("3", TokenType.COLUMN_NAME);
    builder.define("4", TokenType.TEXT, "MB");
    builder.define("5", TokenType.TEXT, "s");
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    // Handle positional parameters with colons
    if (args.contains("0")) {
      this.byteSizeColumn = ((ColumnName) args.value("0")).value();
      this.timeDurationColumn = ((ColumnName) args.value("1")).value();
      this.totalBytesColumn = ((ColumnName) args.value("2")).value();
      this.totalTimeColumn = ((ColumnName) args.value("3")).value();
    } else {
      throw new DirectiveParseException(
          "Improper usage of directive 'aggregate-stats', " +
          "usage - 'aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec'");
    }
    
    // Handle positional parameters for output units
    if (args.contains("4")) {
      this.byteSizeOutputUnit = ((Text) args.value("4")).value();
    } else {
      this.byteSizeOutputUnit = "MB";
    }
    
    if (args.contains("5")) {
      this.timeOutputUnit = ((Text) args.value("5")).value();
    } else {
      this.timeOutputUnit = "s";
    }
    
    // Validate output units
    validateByteSizeUnit(byteSizeOutputUnit);
    validateTimeUnit(timeOutputUnit);
  }

  private void validateByteSizeUnit(String unit) throws DirectiveParseException {
    if (!unit.equals("B") && !unit.equals("KB") && !unit.equals("MB") && 
        !unit.equals("GB") && !unit.equals("TB") && !unit.equals("PB")) {
      throw new DirectiveParseException(
        String.format("Invalid byte size output unit: %s. Supported units are: B, KB, MB, GB, TB, PB", unit));
    }
  }

  private void validateTimeUnit(String unit) throws DirectiveParseException {
    if (!unit.equals("ns") && !unit.equals("ms") && !unit.equals("s") && 
        !unit.equals("m") && !unit.equals("h") && !unit.equals("d")) {
      throw new DirectiveParseException(
        String.format("Invalid time output unit: %s. Supported units are: ns, ms, s, m, h, d", unit));
    }
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    if (context == null) {
      throw new DirectiveExecutionException(NAME, "ExecutorContext is null. Cannot execute directive.");
    }

    TransientStore store = context.getTransientStore();
    
    // Initialize counters if they don't exist
    if (store.get(TOTAL_BYTES_KEY) == null) {
      store.set(TransientVariableScope.GLOBAL, TOTAL_BYTES_KEY, 0L);
    }
    if (store.get(TOTAL_NANOSECONDS_KEY) == null) {
      store.set(TransientVariableScope.GLOBAL, TOTAL_NANOSECONDS_KEY, 0L);
    }
    if (store.get(COUNT_KEY) == null) {
      store.set(TransientVariableScope.GLOBAL, COUNT_KEY, 0L);
    }

    // Process each row and accumulate values
    for (Row row : rows) {
      // Increment the count
      store.increment(TransientVariableScope.GLOBAL, COUNT_KEY, 1L);
      
      // Process byte size
      if (row.find(byteSizeColumn) != -1) {
        Object byteSizeObj = row.getValue(byteSizeColumn);
        try {
          ByteSize byteSize;
          if (byteSizeObj instanceof ByteSize) {
            byteSize = (ByteSize) byteSizeObj;
          } else {
            byteSize = new ByteSize(byteSizeObj.toString());
          }
          
          // Accumulate the bytes
          long currentBytes = (Long) store.get(TOTAL_BYTES_KEY);
          store.set(TransientVariableScope.GLOBAL, TOTAL_BYTES_KEY, currentBytes + byteSize.getBytes());
        } catch (Exception e) {
          throw new DirectiveExecutionException(NAME, 
            String.format("Failed to parse byte size value '%s': %s", byteSizeObj, e.getMessage()), e);
        }
      }
      
      // Process time duration
      if (row.find(timeDurationColumn) != -1) {
        Object timeObj = row.getValue(timeDurationColumn);
        try {
          TimeDuration timeDuration;
          if (timeObj instanceof TimeDuration) {
            timeDuration = (TimeDuration) timeObj;
          } else {
            timeDuration = new TimeDuration(timeObj.toString());
          }
          
          // Accumulate the nanoseconds
          long currentNanos = (Long) store.get(TOTAL_NANOSECONDS_KEY);
          store.set(TransientVariableScope.GLOBAL, TOTAL_NANOSECONDS_KEY, 
                    currentNanos + timeDuration.getNanoseconds());
        } catch (Exception e) {
          throw new DirectiveExecutionException(NAME, 
            String.format("Failed to parse time duration value '%s': %s", timeObj, e.getMessage()), e);
        }
      }
    }
    
    // Always generate an output row with current aggregated values
    // Create a new row with the aggregated values
    Row aggregateRow = new Row();
    
    // Get the accumulated values
    long totalBytes = (Long) store.get(TOTAL_BYTES_KEY);
    long totalNanoseconds = (Long) store.get(TOTAL_NANOSECONDS_KEY);
    long count = (Long) store.get(COUNT_KEY);
    
    // Convert to the requested output units
    double bytesInOutputUnit = convertBytes(totalBytes, byteSizeOutputUnit);
    double timeInOutputUnit = convertTime(totalNanoseconds, timeOutputUnit);
    
    // Add the values to the output row
    aggregateRow.add(totalBytesColumn, bytesInOutputUnit);
    aggregateRow.add(totalTimeColumn, timeInOutputUnit);
    
    // Return the aggregate row
    List<Row> result = new ArrayList<>();
    result.add(aggregateRow);
    return result;
  }

  private double convertBytes(long bytes, String targetUnit) {
    switch (targetUnit) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024);
      case "GB":
        return bytes / (1024.0 * 1024 * 1024);
      case "TB":
        return bytes / (1024.0 * 1024 * 1024 * 1024);
      case "PB":
        return bytes / (1024.0 * 1024 * 1024 * 1024 * 1024);
      default:
        return bytes / (1024.0 * 1024); // Default to MB
    }
  }

  private double convertTime(long nanoseconds, String targetUnit) {
    switch (targetUnit) {
      case "ns":
        return nanoseconds;
      case "ms":
        return nanoseconds / 1_000_000.0;
      case "s":
        return nanoseconds / 1_000_000_000.0;
      case "m":
        return nanoseconds / (60.0 * 1_000_000_000L);
      case "h":
        return nanoseconds / (60.0 * 60 * 1_000_000_000L);
      case "d":
        return nanoseconds / (24.0 * 60 * 60 * 1_000_000_000L);
      default:
        return nanoseconds / 1_000_000_000.0; // Default to seconds
    }
  }
}
