package no.ntnu.okse.protocol.xmpp;


import static org.testng.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.OpenfireXMPPServerFactory;
import no.ntnu.okse.clients.xmpp.XMPPClient;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.messaging.Message;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PayloadItem;
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

  private ArrayList<Item> itemList = new ArrayList<>();


  @BeforeClass
  public void setUp() throws Exception {
    //MockitoAnnotations.initMocks(this);
    Utilities.createConfigDirectoryAndFilesIfNotExists();
    OpenfireXMPPServerFactory.start();
    // Make sure the server starts
    Thread.sleep(5000);
    ps = new XMPPProtocolServer("localhost", 5222, "okse@localhost", "pass");
    ps.boot();
    Thread.sleep(1000);
    server = ps.getServer();
  }

  @AfterClass
  private void tearDown() throws Exception {
    ps.stopServer();
    server = null;
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
    LeafNode nodeToCheck = server.getLeafNode("testTopic");
    assertEquals(nodeToCheck.getItems().size(), 0);
    server.sendMessage(new Message("testMessage", "testTopic", null, ps.getProtocolServerType()));
    assertEquals(nodeToCheck.getItems().size(), 1);
  }

  @Test
  public void testSubscribeUnsubscribeToNode() throws Exception {
    Node nodeToCheck = server.getLeafNode("testTopic");
    assertEquals(nodeToCheck.getSubscriptions().size(), 1);
    server.subscribeToTopic("testTopic");
    assertEquals(nodeToCheck.getSubscriptions().size(), 2);
    server.unsubscribeFromTopic("testTopic");
    assertEquals(nodeToCheck.getSubscriptions().size(), 1);
  }

  @Test
  public void testOnMessageReceived() throws Exception {
    itemList.add(new PayloadItem<>("item1", new Item()));
    int messageCount = ps.getTotalMessagesReceived();
    server.onMessageReceived(itemList, "testTopic");
    assertEquals(ps.getTotalMessagesReceived(), messageCount + 1);
  }




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