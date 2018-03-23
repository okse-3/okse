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

    xmppServer = new XMPPServer();
    xmppServer.finishSetup();

    // If the domain is not the same in the database and in the config, reset database
    if (!isCorrectDomain()) {
      OpenfireXMPPServerDBManager.deleteDatabase();
      startXMPPServer();
    } else {
      // Listen for clients on
      xmppServer.getConnectionManager().enableClientListener(true);
      // Everyone should be able to create nodes
      xmppServer.getPubSubModule().setNodeCreationRestricted(true);
    }
  }

  /**
   * Checks if the domain in the database is the same as the one in the config file
   *
   * @return A boolean indicating if the domain is the same
   */
  private static boolean isCorrectDomain() {
    if (!serverRunning()) {
      throw new IllegalStateException("XMPP server is not running");
    }
    String domain = xmppServer.getPubSubModule().getAddress().getDomain();
    return JiveGlobals.getProperty("xmpp.domain").equals(domain.substring(7));
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

}
