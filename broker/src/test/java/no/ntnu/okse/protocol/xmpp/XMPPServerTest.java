package no.ntnu.okse.protocol.xmpp;


import static org.testng.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.OpenfireXMPPServerFactory;
import no.ntnu.okse.clients.xmpp.XMPPClient;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.messaging.Message;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jxmpp.jid.impl.JidCreate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class XMPPServerTest {


  //@InjectMocks
  private XMPPProtocolServer ps;
  //@Mock(name = "server")
  private XMPPServer server;

  private XMPPClient client;



  @BeforeClass
  public void setUp() throws Exception {
    //MockitoAnnotations.initMocks(this);
    Utilities.createConfigDirectoryAndFilesIfNotExists();
    OpenfireXMPPServerFactory.start();
    // Make sure the server starts
    Thread.sleep(5000);
    XMPPProtocolServerUtil.start();
    Thread.sleep(1000);
    client = new XMPPClient("testClient", "localhost", 5222);
    client.connect();
  }

  @AfterClass
  private void tearDown() throws Exception {
    client.disconnect();
    client = null;
    ps.stopServer();
    OpenfireXMPPServerFactory.stop();
  }

  @Test
  public void bootServerTest() {
    assertTrue(OpenfireXMPPServerFactory.isRunning());
    assertTrue(XMPPProtocolServerUtil.isRunning());
    assertNotNull(this.client);
//    assertNotNull(this.server);
  }

  @Test
  public void testCreateOrGetLeafNode() throws Exception {
    //assertNotNull(server.getLeafNode("testTopic"));
  }

  @Test
  public void testCreateAndSendMessage() throws Exception {
    client.subscribe("testTopic");
    int oldCount = client.messageCounter;
    //server.sendMessage(new Message("testMessage", "testTopic", null, ps.getProtocolServerType()));
    client.publish("testTopic", "testMessage");
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
    int messageCount = ps.getTotalMessagesReceived();
    server.subscribeToTopic("testTopic");
    client.publish("testTopic", "testMessage");
    assertEquals(messageCount + 1, ps.getTotalMessagesReceived());
  }

  @Test


  //Following test is not possible based on current architecture. Please conduct XMPP to XMPP communication tests manually.
  /*
  @Test
  public void testEndToEnd() throws Exception {
    XMPPClient receiver = new XMPPClient("testReceiverClient", "localhost", 5222 );
    receiver.connect();
    client.subscribe("testTopic");
    receiver.subscribe("testTopic");
  }
*/

}