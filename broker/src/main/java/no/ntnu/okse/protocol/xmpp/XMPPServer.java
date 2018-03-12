package no.ntnu.okse.protocol.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.xdata.packet.DataForm.Type;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPServer implements SubscriptionChangeListener{

  private final Jid JID;
  private Logger log = Logger.getLogger(XMPPServer.class.getName());
  private final ConcurrentHashMap<String, PubSubListener<PayloadItem>> listenerMap = new ConcurrentHashMap<>();
  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;
  private ConfigureForm form;
  private XMPPProtocolServer protocolServer;


  /**
   * Init method for XMPPServers
   * @param protocolServer, the managing protocol server
   * @param host, host IP or domain as a {@link String}
   * @param port, host port number as a {@link Integer}
   */
  public XMPPServer(XMPPProtocolServer protocolServer, String host, Integer port) {

    try {
      JID = JidCreate.bareFrom("pubsub.okse.no"); // needs clarification with customer
    } catch (XmppStringprepException e) {
      e.printStackTrace();
      System.out.println("Error in JID string");
    }
    XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
    try {
      configBuilder.setHostAddress(InetAddress.getByName(host));
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("Error in host string");
    }
    configBuilder.setPort(port);
    try {
      configBuilder.setXmppDomain(JID); // needs clarification with customer
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }
    this.connection = new XMPPTCPConnection(configBuilder.build());
    try {
      connection.connect();
    } catch (SmackException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (XMPPException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      connection.login(user, pass); // needs clarification with customer
    } catch (XMPPException e) {
      e.printStackTrace();
    } catch (SmackException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    this.protocolServer = protocolServer;
    this.pubSubManager = PubSubManager.getInstance(connection); //add JID?
    form = new ConfigureForm(Type.submit);
    form.setAccessModel(AccessModel.open);
    form.setDeliverPayloads(true);
    form.setPersistentItems(true);
    form.setPublishModel(PublishModel.open);
    SubscriptionService.getInstance().addSubscriptionChangeListener(this);

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
   * standard: an open PublishModel, so that everyone can subscribe to the node; an open AccessModel,
   * so that everyone can subscribe to the node; not delivering payloads; and having persistent items.
   *
   * @param topic the ID of the created node as a {@link String}
   * @return a configured {@link LeafNode} with the given NodeID.
   * @throws XMPPErrorException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
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
   * @throws InterruptedException
   * @throws NotALeafNodeException
   * @throws NotAPubSubNodeException
   * @throws NotConnectedException
   * @throws NoResponseException
   * @throws XMPPErrorException
   */
  public LeafNode getLeafNode(String topic)
      throws InterruptedException, NotALeafNodeException, NotAPubSubNodeException, NotConnectedException, NoResponseException, XMPPErrorException {
    try {
      return pubSubManager.getLeafNode(topic);
    } catch (XMPPErrorException e) {
      return createLeafNode(topic);
    }

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
      InterruptedException, NoResponseException, NotALeafNodeException {
    LeafNode node = getLeafNode(message.getTopic());
    subscribeToNode(node);
    node.publish(messageToPayloadItem(message));
    log.debug("Distributed messages with topic: " + message.getTopic());
  }


  /**
   * Sets up a subscription to a node by adding an event listener
   *
   * @param node, the {@link LeafNode} to subscribe to
   * @throws XMPPErrorException
   * @throws NotAPubSubNodeException
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NoResponseException
   */
  public void subscribeToNode(LeafNode node)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    PubSubListener listener = new PubSubListener<PayloadItem>(this, node.getId());
    node.addItemEventListener(listener);
    node.subscribe(JID + "/" + node.getId()); // needs clarification with customer
    listenerMap.put(node.getId(), listener);
  }

  /**
   * Fetches and subscribes to a node with the given topic
   * @param topic, a topic/ID as a {@link String}
   * @throws NotConnectedException
   * @throws InterruptedException
   * @throws NotALeafNodeException
   * @throws NoResponseException
   * @throws NotAPubSubNodeException
   * @throws XMPPErrorException
   */
  public void subscribeToTopic(String topic)
      throws NotConnectedException, InterruptedException, NotALeafNodeException, NoResponseException, NotAPubSubNodeException, XMPPErrorException {
    subscribeToNode(getLeafNode(topic));
  }

  public void unsubscribeFromTopic(String topic)
      throws InterruptedException, NotALeafNodeException, XMPPErrorException, NotConnectedException, NoResponseException {
    unsubscribeFromNode(pubSubManager.getOrCreateLeafNode(topic));
  }

  public void unsubscribeFromNode(LeafNode node)
      throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
    node.unsubscribe(JID + "/" + node.getId()); // needs clarification with customer
    node.removeItemEventListener(listenerMap.get(node.getId()));
  }

  /**
   * Translates a {@link PayloadItem} to an OKSE {@link Message} Object
   *
   * @param pi, the {@link PayloadItem} to be translated
   * @param topic, the topic/ID of the item as a  {@link String}
   * @return a {@link Message} object containing the payload data, topic and protocol origin
   */
  private Message payloadItemToMessage(PayloadItem pi, String topic) {
    return new Message(pi.getPayload().toString(), topic, null, protocolServer.getProtocolServerType());
  }

  /**
   * Translates a {@link Message} object to a {@link SimplePayload} object that can be sent as a {@link PayloadItem}
   *
   * @param message, the {@link Message} to be sent
   * @return a {@link PayloadItem} containing the message
   */
  private PayloadItem<SimplePayload> messageToPayloadItem(Message message) {
    SimplePayload payload = new SimplePayload(message.getTopic(), JID + "/" + message.getTopic(), message.getMessage());
    return new PayloadItem<>(payload);
  }

  /**
   * Forwards a received message to the OKSE core to be distributed and updates the tracked information
   *
   * @param itemList, a list of {@link Item} objects passed on from the event listener
   * @param topic, the {@link String} of the node that sent the payload
   */
  public void onMessageReceived(List<Item> itemList, String topic) {
    log.debug("Received a message with topic: " + topic);
    for (Item item: itemList) {
      if (item instanceof PayloadItem) {
        MessageService.getInstance().distributeMessage(
            payloadItemToMessage((PayloadItem) item, topic));
        log.debug("Redistributed message with topic: " + topic);
        protocolServer.incrementTotalMessagesReceived();
        protocolServer.incrementTotalRequests();

      }
    }
  }

  @Override
  public void subscriptionChanged(SubscriptionChangeEvent e) {
    if(e.getData().getOriginProtocol().equals(XMPPProtocolServer.SERVERTYPE)) {
      if(e.getType() == SubscriptionChangeEvent.Type.UNSUBSCRIBE) {
        try {
          unsubscribeFromTopic(e.getData().getTopic());
        } catch (InterruptedException | NotALeafNodeException | XMPPErrorException | NoResponseException | NotConnectedException e1) {
          e1.printStackTrace();
        }
        //TODO logging and docs
      }
    }
  }
}
