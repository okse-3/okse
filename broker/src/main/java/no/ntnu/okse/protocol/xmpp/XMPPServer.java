package no.ntnu.okse.protocol.xmpp;

import java.io.IOException;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.web.Server;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public class XMPPServer {

  private AbstractXMPPConnection connection;
  private PubSubManager pubSubManager;


  public XMPPServer(String host, Integer port) {
    try {
      XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
      //configBuilder.setUsernameAndPassword("okse", "hunter2"); //TODO password??? is this needed?
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
  }

  public void sendMessage(Message message) {

  }

  public void onMessageReceived(Message message) {

  }

  //sub and node configuration are to be implemented here

}
