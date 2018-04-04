package no.ntnu.okse.protocol.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.xdata.packet.DataForm.Type;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPServer implements SubscriptionChangeListener {

  private String host;
  private Integer port;
  private EntityBareJid jid;
  private String password;
  private static ConfigureForm form = createNodeForm();
  private XMPPProtocolServer protocolServer;
  private PubSubManager pubSubManager;
  private AbstractXMPPConnection connection;

  private Logger log = Logger.getLogger(XMPPServer.class);


  /**
   * Init method for XMPPServers
   *
   * @param protocolServer, the managing protocol server
   * @param host, host IP or domain as a {@link String}
   * @param port, host port number as a {@link Integer}
   * @param password, client password, as a {@link String}
   * @param jid, client JID, as a {@link EntityBareJid}
   */
  public XMPPServer(XMPPProtocolServer protocolServer, String host, Integer port, EntityBareJid jid,
      String password) {

    this.host = host;
    this.port = port;
    this.jid = jid;
    this.password = password;
    this.protocolServer = protocolServer;

    createConnection();
    logInToHost();

    try {
      this.pubSubManager = PubSubManager.getInstance(connection, JidCreate.domainBareFrom("pubsub." + host));
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }

    SubscriptionService.getInstance().addSubscriptionChangeListener(this);
  }

  /**
   * Logs in to the host
   */
  private void logInToHost() {
    AccountManager accountManager = AccountManager.getInstance(connection);
    accountManager.sensitiveOperationOverInsecureConnection(true);

    try {
      connection.login(jid.getLocalpart(), password);
      log.info("Logged in successfully.");
    } catch (XMPPException e) {
      log.error("Attempting to create user.");
      try {
        accountManager.createAccount(jid.getLocalpart(), password);
      } catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e1) {
        log.error("Could not create account.");
        e1.printStackTrace();
      }
      // Must create new connection for Openfire to work correctly
      createConnection();
      try {
        connection.login(jid.getLocalpart(), password);
      } catch (XMPPException | SmackException | InterruptedException | IOException e1) {
        log.error("Could not connect.");
        e1.printStackTrace();
      }
    } catch (InterruptedException | IOException | SmackException e) {
      log.error("Could not connect.");
      e.printStackTrace();
    }
  }

  /**
   * Create a connection to the server
   */
  private void createConnection() {
    this.connection = setUpConnection(host, port);

    try {
      connection.connect();
      log.info("XMPP TCP connection established.");
    } catch (SmackException | IOException | InterruptedException | XMPPException e) {
      log.error("Could not connect to xmpp server");
      e.printStackTrace();
    }
  }

  /**
   * Sets up the {@link ConfigureForm} used when creating a new {@link LeafNode}
   * @return the configure form
   */
  private static ConfigureForm createNodeForm() {
    ConfigureForm form = new ConfigureForm(Type.submit);
    form.setAccessModel(AccessModel.open);
    form.setDeliverPayloads(true);
    form.setPersistentItems(true);
    form.setPublishModel(PublishModel.open);
    form.setNodeType(NodeType.leaf);
    return form;
  }

  /**
   * Sets up the XMPP connection over TCP.
   * @param host, the host address to connect to as a {@link String} (needs to be parsable by {@link InetAddress#getByName})
   * @param port, the port number the service is running on as a {@link Integer}
   * @return A {@link XMPPTCPConnection}
   */
  private XMPPTCPConnection setUpConnection(String host, Integer port) {
    XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
    try {
      configBuilder.setHostAddress(InetAddress.getByName(host));
      configBuilder.setHost(host);
    } catch (UnknownHostException e) {
      log.error("Host string is not valid.");
      e.printStackTrace();
    }
    configBuilder.setPort(port);
    configBuilder.setConnectTimeout(30000);
    configBuilder.setSecurityMode(SecurityMode.disabled);
    configBuilder.setUsernameAndPassword(jid.getLocalpart(), password);
    try {
      configBuilder.setXmppDomain(host);
    } catch (XmppStringprepException e) {
      log.error("Could not set domain host.");
      e.printStackTrace();
    }
    XMPPTCPConnectionConfiguration config = configBuilder.build();

    return new XMPPTCPConnection(config);
  }


  /**
   * Disconnects ongoing connections and shuts down the server
   */
  public void stopServer() {
    connection.disconnect();
    log.info("XMPPServer closed");
  }


  /**
   * Create a {@link LeafNode} with a topic/ID. The node will be configured with what we set to be
   * standard: an open PublishModel, so that everyone can subscribe to the node; an open
   * AccessModel, so that everyone can subscribe to the node; not delivering payloads; and having
   * persistent items.
   *
   * @param topic the ID of the created node as a {@link String}
   * @return a configured {@link LeafNode} with the given NodeID.
   */
  private LeafNode createLeafNode(String topic)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    log.debug("Created a new node with id " + topic);
    return (LeafNode) pubSubManager.createNode(topic, form);
  }

  /**
   * Fetches a {@link LeafNode} of the matching topic, will create one if not found
   *
   * @param topic, the topic/ID of the node as a {@link String}
   * @return a {@link LeafNode} of the matching topic
   */
  public LeafNode getLeafNode(String topic)
      throws InterruptedException, NotAPubSubNodeException, NotConnectedException, NoResponseException, XMPPErrorException {
    try {
      return pubSubManager.getNode(topic);
    } catch (XMPPErrorException e) {
      log.debug("Node not found, creating new.");
      return createLeafNode(topic);
    }

  }


  /**
   * Sends the Message to a node with the messages topic
   *
   * @param message, {@link Message} to be sent
   */
  public void sendMessage(Message message) throws XMPPErrorException, NotConnectedException,
      InterruptedException, NoResponseException, NotAPubSubNodeException {
    LeafNode node = getLeafNode(message.getTopic());
    subscribeToNode(node);
    node.publish(messageToPayloadItem(message));
    log.debug("Distributed messages with topic: " + message.getTopic());
  }


  /**
   * Sets up a subscription to a node by adding an event listener
   *
   * @param node, the {@link LeafNode} to subscribe to
   */
  public void subscribeToNode(LeafNode node)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    log.debug(String.format("Subscribing to %s ", node.getId()));
    PubSubListener listener = new PubSubListener<PayloadItem>(this, node.getId());
    node.addItemEventListener(listener);
    node.subscribe(connection.getUser().asEntityBareJidString());
    log.debug(String.format("Successfully subscribed to %s ", node.getId()));
  }

  /**
   * Fetches and subscribes to a node with the given topic
   *
   * @param topic, a topic/ID as a {@link String}
   */
  public void subscribeToTopic(String topic)
      throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException, NotALeafNodeException {
    subscribeToNode(pubSubManager.getOrCreateLeafNode(topic));
  }

  /**
   * Removes a subscription on the given topic
   * @param topic, the topic id as a {@link String}
   */
  public void unsubscribeFromTopic(String topic)
      throws InterruptedException, NotALeafNodeException, XMPPErrorException, NotConnectedException, NoResponseException, NotAPubSubNodeException {
    unsubscribeFromNode(pubSubManager.getLeafNode(topic));
  }

  /**
   * Removes a subscription on the given topic node
   * @param node, the topic node as a {@link LeafNode}
   */
  public void unsubscribeFromNode(LeafNode node)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    log.debug(String.format("Unsubscribing from %s ", node.getId()));
    node.unsubscribe(connection.getUser().asEntityBareJidString());
    log.debug(String.format("Successfully unsubscribed from %s ", node.getId()));
  }

  /**
   * Translates a {@link PayloadItem} to an OKSE {@link Message} Object
   *
   * @param pi, the {@link PayloadItem} to be translated
   * @param topic, the topic/ID of the item as a  {@link String}
   * @return a {@link Message} object containing the payload data, topic and protocol origin
   */
  private Message payloadItemToMessage(PayloadItem pi, String topic) {
    return new Message(pi.getPayload().toString(), topic, null,
        protocolServer.getProtocolServerType());
  }

  /**
   * Translates a {@link Message} object to a {@link SimplePayload} object that can be sent as a
   * {@link PayloadItem}
   *
   * @param message, the {@link Message} to be sent
   * @return a {@link PayloadItem} containing the message
   */
  private PayloadItem<SimplePayload> messageToPayloadItem(Message message) {
    SimplePayload payload = new SimplePayload(message.getTopic(),
        host + "/" + message.getTopic(), message.getMessage());
    return new PayloadItem<>(payload);
  }

  /**
   * Forwards a received message to the OKSE core to be distributed and updates the tracked
   * information
   *
   * @param itemList, a list of {@link Item} objects passed on from the event listener
   * @param topic, the {@link String} of the node that sent the payload
   */
  public void onMessageReceived(List<Item> itemList, String topic) {
    log.debug("Received a message with topic: " + topic);
    for (Item item : itemList) {
      if (item instanceof PayloadItem) {
        MessageService.getInstance().distributeMessage(
            payloadItemToMessage((PayloadItem) item, topic));
        log.debug("Redistributed message with topic: " + topic);
        protocolServer.incrementTotalMessagesReceived();
        protocolServer.incrementTotalRequests();

      }
    }
  }

  /**
   * Delegates subscription events received from other protocols to the corresponding methods
   * @param e, the {@link SubscriptionChangeEvent} to handle
   */
  @Override
  public void subscriptionChanged(SubscriptionChangeEvent e) {
    if (e.getData().getOriginProtocol().equals(XMPPProtocolServer.SERVERTYPE)) {
      if (e.getType() == SubscriptionChangeEvent.Type.UNSUBSCRIBE) {
        try {
          log.debug("Unsubscribe event received");
          unsubscribeFromTopic(e.getData().getTopic());
        } catch (InterruptedException | NotALeafNodeException | XMPPErrorException | NoResponseException | NotConnectedException | NotAPubSubNodeException e1) {
          e1.printStackTrace();
        }
        //TODO logging and docs
      }
    }
  }
}
