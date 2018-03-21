package no.ntnu.okse;

import java.io.IOException;
import org.jivesoftware.openfire.XMPPServer;

public class OpenfireXMPPServerFactory {

  private static XMPPServer xmppServer;

  public static void startXMPPServer() {
    if (serverRunning()) {
      throw new IllegalStateException("XMPP server already running");
    }

    try {
      OpenfireXMPPServerDBManager.setupConfigFile();
    } catch (IOException e) {
      throw new IllegalStateException("Config file is not correct");
    }

    xmppServer = new XMPPServer();
    xmppServer.finishSetup();
    // Listen for clients on
    xmppServer.getConnectionManager().enableClientListener(true);
    // Everyone should be able to create nodes
    xmppServer.getPubSubModule().setNodeCreationRestricted(true);
  }

  public static void stopXMPPServer() {
    if (!serverRunning()) {
      throw new IllegalStateException("XMPP server is already stopped");
    }
    xmppServer.stop();
  }

  public static boolean serverRunning() {
    return xmppServer != null && xmppServer.isStarted() && !xmppServer.isShuttingDown();
  }

}
