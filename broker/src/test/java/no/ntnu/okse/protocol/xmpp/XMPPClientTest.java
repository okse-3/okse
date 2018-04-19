package no.ntnu.okse.protocol.xmpp;

import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import no.ntnu.okse.OpenfireXMPPServerFactory;
import no.ntnu.okse.clients.xmpp.XMPPClient;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.messaging.Message;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class XMPPClientTest {

  private XMPPClient client;
  private XMPPClient client2;
  private PubSubManager client_pubsub;
  private PubSubManager client2_pubsub;

  @BeforeClass
  public void setUp() throws Exception{
    Utilities.createConfigDirectoryAndFilesIfNotExists();
    OpenfireXMPPServerFactory.start();
    // Make sure the server starts
    Thread.sleep(5000);
    XMPPProtocolServerUtil.start();
    Thread.sleep(1000);

    //Creates and connects clients
    client = new XMPPClient("localhost", 5222, "okse2@localhost");
    client.connect();
    client2 = new XMPPClient("localhost", 5222, "ubu@localhost");
    client2.connect();

    //Field access control
    Field f = client.getClass().getDeclaredField("pubSubManager"); //NoSuchFieldException
    f.setAccessible(true);
    client_pubsub = (PubSubManager) f.get(client);

    Field f2 = client2.getClass().getDeclaredField("pubSubManager"); //NoSuchFieldException
    f2.setAccessible(true);
    client2_pubsub = (PubSubManager) f2.get(client);

    assertNotNull(client);
    assertNotNull(client2);
  }

  @AfterClass
  public void tearDown(){
    client.disconnect();
    client2.disconnect();
    client = null;
    client2 = null;
    OpenfireXMPPServerFactory.stop();
    XMPPProtocolServerUtil.stop();
  }

  @Test
  public void testCreateAndSendMessageTwoClients() throws Exception {
    //System.out.println("Running testCreateAndSendMessage");
    client.subscribe("testTopic");
    client2.subscribe("testTopic");
    //System.out.println(client.messageCounter);
    int oldCount = client.messageCounter;
    client.publish("testTopic", "je suis cleint1");
    client2.publish("testTopic", "je suis client2");
    Thread.sleep(1000); //Makes sure transmission completes before end of test
    //System.out.println(client.messageCounter);
    assertTrue(client.messageCounter == oldCount + 2);
  }

  @Test
  public void subscriptionOfTwoNodes() throws Exception{
    client.subscribe("doubleSubTopic");
    client2.subscribe("doubleSubTopic");
    assertEquals(client_pubsub.getNode("doubleSubTopic").discoverInfo().getIdentities(), client2_pubsub.getNode("doubleSubTopic").discoverInfo().getIdentities());

    /*
    client.pubSubManager.getSubscriptions().clear();
    System.out.println(client.pubSubManager.getSubscriptions());
    client.subscribe("doubleSubTopic");
    client2.subscribe("doubleSubTopic");
    System.out.println(client.pubSubManager.getSubscriptions());
    System.out.println(client2.pubSubManager.getSubscriptions());
    assertEquals(client.pubSubManager.getNode("doubleSubTopic").discoverInfo().getIdentities(), client2.pubSubManager.getNode("doubleSubTopic").discoverInfo().getIdentities());
    */
  }

  @Test
  public void unsubscribeClient() throws Exception{
    List<Subscription> preSubscriptionState  = client_pubsub.getSubscriptions();
    client.subscribe("testDisconnect");
    List<Subscription> postSubscriptionState  = client_pubsub.getSubscriptions();
    client.unsubscribe("testDisconnect");
    List<Subscription> postUnSubscriptionState  = client_pubsub.getSubscriptions();

    /*
    List<Subscription> preSubscriptionState  = client.pubSubManager.getSubscriptions();
    client.subscribe("testDisconnect");
    List<Subscription> postSubscriptionState  = client.pubSubManager.getSubscriptions();
    client.unsubscribe("testDisconnect");
    List<Subscription> postUnSubscriptionState  = client.pubSubManager.getSubscriptions();
    */

    assertEquals(preSubscriptionState.toString(), postUnSubscriptionState.toString());
    assertNotEquals(preSubscriptionState, postSubscriptionState);
  }


  //XMPP to XMPP Client test runs in MessageSendingTest.java

/*
  public void testClientEndToEndMessageContent() throws Exception {
    client.subscribe("messageReceptionTopic");
    int clientMessageCount = client.messageCounter;
    client2.subscribe("messageReceptionTopic");
    client.publish("messageReceptionTopic", "End to end transmission");
    assertEquals(clientMessageCount, clientMessageCount++);
  }
  */
}
