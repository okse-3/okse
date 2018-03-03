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
   * @param host, host IP or domain as a {@link String} //TODO: decide whether to use IP address or DNS. IP requires use of setHostAddress()
   * @param port, host port number as a {@link Integer}
   */
  public XMPPServer(XMPPProtocolServer protocolServer, String host, Integer port) {
    try {
      this.protocolServer = protocolServer;
      log = Logger.getLogger(XMPPServer.class.getName());

      //TODO, move this to configureConnection method
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


  /**
   *
   * @return
   */
  private XMPPTCPConnection configureConnection() {
    //TODO read from config and apply to connection
    return null;
  }

  /**
   * Disconnects ongoing connections and shuts down the server
   */
  public void stopServer() {
    connection.disconnect();
    log.info("XMPPServer closed");
  }

  /**
   * Create a {@link LeafNode} with a NodeID and a predetermined configuration
   *
   * @param nodeId the ID of the Node as a {@link String}
   * @param config the premade {@link ConfigureForm} for the Node
   * @throws XMPPErrorException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
   */
  public LeafNode createLeafNodeWithForm(String nodeId, ConfigureForm config)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    LeafNode leaf = (LeafNode) pubSubManager.createNode(nodeId, config);
    return leaf;
  }

  /**
   * Create a {@link LeafNode} with a NodeID, and configuring it in the function.
   *
   * @param nodeId the ID of the created node as a {@link String}
   * @return a configurated {@link LeafNode} with the given NodeID.
   * @throws XMPPErrorException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
   */
  public LeafNode createLeafNodeWithConfig(String nodeId)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    //TODO: This is an example method of how this can be done, figured it would be good to have
    ConfigureForm config = new ConfigureForm(Type.form);
    config.setAccessModel(AccessModel.open); // can set to open, roster, whitelist, presence and authorize
    config.setDeliverPayloads(false); // deliver payloads or only send event notifications
    config.setNotifyRetract(true); // set whether subscribers should be notified when items are deleted from the node
    config.setPersistentItems(true); // set whether items should be persisted in the node
    config.setPublishModel(PublishModel.open); // open, publishers or subscribers. Sets who can publish to this node.

    LeafNode leaf = (LeafNode) pubSubManager.createNode(nodeId, config);
    return leaf;
  }

  /**
   * Create a {@link ConfigureForm} that contains the necessary configuration options for a node.
   *
   * @param accessModel A type of {@link AccessModel} setting the access model of the node.
   * @param deliverPayloads A boolean determining whether or not the node should deliver payloads.
   * @param notifyRetract A boolean determining whether or not the node should notify subscribers
   * of retractions.
   * @param persistentItems A boolean determining whether or not the node should persist items.
   * @param publishModel A type of {@link PublishModel} setting the publish model of the node.
   * @return a filled in {@link ConfigureForm} with node configurations.
   */
  public ConfigureForm createNodeConfiguration(AccessModel accessModel, boolean deliverPayloads,
      boolean notifyRetract, boolean persistentItems, PublishModel publishModel) {
    ConfigureForm config = new ConfigureForm(Type.form);
    config.setAccessModel(accessModel);
    config.setDeliverPayloads(deliverPayloads);
    config.setNotifyRetract(notifyRetract);
    config.setPersistentItems(persistentItems);
    config.setPublishModel(publishModel);
    return config;
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
    node.subscribe("okse"); //TODO, read JID from config
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
    SimplePayload payload = new SimplePayload(message.getTopic(), "pubsub:okse", message.getMessage()); //TODO read namespace from config
    return new PayloadItem<>(payload);
  }

  /**
   * Forwards a received message to the OKSE core to be distributed and updates the tracked information
   *
   * @param itemList, a list of {@link Item} objects passed on from the event listener
   * @param topic, the {@link Topic} of the node that sent the payload
   */
  public void onMessageReceived(List<Item> itemList, Topic topic) {
    log.debug("Received a message with topic: " + topic.getName());
    for (Item item: itemList) {
      if (item instanceof PayloadItem) {
        MessageService.getInstance().distributeMessage(payloadItemTOMessage((PayloadItem) item, topic));
        log.debug("Redistributed message with topic: " + topic.getName());
        protocolServer.incrementTotalMessagesReceived();
        protocolServer.incrementTotalRequests();

      }
    }
  }


}
