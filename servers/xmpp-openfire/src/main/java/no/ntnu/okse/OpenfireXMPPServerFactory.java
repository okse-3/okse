package no.ntnu.okse;

import java.io.IOException;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;

public class OpenfireXMPPServerFactory {

  private static XMPPServer xmppServer;

  /**
   * Starts the Openfire XMPP server
   */
  public static void startXMPPServer() {
    if (serverRunning()) {
      throw new IllegalStateException("XMPP server already running");
    }

    try {
      OpenfireXMPPServerDBManager.setupConfigFile();
    } catch (IOException e) {
      throw new IllegalStateException("Config file is not correct");
    }

    // Start with clean database
    OpenfireXMPPServerDBManager.deleteDatabase();

    xmppServer = new XMPPServer();
    xmppServer.finishSetup();

    // Listen for clients on
    xmppServer.getConnectionManager().enableClientListener(true);
    // Everyone should be able to create nodes
    xmppServer.getPubSubModule().setNodeCreationRestricted(false);
  }

  /**
   * Stops the Openfire XMPP server
   */
  public static void stopXMPPServer() {
    if (!serverRunning()) {
      throw new IllegalStateException("XMPP server is already stopped");
    }
    xmppServer.stop();
  }

  /**
   * Checks if the Openfire XMPP server is running
   *
   * @return A boolean indicating if the XMPP server is running
   */
  public static boolean serverRunning() {
    return xmppServer != null && xmppServer.isStarted() && !xmppServer.isShuttingDown();
  }

  public static void main(String[] args) {
    startXMPPServer();
  }

}
