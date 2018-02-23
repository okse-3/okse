package no.ntnu.okse.protocol.xmpp;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

public class XMPPProtocolServer extends AbstractProtocolServer {

  private static final String SERVERTYPE = "xmpp";

  private XMPPServer server;

  private static Logger log = Logger.getLogger(XMPPProtocolServer.class.getName());

  /**
   * Constructor for the XMPP protocol server
   * @param host, host ip as a {@link String}
   * @param port, port number as a {@link Integer}
   */
  public XMPPProtocolServer(String host, Integer port) {
    this.port = port;
    this.host = host;
    this.protocolServerType = SERVERTYPE;
  }

  @Override
  public void boot() {
    if (!_running) {
      server = new XMPPServer(host, port);
      _serverThread = new Thread(this::run);
      _serverThread.setName("XMPPProtocolServer");
      _serverThread.start();
      _running = true;
      log.info("XMPPProtocolServer booted successfully");
    }
  }

  @Override
  public void run() {

  }

  @Override
  public void stopServer() {
    log.info("Stopping XMPPProtocolServer");
    server.stopServer();

  }

  @Override
  public String getProtocolServerType() {
    return SERVERTYPE;
  }

  @Override
  public void sendMessage(Message message) {
    server.sendMessage(message);
  }

  public boolean isRunning() {
    return _running;
  }
}
