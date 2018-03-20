package no.ntnu.okse;

import org.jivesoftware.openfire.XMPPServer;

public class OpenfireXMPPServerFactory {

  private static XMPPServer xmppServer;

  public static void startXMPPServer() {
    if (serverRunning()) {
      throw new IllegalStateException("XMPP server already running");
    }
    xmppServer = new XMPPServer();
    xmppServer.finishSetup();
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
