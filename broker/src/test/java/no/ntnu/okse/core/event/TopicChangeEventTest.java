/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
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

import no.ntnu.okse.core.event.TopicChangeEvent.TopicChangeEventType;
import no.ntnu.okse.core.topic.Topic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TopicChangeEventTest {

  Topic one;
  Topic two;
  TopicChangeEvent tce;

  @BeforeMethod
  public void setUp() {
    one = new Topic("root", "DEFAULT");
    two = new Topic("sub", "DEFAULT");
    two.setParent(one);
    tce = new TopicChangeEvent(TopicChangeEventType.NEW, two);
  }

  @AfterMethod
  public void tearDown() {
    one = null;
    two = null;
    tce = null;
  }

  @Test
  public void testGetData() {
    assertNotNull(tce.getData());
    assertEquals(tce.getData().getFullTopicString(), "root/sub");
    assertSame(tce.getData(), two);
  }

  @Test
  public void testGetType() {
    assertNotNull(tce.getEventType());
    assertEquals(tce.getEventType(), TopicChangeEventType.NEW);
    tce = new TopicChangeEvent(TopicChangeEventType.DELETE, one);
    assertEquals(tce.getEventType(), TopicChangeEventType.DELETE);
  }
}