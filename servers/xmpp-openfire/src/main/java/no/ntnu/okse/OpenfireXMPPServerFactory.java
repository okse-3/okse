package no.ntnu.okse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.openfire.XMPPServer;

public class OpenfireXMPPServerFactory {

  private static XMPPServer xmppServer;
  private static Process p;

  /**
   * Start a process with the Openfire XMPP Server
   *
   * @throws IOException Thrown if process cannot be opened
   */
  public static void start() throws IOException {
    if (p != null) {
      throw new IllegalStateException("Already started a process");
    }
    ProcessBuilder proc = new ProcessBuilder("java", "-cp",
        System.getProperty("java.class.path", "."), OpenfireXMPPServerFactory.class.getName());
    p = proc.inheritIO().start();
    Logger.getLogger(OpenfireXMPPServerFactory.class.getName())
        .log(Level.INFO, "Started Openfire XMPP server");
  }

  /**
   * Stops the process with the Openfire XMPP Server
   */
  public static void stop() {
    if (p == null) {
      throw new IllegalStateException("No running process");
    }
    p.destroy();
    p = null;
    Logger.getLogger(OpenfireXMPPServerFactory.class.getName())
        .log(Level.INFO, "Stopped Openfire XMPP Server");
  }

  /**
   * Checks if the Openfire XMPP Server is running
   * @return
   */
  public static boolean isRunning() {
    return p != null && p.isAlive();
  }

  /**
   * Starts the Openfire XMPP server
   */
  private static void startXMPPServer() {
    if (serverRunning()) {
      throw new IllegalStateException("XMPP server already running");
    }

    try {
      OpenfireXMPPServerDBManager.setupConfigFile();
    } catch (IOException e) {
      e.printStackTrace();
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
  private static void stopXMPPServer() {
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
    Runtime.getRuntime().addShutdownHook(new Thread(OpenfireXMPPServerFactory::stopXMPPServer));
  }

}
