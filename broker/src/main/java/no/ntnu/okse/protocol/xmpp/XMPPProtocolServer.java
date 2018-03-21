package no.ntnu.okse.protocol.xmpp;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPProtocolServer extends AbstractProtocolServer {

  private String username, password;
  private EntityBareJid jid;
  private XMPPServer server;

  /**
   * Constructor for the XMPP Protocol Server. Creates JID as an {@link EntityBareJid},
   * which has a local part (before @-sign) and domain part (after @-sign).
   *
   * @param host the host IP-address, as a {@link String}
   * @param port the port number, as an {@link Integer}
   * @param jid the client JID, as a {@link String}
   * @param password the client password, as a {@link String}
   */
  public XMPPProtocolServer(String host, Integer port, String jid, String password)
      throws XmppStringprepException {
    this.port = port;
    this.host = host;
    this.jid = JidCreate.entityBareFrom(jid);
    this.username = this.jid.getLocalpart().toString();
    this.password = password;
    protocolServerType = "xmpp";
    log = Logger.getLogger(XMPPProtocolServer.class.getName());
  }

  /**
   * Sets up the {@link XMPPServer}, starts it and sets _running state to true
   */
  @Override
  public void boot() {
    if (!_running) {
      _running = true;

      server = new XMPPServer(this, host, port, username, password, jid);
      _serverThread = new Thread(this::run);
      _serverThread.setName(protocolServerType);
      _serverThread.start();
      log.info("XMPPProtocolServer booted successfully");
    }
  }

  @Override
  public void run() {
    //TODO
  }

  /**
   * Stops the XMPPProtocolServer and {@link XMPPServer}
   */
  @Override
  public void stopServer() {
    log.info("Stopping XMPPServer");
    server.stopServer();
    log.info("Stopping XMPPProtocolServer");
    _running = false;

  }

  /**
   * @return the server type as a {@link String}
   */
  @Override
  public String getProtocolServerType() {
    return protocolServerType;
  }

  /**
   * Forwards the message to the server to handle, given it does not originate from this protocol
   * @param message An instance of Message containing the required data to distribute a message.
   */
  @Override
  public void sendMessage(Message message) {
    if (!message.getOriginProtocol().equals(protocolServerType)
        || message.getAttribute("duplicate") != null) {
      incrementTotalRequests();
      try {
        server.sendMessage(message);
        incrementTotalMessagesSent();
      } catch (XMPPErrorException | NoResponseException | InterruptedException e) {
        incrementTotalErrors();
        e.printStackTrace();
      } catch (NotAPubSubNodeException | NotALeafNodeException | NotConnectedException e) {
        incrementTotalBadRequest();
        e.printStackTrace();
      }
    }
  }

  /**
   * @return true if ProtocolServer is in a running state, else false
   */
  public boolean isRunning() {
    return _running;
  }
}
