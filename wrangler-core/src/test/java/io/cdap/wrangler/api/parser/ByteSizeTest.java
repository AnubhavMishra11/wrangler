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
 * Tests for {@link ByteSize} class.
 */
public class ByteSizeTest {

  @Test
  public void testBasicParsing() {
    // Test basic parsing
    ByteSize size1 = new ByteSize("10KB");
    Assert.assertEquals(10 * 1024, size1.getBytes());
    Assert.assertEquals("10KB", size1.getOriginalValue());
    Assert.assertEquals(10.0, size1.getValue(), 0.001);
    Assert.assertEquals("KB", size1.getUnit());
    
    ByteSize size2 = new ByteSize("1.5MB");
    Assert.assertEquals(1.5 * 1024 * 1024, size2.getBytes(), 0.001);
    
    ByteSize size3 = new ByteSize("2GB");
    Assert.assertEquals(2L * 1024L * 1024L * 1024L, size3.getBytes());
    
    ByteSize size4 = new ByteSize("3TB");
    Assert.assertEquals(3L * 1024L * 1024L * 1024L * 1024L, size4.getBytes());
  }
  
  @Test
  public void testCaseInsensitivity() {
    // Test case insensitivity
    ByteSize size1 = new ByteSize("10kb");
    ByteSize size2 = new ByteSize("10KB");
    Assert.assertEquals(size1.getBytes(), size2.getBytes());
    
    ByteSize size3 = new ByteSize("5mb");
    ByteSize size4 = new ByteSize("5MB");
    Assert.assertEquals(size3.getBytes(), size4.getBytes());
  }
  
  @Test
  public void testWhitespace() {
    // Test whitespace handling
    ByteSize size1 = new ByteSize("10 KB");
    Assert.assertEquals(10 * 1024, size1.getBytes());
    
    ByteSize size2 = new ByteSize(" 5MB ");
    Assert.assertEquals(5 * 1024 * 1024, size2.getBytes());
  }
  
  @Test
  public void testUnitConversion() {
    // Test unit conversion
    ByteSize size = new ByteSize("1024KB");
    Assert.assertEquals(1024, size.convertTo("KB"), 0.001);
    Assert.assertEquals(1, size.convertTo("MB"), 0.001);
    Assert.assertEquals(0.001, size.convertTo("GB"), 0.001);
    Assert.assertEquals(1024 * 1024, size.convertTo("B"), 0.001);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    // Test invalid format
    new ByteSize("invalid");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    // Test invalid unit
    new ByteSize("10XB");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnitConversion() {
    // Test invalid unit conversion
    ByteSize size = new ByteSize("10KB");
    size.convertTo("invalid");
  }
  
  @Test
  public void testToJson() {
    // Test JSON serialization
    ByteSize size = new ByteSize("10KB");
    Assert.assertTrue(size.toJson().toString().contains("BYTE_SIZE"));
    Assert.assertTrue(size.toJson().toString().contains("10KB"));
  }
  
  @Test
  public void testTokenType() {
    // Test token type
    ByteSize size = new ByteSize("10KB");
    Assert.assertEquals(TokenType.BYTE_SIZE, size.type());
  }
}
