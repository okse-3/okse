/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 - 2018 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.core.event;

import no.ntnu.okse.core.subscription.Publisher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PublisherChangeEventTest {

  PublisherChangeEvent e;
  Publisher p;

  @BeforeMethod
  public void setUp() {
    p = new Publisher("topic", "0.0.0.0", 8080, "Test");
    e = new PublisherChangeEvent(PublisherChangeEvent.Type.REGISTER, p);
  }

  @AfterMethod
  public void tearDown() {
    p = null;
    e = null;
  }

  @Test
  public void testGetData() {
    assertNotNull(e.getData());
    assertTrue(e.getData() instanceof Publisher);
    assertSame(e.getData(), p);
    assertEquals(e.getData().getTopic(), "topic");
  }

  @Test
  public void testGetType() {
    assertNotNull(e.getType());
    assertEquals(e.getType(), PublisherChangeEvent.Type.REGISTER);
    e = new PublisherChangeEvent(PublisherChangeEvent.Type.UNREGISTER, p);
    assertEquals(e.getType(), PublisherChangeEvent.Type.UNREGISTER);
  }
}