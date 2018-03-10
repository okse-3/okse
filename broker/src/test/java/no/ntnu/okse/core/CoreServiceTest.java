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

package no.ntnu.okse.core;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import no.ntnu.okse.protocol.ProtocolServer;
import org.junit.BeforeClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.testng.Assert.*;

@Test(singleThreaded = true)
public class CoreServiceTest {

  CoreService cs;
  boolean callback;
  int counter;

  @BeforeClass
  public void boot() throws Exception {
    cs = CoreService.getInstance();
    Field field = CoreService.class.getDeclaredField("protocolServers");
    field.setAccessible(true);
    ArrayList<ProtocolServer> ps = (ArrayList<ProtocolServer>) field.get(cs);
    ps.clear();
    CoreService.protocolServersBooted = false;
    InputStream resourceAsStream = CoreService.class
        .getResourceAsStream("/config/protocolservers.xml");
    cs.bootProtocolServers(resourceAsStream);
  }

  @BeforeMethod
  public void setUp() {
    callback = false;
    counter = 0;
  }

  @AfterMethod
  public void tearDown() {
    callback = false;
    counter = 0;
  }

  @Test
  public void testGetInstance() {
    assertSame(cs, CoreService.getInstance());
  }

  @Test
  public void testExecute() {
    for (int i = 1; i < 11; i++) {
      if (counter < 10) {
        cs.execute(() -> counter++);
      } else {
        cs.execute(() -> assertEquals(counter, 10));
      }
    }
  }

  @Test
  public void testGetEventQueue() {
    assertNotNull(cs.getEventQueue());
    assertTrue(cs.getEventQueue() instanceof LinkedBlockingQueue);
  }

  @Test
  public void testGetExecutor() {
    assertNotNull(cs.getExecutor());
    assertTrue(cs.getExecutor() instanceof ExecutorService);
  }

  @Test
  public void testRegisterService() {
    TestService ts = new TestService();
    cs.registerService(ts);
    assertNotNull(cs.getService(ts.getClass()));
    cs.removeService(ts);
  }

  @Test
  public void testRemoveService() {
    TestService ts = new TestService();
    cs.registerService(ts);
    assertNotNull(cs.getService(ts.getClass()));
    cs.removeService(ts);
    assertNull(cs.getService(ts.getClass()));
  }

  @Test
  public void testGetService() {
    TestService ts = new TestService();
    cs.registerService(ts);
    assertNotNull(cs.getService(ts.getClass()));
    assertSame(cs.getService(ts.getClass()), ts);
    cs.removeService(ts);
  }

  @Test
  public void testAddProtocolServer() {
    TestProtocol tp = new TestProtocol(1);
    cs.addProtocolServer(tp);
    System.out.println(cs.getAllProtocolServers());
    assertTrue(cs.getAllProtocolServers().contains(tp));
    cs.removeProtocolServer(tp);
  }

  @Test
  public void testRemoveProtocolServer() {
    TestProtocol tp = new TestProtocol(1);
    cs.addProtocolServer(tp);
    assertTrue(cs.getAllProtocolServers().contains(tp));
    cs.removeProtocolServer(tp);
    assertFalse(cs.getAllProtocolServers().contains(tp));
  }

  @Test
  public void testGetTotalRequestsFromProtocolServers() {
    TestProtocol tp = new TestProtocol(1);
    TestProtocol tp2 = new TestProtocol(2);
    cs.addProtocolServer(tp);
    cs.addProtocolServer(tp2);
    assertEquals(cs.getTotalRequestsFromProtocolServers(), 9);
    cs.removeProtocolServer(tp);
    cs.removeProtocolServer(tp2);
  }

  @Test
  public void testGetTotalMessagesReceivedFromProtocolServers() {
    TestProtocol tp = new TestProtocol(1);
    TestProtocol tp2 = new TestProtocol(2);
    cs.addProtocolServer(tp);
    cs.addProtocolServer(tp2);
    assertEquals(cs.getTotalMessagesReceivedFromProtocolServers(), 3);
    cs.removeProtocolServer(tp);
    cs.removeProtocolServer(tp2);
  }

  @Test
  public void testGetTotalMessagesSentFromProtocolServers() {
    TestProtocol tp = new TestProtocol(1);
    TestProtocol tp2 = new TestProtocol(2);
    cs.addProtocolServer(tp);
    cs.addProtocolServer(tp2);
    assertEquals(cs.getTotalMessagesSentFromProtocolServers(), 6);
    cs.removeProtocolServer(tp);
    cs.removeProtocolServer(tp2);
  }

  @Test
  public void testGetTotalBadRequestsFromProtocolServers() {
    TestProtocol tp = new TestProtocol(1);
    TestProtocol tp2 = new TestProtocol(2);
    cs.addProtocolServer(tp);
    cs.addProtocolServer(tp2);
    assertEquals(cs.getTotalBadRequestsFromProtocolServers(), 12);
    cs.removeProtocolServer(tp);
    cs.removeProtocolServer(tp2);
  }

  @Test
  public void testGetTotalErrorsFromProtocolServers() {
    TestProtocol tp = new TestProtocol(1);
    TestProtocol tp2 = new TestProtocol(2);
    cs.addProtocolServer(tp);
    cs.addProtocolServer(tp2);
    assertEquals(cs.getTotalErrorsFromProtocolServers(), 15);
    cs.removeProtocolServer(tp);
    cs.removeProtocolServer(tp2);
  }

  @Test
  public void testGetAllProtocolServers() {
    TestProtocol tp = new TestProtocol(1);
    assertFalse(cs.getAllProtocolServers().contains(tp));
    cs.addProtocolServer(tp);
    assertTrue(cs.getAllProtocolServers().contains(tp));
    cs.removeProtocolServer(tp);
  }

  @Test
  public void testGetProtocolServer() {
    TestProtocol tp = new TestProtocol(1);
    cs.addProtocolServer(tp);
    ProtocolServer tp2 = cs.getProtocolServer(tp.getClass());
    assertSame(tp, tp2);
    cs.removeProtocolServer(tp);
  }

  @Test
  public void testGetProtocolServer1() {
    TestProtocol tp = new TestProtocol(1);
    cs.addProtocolServer(tp);
    ProtocolServer tp2 = cs.getProtocolServer("Test");
    assertSame(tp, tp2);
    cs.removeProtocolServer(tp);
  }

  /* HELPER CLASSES */

  public static class TestService extends AbstractCoreService {

    public TestService() {
      super(TestService.class.getName());
    }

    protected void init() {
    }

    public void boot() {
    }

    public void registerListenerSupport() {
    }

    public void run() {
    }

    public void stop() {
    }
  }

  public static class TestProtocol extends AbstractProtocolServer {

    public TestProtocol(Integer baseCount) {
      super("0.0.0.0", 0, "Test");
      totalMessagesReceived.getAndAdd(baseCount);
      totalMessagesSent.getAndAdd(2 * baseCount);
      totalRequests.getAndAdd(3 * baseCount);
      totalBadRequests.getAndAdd(4 * baseCount);
      totalErrors.getAndAdd(5 * baseCount);
    }

    public void boot() {
    }

    public void run() {
    }

    public void stopServer() {
    }

    public void sendMessage(Message message) {
    }
  }
}