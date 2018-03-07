package no.ntnu.okse.protocol.xmpp;

import java.net.InetAddress;
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
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.xdata.packet.DataForm.Type;

public class XMPPServer {

  private Logger log;
  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;
  private XMPPProtocolServer protocolServer;

  // linked blocking queue for server concurrency?


  /**
   * Init method for XMPPServers
   * @param protocolServer, the managing protocol server
   * @param host, host IP or domain as a {@link String}
   * @param port, host port number as a {@link Integer}
   */
  public XMPPServer(XMPPProtocolServer protocolServer, String host, Integer port) {
    try {
      this.protocolServer = protocolServer;
      log = Logger.getLogger(XMPPServer.class.getName());

      XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
      configBuilder.setHostAddress(InetAddress.getByName(host));
      configBuilder.setPort(port);
      configBuilder.setXmppDomain(host + "/" + port);
      this.connection = new XMPPTCPConnection(configBuilder.build());
      connection.connect();
      connection.login();

      this.pubSubManager = PubSubManager.getInstance(connection);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }


  /**
   * Disconnects ongoing connections and shuts down the server
   */
  public void stopServer() {
    connection.disconnect();
    log.info("XMPPServer closed");
  }


  /**
   * Create a {@link LeafNode} with a NodeID. The node will be configured with what we set to be
   * standard: an open PublishModel, so that everyone can subscribe to the node; an open AccessModel,
   * so that everyone can subscribe to the node; not delivering payloads; and having persistent items.
   *
   * @param nodeId the ID of the created node as a {@link String}
   * @return a configurated {@link LeafNode} with the given NodeID.
   * @throws XMPPErrorException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
   */
  public LeafNode createLeafNode(String nodeId)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    ConfigureForm config = new ConfigureForm(Type.form);
    config.setAccessModel(AccessModel.open);
    config.setDeliverPayloads(false);
    config.setPersistentItems(true);
    config.setPublishModel(PublishModel.open);
    log.debug("Created a new node with id " + nodeId);
    return (LeafNode) pubSubManager.createNode(nodeId, config);
  }


  /**
   * Sends the Message to a node with the messages topic
   *
   * @param message, {@link Message} to be sent
   * @throws XMPPErrorException
   * @throws NotAPubSubNodeException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
   */
  public void sendMessage(Message message)
      throws XMPPErrorException, NotAPubSubNodeException, NotConnectedException,
      InterruptedException, NoResponseException {
    LeafNode node = pubSubManager.getNode(message.getTopic());
    node.publish(messageToPayloadItem(message));
    log.debug("Distributed messages with topic: " + message.getTopic());
  }


  /**
   * Sets up a subscription to a node by adding an event listener
   *
   * @param topic, the {@link Topic} of the relevant node, will create a {@link org.jivesoftware.smackx.pubsub.Node} if one is not found
   * @throws XMPPErrorException
   * @throws NotAPubSubNodeException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
   */
  public void subscribeToNode(Topic topic)
      throws XMPPErrorException, NotAPubSubNodeException, NotConnectedException, InterruptedException, NoResponseException {
    LeafNode node = pubSubManager.getNode(topic.getName());

    //TODO, handle synchronization with other protocols

    node.addItemEventListener(new PubSubListener<PayloadItem>(this, topic));
    node.subscribe(topic.getName() + "@" + connection.getXMPPServiceDomain());
  }

  /**
   * Translates a {@link PayloadItem} to an OKSE {@link Message} Object
   *
   * @param pi, the {@link PayloadItem} to be translated
   * @param topic, the {@link Topic} of the item
   * @return a {@link Message} object containing the payload data, topic and protocol origin
   */
  private Message payloadItemTOMessage(PayloadItem pi, Topic topic) {
    return new Message(pi.getPayload().toString(), topic.toString(), null, protocolServer.getProtocolServerType());
  }

  /**
   * Translates a {@link Message} object to a {@link SimplePayload} object that can be sent as a {@link PayloadItem}
   *
   * @param message, the {@link Message} to be sent
   * @return a {@link PayloadItem} containing the message
   */
  private PayloadItem<SimplePayload> messageToPayloadItem(Message message) {
    SimplePayload payload = new SimplePayload(message.getTopic(), "pubsub:okse:" + message.getTopic(), message.getMessage()); //TODO read namespace from config
    return new PayloadItem<>(payload);
  }

  /**
   * Forwards a received message to the OKSE core to be distributed and updates the tracked information
   *
   * @param itemList, a list of {@link Item} objects passed on from the event listener
   * @param topic, the {@link Topic} of the node that sent the payload
   */
  public void onMessageReceived(List<Item> itemList, Topic topic) {
    protocolServer.incrementTotalMessagesReceived();
    log.debug("Received a message with topic: " + topic.getName());
    for (Item item: itemList) {
      if (item instanceof PayloadItem) {
        MessageService.getInstance().distributeMessage(payloadItemTOMessage((PayloadItem) item, topic));
        log.debug("Redistributed message with topic: " + topic.getName());
        protocolServer.incrementTotalMessagesReceived();
        protocolServer.incrementTotalRequests();

      }
      //else if (Item instanceof ItemPublishEvent){
        //ask for the published message
      //}
    }
  }


}
