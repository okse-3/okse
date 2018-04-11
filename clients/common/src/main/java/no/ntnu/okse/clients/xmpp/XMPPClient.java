package no.ntnu.okse.clients.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import no.ntnu.okse.clients.TestClient;
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
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPClient implements TestClient{

  private EntityBareJid jid;
  private String password;
  private String serverHost;
  private Integer serverPort;
  public int messageCounter;
  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;
  private ConcurrentHashMap<String, ItemEventListener> listenerMap;


  public XMPPClient(String host, Integer port) {
    try {
      this.jid = JidCreate.entityBareFrom(host);
    } catch (XmppStringprepException e) {
      e.printStackTrace();
    }
    password = "Password";
    listenerMap = new ConcurrentHashMap<>();
    serverHost = host;
    messageCounter = 0;
    serverPort = port;
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
    pubSubManager = PubSubManager.getInstance(connection);
    logInToHost();
  }

  private void createConnection() {
    this.connection = setUpConnection(serverHost, serverPort);
    try {
      connection.connect();
    } catch (SmackException | IOException | InterruptedException | XMPPException e) {
      e.printStackTrace();
    }
  }

  private void logInToHost() {
    AccountManager accountManager = AccountManager.getInstance(connection);
    accountManager.sensitiveOperationOverInsecureConnection(true);

    try {
      connection.login(jid.getLocalpart(), password);
    } catch (XMPPException e) {
      try {
        accountManager.createAccount(jid.getLocalpart(), password);
      } catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e1) {
        e1.printStackTrace();
      }
      createConnection();
      try {
        connection.login(jid.getLocalpart(), password);
      } catch (XMPPException | SmackException | InterruptedException | IOException e1) {
        e1.printStackTrace();
      }
    } catch (InterruptedException | IOException | SmackException e) {
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
      LeafNode node = pubSubManager.getOrCreateLeafNode(topic);
      node.addItemEventListener(new ItemEventListener() {
        @Override
        public void handlePublishedItems(ItemPublishEvent itemPublishEvent) {
          listenerMap.put(topic, this);
          messageCounter++;
        }
      });
      node.subscribe(connection.getUser().asEntityBareJidString());
    } catch (NoResponseException | XMPPErrorException | NotConnectedException | NotALeafNodeException | InterruptedException e) {
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

  @Override
  public void publish(String topic, String content) {
    SimplePayload payload = new SimplePayload(topic, serverHost + "/" + topic, content);
    try {
      pubSubManager.getLeafNode(topic).publish(new PayloadItem<>(payload));
    } catch (NotConnectedException | InterruptedException | NoResponseException | NotALeafNodeException | NotAPubSubNodeException | XMPPErrorException e) {
      e.printStackTrace();
    }
  }
}
