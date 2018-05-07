package no.ntnu.okse.clients.xmpp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.clients.TestClient;
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
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.xdata.packet.DataForm.Type;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPClient implements TestClient {

  private EntityBareJid jid;
  private String password = "Password";
  private String serverHost;
  private Integer serverPort;
  public int messageCounter;
  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;
  private ConcurrentHashMap<String, ItemEventListener> listenerMap;
  protected Callback callback;

  public XMPPClient(String host, Integer port, String jid, String password) {
    try {
      this.jid = JidCreate.entityBareFrom(jid);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }
    listenerMap = new ConcurrentHashMap<>();
    serverHost = host;
    serverPort = port;
    this.password = password;
  }

  public XMPPClient(String host, Integer port) {
    this(host, port, "testclient@127.0.0.1", "password");
  }

  private XMPPTCPConnection setUpConnection(String host, Integer port) {
    XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
    try {
      configBuilder.setHostAddress(InetAddress.getByName(host));
      configBuilder.setHost(host);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    configBuilder.setPort(port);
    configBuilder.setConnectTimeout(30000);
    configBuilder.setSecurityMode(SecurityMode.disabled);
    configBuilder.setUsernameAndPassword(jid.getLocalpart(), password);
    try {
      configBuilder.setXmppDomain(host);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }
    XMPPTCPConnectionConfiguration config = configBuilder.build();

    return new XMPPTCPConnection(config);
  }

  @Override
  public void connect() {
    createConnection();
    try {
      pubSubManager = PubSubManager
          .getInstance(connection, JidCreate.domainBareFrom("pubsub." + serverHost));
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }
    try {
      logInToHost();
    } catch (SmackException | InterruptedException | XMPPException | IOException e) {
      e.printStackTrace();
    }
  }

  private void createConnection() {
    this.connection = setUpConnection(serverHost, serverPort);
    try {
      connection.connect();
    } catch (SmackException | IOException | InterruptedException | XMPPException e) {
      e.printStackTrace();
    }
  }

  private void logInToHost()
      throws SmackException, InterruptedException, IOException, XMPPException {
    AccountManager accountManager = AccountManager.getInstance(connection);
    accountManager.sensitiveOperationOverInsecureConnection(true);
    try {
      accountManager.createAccount(jid.getLocalpart(), password);
    } catch (XMPPErrorException e) {
    }
    connection.login(jid.getLocalpart(), password);
  }

  @Override
  public void disconnect() {
    connection.disconnect();
  }

  @Override
  public void subscribe(String topic) {
    try {
      LeafNode node;
      try {
        node = pubSubManager.getLeafNode(topic);
      } catch (NotAPubSubNodeException | InterruptedException | XMPPErrorException | NotALeafNodeException | NotConnectedException | NoResponseException e) {
        node = createNode(topic);
        if (node == null) {
          return;
        }
      }
      ItemEventListener itemEventListener = itemPublishEvent -> {
        for (Object item : itemPublishEvent.getItems()) {
          if (item instanceof PayloadItem) {
            try {
              String content = new SAXBuilder()
                  .build(new ByteArrayInputStream(((PayloadItem) item).getPayload().toXML().toString().getBytes("UTF-8")))
                  .getRootElement().getValue();
              callback.onMessageReceived(topic, content);
              messageCounter++;
            } catch (JDOMException | IOException e) {
              e.printStackTrace();
            }
          }
        }
      };
      listenerMap.put(topic, itemEventListener);
      node.addItemEventListener(itemEventListener);
      node.subscribe(connection.getUser().asEntityBareJidString());
    } catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void unsubscribe(String topic) {
    try {
      LeafNode node = pubSubManager.getLeafNode(topic);
      node.unsubscribe(connection.getUser().asEntityBareJidString());
      node.removeItemEventListener(listenerMap.get(topic));
      listenerMap.remove(topic);
    } catch (NoResponseException | XMPPErrorException | InterruptedException | NotConnectedException | NotALeafNodeException | NotAPubSubNodeException e) {
      e.printStackTrace();
    }

  }

  private LeafNode createNode(String topic) {
    ConfigureForm form = new ConfigureForm(Type.submit);
    form.setAccessModel(AccessModel.open);
    form.setDeliverPayloads(true);
    form.setPersistentItems(false);
    form.setPublishModel(PublishModel.open);
    form.setNodeType(NodeType.leaf);
    try {
      return (LeafNode) pubSubManager.createNode(topic, form);
    } catch (NoResponseException | XMPPErrorException | InterruptedException | NotConnectedException e1) {
      e1.printStackTrace();
      return null;
    }
  }

  @Override
  public void publish(String topic, String content) {
    SimplePayload payload = new SimplePayload(topic, serverHost + "/" + topic,
        String.format("<body>%s</body>", content));
    LeafNode node;
    try {
      node = pubSubManager.getLeafNode(topic);
    } catch (NotALeafNodeException | NoResponseException | InterruptedException | NotConnectedException | NotAPubSubNodeException | XMPPErrorException e) {
      node = createNode(topic);
      if (node == null) {
        return;
      }
    }
    try {
      node.publish(new PayloadItem<>(generateItemID(), payload));
    } catch (NotConnectedException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  private String generateItemID() {
    return String.format("id*%f*%d", Math.random(), System.currentTimeMillis());
  }

  protected interface Callback {

    void onMessageReceived(String topic, String message);

  }
}
