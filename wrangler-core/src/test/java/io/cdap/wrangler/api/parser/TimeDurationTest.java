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

package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TimeDuration} class.
 */
public class TimeDurationTest {

  @Test
  public void testBasicParsing() {
    // Test basic parsing
    TimeDuration time1 = new TimeDuration("100ms");
    Assert.assertEquals(100 * 1_000_000, time1.getNanoseconds());
    Assert.assertEquals("100ms", time1.getOriginalValue());
    Assert.assertEquals(100.0, time1.getValue(), 0.001);
    Assert.assertEquals("ms", time1.getUnit());
    Assert.assertEquals(100, time1.getMilliseconds());
    Assert.assertEquals(0.1, time1.getSeconds(), 0.001);
    
    TimeDuration time2 = new TimeDuration("1.5s");
    Assert.assertEquals(1.5 * 1_000_000_000, time2.getNanoseconds(), 0.001);
    Assert.assertEquals(1500, time2.getMilliseconds());
    Assert.assertEquals(1.5, time2.getSeconds(), 0.001);
    
    TimeDuration time3 = new TimeDuration("2m");
    Assert.assertEquals(2 * 60 * 1_000_000_000L, time3.getNanoseconds());
    
    TimeDuration time4 = new TimeDuration("3h");
    Assert.assertEquals(3 * 60 * 60 * 1_000_000_000L, time4.getNanoseconds());
  }
  
  @Test
  public void testCaseInsensitivity() {
    // Test case insensitivity
    TimeDuration time1 = new TimeDuration("100ms");
    TimeDuration time2 = new TimeDuration("100MS");
    Assert.assertEquals(time1.getNanoseconds(), time2.getNanoseconds());
    
    TimeDuration time3 = new TimeDuration("5s");
    TimeDuration time4 = new TimeDuration("5S");
    Assert.assertEquals(time3.getNanoseconds(), time4.getNanoseconds());
  }
  
  @Test
  public void testWhitespace() {
    // Test whitespace handling
    TimeDuration time1 = new TimeDuration("100 ms");
    Assert.assertEquals(100 * 1_000_000, time1.getNanoseconds());
    
    TimeDuration time2 = new TimeDuration(" 5s ");
    Assert.assertEquals(5L * 1_000_000_000L, time2.getNanoseconds());
  }
  
  @Test
  public void testUnitConversion() {
    // Test unit conversion
    TimeDuration time = new TimeDuration("1000ms");
    Assert.assertEquals(1000, time.convertTo("ms"), 0.001);
    Assert.assertEquals(1, time.convertTo("s"), 0.001);
    Assert.assertEquals(1000 * 1_000_000, time.convertTo("ns"), 0.001);
    Assert.assertEquals(1.0 / 60, time.convertTo("m"), 0.001);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    // Test invalid format
    new TimeDuration("invalid");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    // Test invalid unit
    new TimeDuration("10xs");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnitConversion() {
    // Test invalid unit conversion
    TimeDuration time = new TimeDuration("10ms");
    time.convertTo("invalid");
  }
  
  @Test
  public void testToJson() {
    // Test JSON serialization
    TimeDuration time = new TimeDuration("10ms");
    Assert.assertTrue(time.toJson().toString().contains("TIME_DURATION"));
    Assert.assertTrue(time.toJson().toString().contains("10ms"));
  }
  
  @Test
  public void testTokenType() {
    // Test token type
    TimeDuration time = new TimeDuration("10ms");
    Assert.assertEquals(TokenType.TIME_DURATION, time.type());
  }
}
