package no.ntnu.okse.protocol.xmpp;


import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.OpenfireXMPPServerFactory;
import no.ntnu.okse.clients.xmpp.XMPPClient;
import no.ntnu.okse.core.messaging.Message;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XMPPServerTest {

  @InjectMocks
  private XMPPProtocolServer ps = new XMPPProtocolServer("localhost", 5222, "okse@localhost", "pass");
  @Mock(name = "server")
  private XMPPServer xmppServerSpy;


  private XMPPClient client;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    ps.boot();
    Field serverField = ps.getClass().getDeclaredField("server");
    serverField.setAccessible(true);
    xmppServerSpy = (XMPPServer) serverField.get(ps);
    OpenfireXMPPServerFactory.start();
    client = new XMPPClient("testClient", "localhost", 5222);
  }

  @AfterMethod
  private void tearDown() throws Exception {
    client = null;
    ps.stopServer();
    OpenfireXMPPServerFactory.stop();
  }

  @Test
  public void bootServerTest() {
    assertTrue(OpenfireXMPPServerFactory.isRunning());
    assertTrue(ps.isRunning());
  }

  @Test
  public void testCreateOrGetLeafNode() throws Exception {
    assertNotNull(xmppServerSpy.getLeafNode("testTopic"));
  }

/*  @Test
  public void testCreateAndSendMessage() throws Exception {
    //server.subscribeToTopic("testTopic");
    client.subscribe("testTopic");
    int oldCount = client.messageCounter;
    server.sendMessage(new Message("testMessage", "testTopic", null, ps.getProtocolServerType()));
    assertTrue(client.messageCounter == oldCount + 1);
  }*/

  @Test
  public void testSubscribeUnsubscribeToNode() throws Exception {
    ConcurrentHashMap listenerMap = (ConcurrentHashMap) xmppServerSpy.getClass().getField("listenerMap").get(xmppServerSpy);
    xmppServerSpy.subscribeToTopic("testTopic");
    assertTrue(listenerMap.get("testTopic") != null);
  }

  @Test
  public void testOnMessageReceived() throws Exception {
    xmppServerSpy.subscribeToTopic("testTopic");
    client.publish("testTopic", "testMessage");

  }

}