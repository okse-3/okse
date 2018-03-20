package no.ntnu.okse;

import org.jivesoftware.openfire.XMPPServer;

public class OpenfireXMPPServerFactory {

  private static XMPPServer xmppServer;

  public static void startXMPPServer() {
    if (xmppServer != null && xmppServer.isStarted() && !xmppServer.isShuttingDown()) {
      throw new IllegalStateException("XMPP server already running");
    }
    xmppServer = new XMPPServer();
    xmppServer.finishSetup();
  }

  public static void stopXMPPServer() {
    if (xmppServer == null || xmppServer.isShuttingDown()) {
      throw new IllegalStateException("XMPP server is already stopped");
    }
    xmppServer.stop();
  }

}
