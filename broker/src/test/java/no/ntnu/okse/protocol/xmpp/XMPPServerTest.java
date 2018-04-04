package no.ntnu.okse.protocol.xmpp;


import static org.testng.Assert.*;

import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.clients.xmpp.XMPPClient;
import no.ntnu.okse.core.messaging.Message;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XMPPServerTest {

  XMPPServer server;
  XMPPProtocolServer ps;
  XMPPClient client;

  @BeforeMethod
  public void setUp() throws Exception {
    ps = new XMPPProtocolServer("localhost", 5222, "okse@localhost", "pass");
    ps.boot();
    server = ((XMPPServer) ps.getClass().getField("server").get(ps));
    client = new XMPPClient("testClient", "localhost", 5222);
  }

  @AfterMethod
  private void tearDown() throws Exception {
    client = null;
    ps.stopServer();
  }

  @Test
  public void testCreateOrGetLeafNode() throws Exception {
    assertNotNull(server.getLeafNode("testTopic"));
  }

  @Test
  public void testCreateAndSendMessage() throws Exception {
    server.subscribeToTopic("testTopic");
    client.subscribe("testTopic");
    int oldCount = client.messageCounter;
    server.sendMessage(new Message("testMessage", "testTopic", null, ps.getProtocolServerType()));
    assertTrue(client.messageCounter == oldCount + 1);
  }

  @Test
  public void testSubscribeUnsubscribeToNode() throws Exception {
    ConcurrentHashMap listenerMap = (ConcurrentHashMap) server.getClass().getField("listenerMap").get(server);
    server.subscribeToTopic("testTopic");
    assertTrue(listenerMap.get("testTopic") != null);
  }

  @Test
  public void testOnMessageReceived() throws Exception {
    server.subscribeToTopic("testTopic");
    client.publish("testTopic", "testMessage");

  }

}