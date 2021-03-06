package no.ntnu.okse.protocol.xmpp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
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
import org.jivesoftware.smackx.pubsub.CollectionNode;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.SubscribeForm;
import org.jivesoftware.smackx.pubsub.SubscribeOptionFields;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm.Type;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPServer {

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
    protocolServer.setServer(this);

    createConnection();
    logInToHost();

    try {
      this.pubSubManager = PubSubManager.getInstance(connection,
          JidCreate.domainBareFrom("pubsub." + host));
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }

    try {
      CollectionNode root = pubSubManager.getNode("");
      SubscribeForm subForm = new SubscribeForm(Type.submit);
      subForm.setDeliverOn(true);
      subForm.setDigestOn(true);
      subForm.setDigestFrequency(5000);
      subForm.setIncludeBody(true);
      FormField formField = new FormField(SubscribeOptionFields.subscription_depth.getFieldName());
      formField.setType(FormField.Type.list_single);
      formField.addValue("all");
      subForm.addField(formField);
      formField = new FormField(SubscribeOptionFields.subscription_type.getFieldName());
      formField.setType(FormField.Type.list_single);
      formField.addValue("items");
      subForm.addField(formField);
      root.addItemEventListener(this::onMessageReceived);
      root.subscribe(connection.getUser().asEntityBareJidString(), subForm);
    } catch (NoResponseException | XMPPErrorException | InterruptedException | NotConnectedException | NotAPubSubNodeException e) {
      log.error("Could not subscribe to root node.");
      e.printStackTrace();
    }
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
      log.info("Attempting to create user.");
      try {
        accountManager.createAccount(jid.getLocalpart(), password);
      } catch (NoResponseException | XMPPErrorException | NotConnectedException
          | InterruptedException e1) {
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
   *
   * @return the configure form
   */
  private static ConfigureForm createNodeForm() {
    ConfigureForm form = new ConfigureForm(Type.submit);
    form.setAccessModel(AccessModel.open);
    form.setDeliverPayloads(true);
    form.setPersistentItems(false);
    form.setPublishModel(PublishModel.open);
    form.setNodeType(NodeType.leaf);
    return form;
  }

  /**
   * Sets up the XMPP connection over TCP.
   *
   * @param host, the host address to connect to as a {@link String} (needs to be parsable by {@link
   * InetAddress#getByName})
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
   * standard: an open PublishModel, so that everyone can publish to the node; an open AccessModel,
   * so that everyone can subscribe to the node; not delivering payloads; and having persistent
   * items.
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
      throws InterruptedException, NotAPubSubNodeException, NotConnectedException,
      NoResponseException, XMPPErrorException {
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
  public void sendMessage(Message message) throws NotConnectedException,
      InterruptedException, NoResponseException, NotAPubSubNodeException {
    LeafNode node = null;
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        node = getLeafNode(message.getTopic());
      } catch (XMPPErrorException error) {
        log.debug(String.format(
            "Node %s is probably already defined by another thread, trying %d more attempts",
            message.getTopic(), 3 - attempt));
      }
    }
    if (node == null) {
      log.error(
          "Could not get node for topic %s. The server is probably wrongly configured or connection has been lost");
      protocolServer.incrementTotalErrors();
      return;
    }
    node.publish(messageToPayloadItem(message));
    log.debug("Distributed messages with topic: " + message.getTopic());
  }

  /**
   * Translates a {@link PayloadItem} to an OKSE {@link Message} Object
   *
   * @param pi, the {@link PayloadItem} to be translated
   * @param topic, the topic/ID of the item as a  {@link String}
   * @return a {@link Message} object containing the payload data, topic and protocol origin
   */
  private Message payloadItemToMessage(PayloadItem pi, String topic) {
    try {
      String content = new SAXBuilder()
          .build(new ByteArrayInputStream(pi.getPayload().toXML().toString().getBytes("UTF-8")))
          .getRootElement().getValue();
      return new Message(content, topic, null, protocolServer.getProtocolServerType());
    } catch (JDOMException | IOException e) {
      log.info("Could not get message content");
    }
    return null;
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
        host + "/" + message.getTopic(),
        String.format("<body from='%s'>%s</body>", jid.toString(), message.getMessage()));
    return new PayloadItem<>(generateItemID(), payload);
  }

  /**
   * Generates a pseudo random unique id for use in PayloadItems. Use current time to generate
   * unique ids, with a random number to separate ids within a single millisecond
   *
   * @return Pseudo random id
   */
  private String generateItemID() {
    return String.format("id*%f*%d", Math.random(), System.currentTimeMillis());
  }

  /**
   * Forwards a received message to the OKSE core to be distributed and updates the tracked
   * information
   *
   * @param event, the publish event containing messages
   */
  private void onMessageReceived(ItemPublishEvent event) {
    List<Item> itemList = event.getItems();
    for (Item item : itemList) {
      if (item instanceof PayloadItem && !isOwner((PayloadItem) item)) {
        PayloadItem message = (PayloadItem) item;
        String topic = message.getNode();
        log.debug("Received a message with topic: " + topic);
        Message systemMessage = payloadItemToMessage(message, topic);
        if (systemMessage != null) {
          MessageService.getInstance().distributeMessage(
              payloadItemToMessage(message, topic));
          log.debug("Redistributed message with topic: " + topic);
          protocolServer.incrementTotalMessagesReceived();
          protocolServer.incrementTotalRequests();
        } else {
          log.warn("Received message with malformed content");
          protocolServer.incrementTotalBadRequest();
        }
      }
    }
  }

  /**
   * Checks if the server owns the item
   *
   * @param item The item to check
   * @return A boolean indicating if the server owns the item
   */
  private boolean isOwner(PayloadItem item) {
    SAXBuilder saxBuilder = new SAXBuilder();
    try {
      Document document = saxBuilder
          .build(new ByteArrayInputStream(item.getPayload().toXML().toString().getBytes("UTF-8")));
      return document.getRootElement().getAttribute("from").getValue()
          .equals(jid.toString());
    } catch (JDOMException | IOException | NullPointerException e) {
      return false;
    }
  }

}
