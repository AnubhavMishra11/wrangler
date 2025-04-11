/*
 * Copyright u00a9 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a time duration value with unit (ms, s, m, h, etc.).
 */
@PublicEvolving
public class TimeDuration implements Token {
  private static final Pattern TIME_DURATION_PATTERN = 
      Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(ns|ms|s|m|h|d)$", Pattern.CASE_INSENSITIVE);
  
  private final String originalValue;
  private final double value;
  private final String unit;
  private final long nanoseconds;

  /**
   * Constructs a TimeDuration object from a string representation like "100ms", "1.5s", etc.
   *
   * @param value String representation of time duration with unit
   * @throws IllegalArgumentException if the value doesn't match the expected pattern
   */
  public TimeDuration(String value) {
    this.originalValue = value;
    Matcher matcher = TIME_DURATION_PATTERN.matcher(value.trim());
    
    if (!matcher.matches()) {
      throw new IllegalArgumentException(String.format(
          "Invalid time duration format: %s. Expected format: <number><unit> (e.g., 100ms, 1.5s)", value));
    }
    
    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2).toLowerCase();
    this.nanoseconds = calculateNanoseconds(this.value, this.unit);
  }

  /**
   * Calculates the number of nanoseconds based on the value and unit.
   *
   * @param value The numeric value
   * @param unit The unit (ns, ms, s, m, h, d)
   * @return The number of nanoseconds
   */
  private long calculateNanoseconds(double value, String unit) {
    switch (unit.toLowerCase()) {
      case "ns":
        return (long) value;
      case "ms":
        return (long) (value * 1_000_000);
      case "s":
        return (long) (value * 1_000_000_000);
      case "m":
        return (long) (value * 60 * 1_000_000_000L);
      case "h":
        return (long) (value * 60 * 60 * 1_000_000_000L);
      case "d":
        return (long) (value * 24 * 60 * 60 * 1_000_000_000L);
      default:
        throw new IllegalArgumentException("Unsupported time unit: " + unit);
    }
  }

  /**
   * Gets the original string value.
   *
   * @return Original string value
   */
  public String getOriginalValue() {
    return originalValue;
  }

  /**
   * Gets the numeric value part.
   *
   * @return Numeric value
   */
  public double getValue() {
    return value;
  }

  /**
   * Gets the unit part (ns, ms, s, etc.).
   *
   * @return Unit string
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Gets the value in nanoseconds.
   *
   * @return Value in nanoseconds
   */
  public long getNanoseconds() {
    return nanoseconds;
  }

  /**
   * Gets the value in milliseconds.
   *
   * @return Value in milliseconds
   */
  public long getMilliseconds() {
    return nanoseconds / 1_000_000;
  }

  /**
   * Gets the value in seconds.
   *
   * @return Value in seconds
   */
  public double getSeconds() {
    return nanoseconds / 1_000_000_000.0;
  }

  /**
   * Converts the nanoseconds to the specified unit.
   *
   * @param targetUnit The target unit (ns, ms, s, m, h, d)
   * @return The value in the target unit
   */
  public double convertTo(String targetUnit) {
    long ns = getNanoseconds();
    switch (targetUnit.toLowerCase()) {
      case "ns":
        return ns;
      case "ms":
        return ns / 1_000_000.0;
      case "s":
        return ns / 1_000_000_000.0;
      case "m":
        return ns / (60.0 * 1_000_000_000L);
      case "h":
        return ns / (60.0 * 60 * 1_000_000_000L);
      case "d":
        return ns / (24.0 * 60 * 60 * 1_000_000_000L);
      default:
        throw new IllegalArgumentException("Unsupported time unit: " + targetUnit);
    }
  }

  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.TIME_DURATION.name());
    object.addProperty("value", originalValue);
    object.addProperty("nanoseconds", nanoseconds);
    return object;
  }

  @Override
  public String toString() {
    return originalValue;
  }
}
