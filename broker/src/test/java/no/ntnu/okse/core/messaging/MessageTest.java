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

package no.ntnu.okse.core.messaging;

import no.ntnu.okse.core.subscription.Publisher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.testng.Assert.*;

public class MessageTest {

    Message m;
    Publisher p;

    @BeforeMethod
    public void setUp() {
        p = new Publisher("test/sub", "0.0.0.0", 8080, "Test");
        m = new Message("test", "test/sub", p, "Test");
    }

    @AfterMethod
    public void tearDown() {
        m = null;
        p = null;
    }

    @Test
    public void testGetMessageID() {
        HashSet<String> ids = new HashSet<>();
        assertNotNull(m.getMessageID());
        // Hex regex
        assertTrue(m.getMessageID().matches("-?[0-9a-fA-F]+"));
        // Do a mass test and check for colliding ID's
        for (int i = 0; i < 1337; i++) {
            setUp();
            ids.add(m.getMessageID());
            tearDown();
        }
        assertEquals(ids.size(), 1337);
    }

    @Test
    public void testGetMessage() {
        assertNotNull(m.getMessage());
        assertEquals(m.getMessage(), "test");
    }

    @Test
    public void testGetTopic() {
        assertEquals(m.getTopic(), "test/sub");
    }

    @Test
    public void testGetPublisher() {
        assertNotNull(m.getPublisher());
        assertEquals(m.getPublisher().getTopic(), "test/sub");
        assertEquals(m.getPublisher().getOriginProtocol(), "Test");
    }

    @Test
    public void testMessageSentFromRegisteredPublisher() {
        assertNotNull(m.getPublisher());
        assertTrue(m.messageSentFromRegisteredPublisher());
        p = null;
        m = new Message("test", "test/sub", p, "Test");
        assertNull(m.getPublisher());
        assertFalse(m.messageSentFromRegisteredPublisher());
    }

    @Test
    public void testSetOriginProtocol() {
        m.setOriginProtocol("OKSE");
        assertEquals(m.getOriginProtocol(), "OKSE");
    }

    @Test
    public void testGetOriginProtocol() {
        assertEquals(m.getOriginProtocol(), "Test");
    }

    @Test
    public void testGetCreationTime() {
        assertTrue(m.getCreationTime() instanceof LocalDateTime);
        assertTrue(m.getCreationTime().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    public void testIsProcessed() {
        assertFalse(m.isProcessed());
    }

    @Test
    public void testSetProcessed() {
        m.setProcessed();
        assertTrue(m.isProcessed());
    }

    @Test
    public void testSetAttribute() {
        m.setAttribute("flag", "value");
        assertEquals(m.getAttribute("flag"), "value");
        assertNull(m.getAttribute("FLAG"));
    }

    @Test
    public void testGetAttribute() {
        assertNull(m.getAttribute("flag"));
        m.setAttribute("flag", "value");
        assertEquals(m.getAttribute("flag"), "value");
    }

    @Test
    public void testGetAttributes() {
        assertEquals(0, m.getAttributes().size());
        m.setAttribute("flag", "value");
        assertEquals(1, m.getAttributes().size());
    }

    @Test
    public void testGetCompletionTime() {
        LocalDateTime completed = m.setProcessed();
        assertTrue(completed.equals(m.getCompletionTime()));
    }

    @Test
    public void testSetSystemMessage() {
        assertFalse(m.isSystemMessage());
        m.setSystemMessage(true);
        assertTrue(m.isSystemMessage());
    }

    @Test
    public void testIsSystemMessage() {
        assertFalse(m.isSystemMessage());
        m.setSystemMessage(true);
        assertTrue(m.isSystemMessage());
    }

    @Test
    public void testToString() {
        assertNotNull(m.toString());
        assertTrue(m.toString() instanceof String);
    }
}