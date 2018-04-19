package no.ntnu.okse.protocol.xmpp;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import no.ntnu.okse.OpenfireXMPPServerFactory;
import no.ntnu.okse.clients.xmpp.XMPPClient;
import no.ntnu.okse.core.messaging.Message;
import org.mockito.InjectMocks;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XMPPServerTest {

  @InjectMocks
  private XMPPProtocolServer ps;
  private XMPPClient client;
  private XMPPServer server;

  @BeforeMethod
  public void setUp() throws Exception {
    OpenfireXMPPServerFactory.start();
    // Make sure the server starts
    Thread.sleep(5000);
    ps = new XMPPProtocolServer("localhost", 5222, "okse@localhost", "pass");
    ps.boot();
    Thread.sleep(3000);
    server = ps.getServer();

    client = new XMPPClient("localhost", 5222);
    client.connect();
  }

  @AfterMethod
  private void tearDown() {
    client.disconnect();
    ps.stopServer();
    OpenfireXMPPServerFactory.stop();
  }

  @Test
  public void bootServerTest() {
    assertTrue(OpenfireXMPPServerFactory.isRunning());
    assertTrue(ps.isRunning());
    assertNotNull(server);
  }

  @Test
  public void testCreateOrGetLeafNode() throws Exception {
    assertNotNull(server.getLeafNode("testTopic"));
  }


  @Test
  public void testCreateAndSendMessage() throws Exception {
    client.subscribe("testTopic");
    Method sendMessage = XMPPServer.class.getDeclaredMethod("sendMessage", Message.class);
    sendMessage.setAccessible(true);
    sendMessage
        .invoke(server, new Message("testMessage", "testTopic", null, ps.getProtocolServerType()));
    Thread.sleep(1000);
    assertEquals(client.messageCounter, 1);
  }
}
