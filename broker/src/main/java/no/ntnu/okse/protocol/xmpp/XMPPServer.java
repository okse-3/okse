package no.ntnu.okse.protocol.xmpp;

import java.util.List;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.topic.Topic;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;

public class XMPPServer {

  private Logger log;
  private static String protocolServerType;
  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;
  private XMPPProtocolServer protocolServer;


  public XMPPServer(XMPPProtocolServer protocolServer, String host, Integer port) {
    try {
      this.protocolServer = protocolServer;
      protocolServerType = "xmpp";
      log = Logger.getLogger(XMPPServer.class.getName());

      XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
      configBuilder.setHost(host);
      configBuilder.setPort(port);
      this.connection = new XMPPTCPConnection(configBuilder.build());
      connection.connect();
      connection.login();

      this.pubSubManager = PubSubManager.getInstance(connection);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void stopServer() {
    connection.disconnect();
    log.info("XMPPServer closed");
  }

  public void sendMessage(Message message)
      throws XMPPErrorException, NotAPubSubNodeException, NotConnectedException, InterruptedException, NoResponseException {
    LeafNode node = pubSubManager.getNode(message.getTopic());
    node.publish(messageToPayloadItem(message));
    log.debug("Distributed messages with topic: " + message.getTopic());
  }

  public void setUpPubSubListener(Topic topic)
      throws XMPPErrorException, NotAPubSubNodeException, NotConnectedException, InterruptedException, NoResponseException {
    LeafNode node = pubSubManager.getNode(topic.getName());

    node.addItemEventListener(new PubSubListener<PayloadItem>(this, topic));
    node.subscribe("okse");
  }

  private Message payloadItemTOMessage(PayloadItem pi, Topic topic) {
    return new Message(pi.getPayload().toString(), topic.toString(), null, protocolServer.getProtocolServerType());
  }

  private PayloadItem<SimplePayload> messageToPayloadItem(Message message) {
    SimplePayload payload = new SimplePayload(message.getTopic(), "pubsub:okse", message.getMessage());
    return new PayloadItem<>(payload);
  }

  public void onMessageReceived(List<Item> itemList, Topic topic) {
    log.debug("Received a message with topic: " + topic.getName());
    for (Item item: itemList) {
      if (item instanceof PayloadItem) {
        log.debug("Redistributed message with topic: " + topic.getName());
        MessageService.getInstance().distributeMessage(payloadItemTOMessage((PayloadItem) item, topic));
      }
    }
  }


  //sub and node configuration are to be implemented here

}
