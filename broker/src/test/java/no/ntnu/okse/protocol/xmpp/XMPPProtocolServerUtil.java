package no.ntnu.okse.protocol.xmpp;

import java.io.IOException;

public class XMPPProtocolServerUtil {

  private static Process p;
  private static XMPPProtocolServer ps;
  //@Mock(name = "server")
  private static XMPPServer server;

  /**
   * Start the process
   *
   * @throws IOException Thrown if process cannot be opened
   */
  public static void start() throws IOException {
    if (p != null) {
      throw new IllegalStateException("Already started a process");
    }
    ProcessBuilder proc = new ProcessBuilder("java", "-cp",
        System.getProperty("java.class.path", "."), XMPPProtocolServerUtil.class.getName());
    p = proc.inheritIO().start();
  }

  /**
   * Stops the process
   */
  public static void stop() {
    if (p == null) {
      throw new IllegalStateException("No running process");
    }
    p.destroy();
    p = null;
  }

  /**
   * Checks if the process is running
   */
  public static boolean isRunning() {
    return p != null && p.isAlive();
  }

  /**
   * Starts the XMPP server
   */
  private static void startXMPPServer() {
    if (serverRunning()) {
      throw new IllegalStateException("XMPP server already running");
    }
    ps = new XMPPProtocolServer("localhost", 5222, "okse@localhost", "pass");
    ps.boot();
    server = ps.getServer();
  }

  /**
   * Stops the the XMPP server
   */
  private static void stopXMPPServer() {
    if (!serverRunning()) {
      throw new IllegalStateException("XMPP server is already stopped");
    }

    ps.stopServer();
  }

  /**
   * Checks if the protocol server is running
   *
   * @return A boolean indicating if the XMPP server is running
   */
  public static boolean serverRunning() {
    return ps != null && ps.isRunning();
  }

  public static void main(String[] args) {
    startXMPPServer();
    Runtime.getRuntime().addShutdownHook(new Thread(XMPPProtocolServerUtil::stopXMPPServer));
  }

}
