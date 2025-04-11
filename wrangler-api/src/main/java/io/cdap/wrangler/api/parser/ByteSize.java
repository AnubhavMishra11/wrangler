/*
 * Copyright © 2017-2019 Cask Data, Inc.
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
 * Class representing a byte size value with unit (KB, MB, GB, etc.).
 */
@PublicEvolving
public class ByteSize implements Token {
  private static final Pattern BYTE_SIZE_PATTERN = 
      Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(B|KB|MB|GB|TB|PB)$", Pattern.CASE_INSENSITIVE);
  
  private final String originalValue;
  private final double value;
  private final String unit;
  private final long bytes;

  /**
   * Constructs a ByteSize object from a string representation like "10KB", "1.5MB", etc.
   *
   * @param value String representation of byte size with unit
   * @throws IllegalArgumentException if the value doesn't match the expected pattern
   */
  public ByteSize(String value) {
    this.originalValue = value;
    Matcher matcher = BYTE_SIZE_PATTERN.matcher(value.trim());
    
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format("Invalid byte size format: %s. Expected format: <number><unit> (e.g., 10KB, 1.5MB)", value));
    }
    
    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2).toUpperCase();
    this.bytes = calculateBytes(this.value, this.unit);
  }

  /**
   * Calculates the number of bytes based on the value and unit.
   *
   * @param value The numeric value
   * @param unit The unit (B, KB, MB, GB, TB, PB)
   * @return The number of bytes
   */
  private long calculateBytes(double value, String unit) {
    switch (unit.toUpperCase()) {
      case "B":
        return (long) value;
      case "KB":
        return (long) (value * 1024L);
      case "MB":
        return (long) (value * 1024L * 1024L);
      case "GB":
        return (long) (value * 1024L * 1024L * 1024L);
      case "TB":
        return (long) (value * 1024L * 1024L * 1024L * 1024L);
      case "PB":
        return (long) (value * 1024L * 1024L * 1024L * 1024L * 1024L);
      default:
        throw new IllegalArgumentException("Unsupported byte unit: " + unit);
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
   * Gets the unit part (B, KB, MB, etc.).
   *
   * @return Unit string
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Gets the value in bytes.
   *
   * @return Value in bytes
   */
  public long getBytes() {
    return bytes;
  }

  /**
   * Converts the bytes to the specified unit.
   *
   * @param targetUnit The target unit (B, KB, MB, GB, TB, PB)
   * @return The value in the target unit
   */
  public double convertTo(String targetUnit) {
    long bytes = getBytes();
    switch (targetUnit.toUpperCase()) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024L);
      case "GB":
        return bytes / (1024.0 * 1024L * 1024L);
      case "TB":
        return bytes / (1024.0 * 1024L * 1024L * 1024L);
      case "PB":
        return bytes / (1024.0 * 1024L * 1024L * 1024L * 1024L);
      default:
        throw new IllegalArgumentException("Unsupported byte unit: " + targetUnit);
    }
  }

  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.BYTE_SIZE.name());
    object.addProperty("value", originalValue);
    object.addProperty("bytes", bytes);
    return object;
  }

  @Override
  public String toString() {
    return originalValue;
  }
}
