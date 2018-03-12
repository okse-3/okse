package no.ntnu.okse.clients.xmpp;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.clients.TestClient;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPClient implements TestClient{

  private String JID;
  private String serverHost;
  private Integer serverPort;
  public int messageCounter;
  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;
  private ConcurrentHashMap<String, ItemEventListener<PayloadItem>> listenerMap;



  public XMPPClient(String JID, String host, Integer port) {
    this.JID = JID;
    serverHost = host;
    messageCounter = 0;
    serverPort = port;
  }

  @Override
  public void connect() {
    try {
      connection = new XMPPTCPConnection("testClient", "testPassword", serverHost);
      try {
        connection.connect().login();
        pubSubManager = PubSubManager.getInstance(connection);
      } catch (XMPPException | SmackException | InterruptedException | IOException e) {
        e.printStackTrace();
      }
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void disconnect() {
    connection.disconnect();
  }

  @Override
  public void subscribe(String topic) {
    try {
      LeafNode node = pubSubManager.getLeafNode(topic);
      node.addItemEventListener(new ItemEventListener() {
        @Override
        public void handlePublishedItems(ItemPublishEvent itemPublishEvent) {
          listenerMap.put(topic, this);
          messageCounter++;
        }
      });
      node.subscribe(JID);
    } catch (NoResponseException | XMPPErrorException | NotConnectedException | NotALeafNodeException | InterruptedException | NotAPubSubNodeException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void unsubscribe(String topic) {
    try {
      LeafNode node = pubSubManager.getLeafNode(topic);
      node.unsubscribe(JID);
      node.removeItemEventListener(listenerMap.get(topic));
      listenerMap.remove(topic);
    } catch (NoResponseException | XMPPErrorException | InterruptedException | NotConnectedException | NotALeafNodeException | NotAPubSubNodeException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void publish(String topic, String content) {
    SimplePayload payload = new SimplePayload(topic, "pubsub:okse:" + topic, content);
    try {
      pubSubManager.getLeafNode(topic).publish(new PayloadItem<>(payload));
    } catch (NotConnectedException | InterruptedException | NoResponseException | NotALeafNodeException | NotAPubSubNodeException | XMPPErrorException e) {
      e.printStackTrace();
    }
  }
}
