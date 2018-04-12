package no.ntnu.okse.protocol.xmpp;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPProtocolServer extends AbstractProtocolServer {

  private EntityBareJid jid;
  private String password;
  private XMPPServer server;
  protected static final String SERVERTYPE = "xmpp";

  /**
   * Constructor for the XMPP Protocol Server. Creates JID as an {@link EntityBareJid},
   * which has a local part (before @-sign) and domain part (after @-sign).
   *
   * @param host the host IP-address, as a {@link String}
   * @param port the port number, as an {@link Integer}
   * @param jid the client JID, as a {@link String}
   * @param password the client password, as a {@link String}
   */
  public XMPPProtocolServer(String host, Integer port, String jid, String password) {
    log = Logger.getLogger(XMPPProtocolServer.class);
    this.port = port;
    this.host = host;
    try {
      this.jid = JidCreate.entityBareFrom(jid);
    } catch (XmppStringprepException e){
      log.error("EntityBareJid was malformed");
      e.printStackTrace();
    }
    this.password = password;
    protocolServerType = SERVERTYPE;
  }

  /**
   * Sets up the {@link XMPPServer}, starts it and sets _running state to true
   */
  @Override
  public void boot() {
    if (!_running) {
      _running = true;
      _serverThread = new Thread(this::run);
      _serverThread.setName("XMPPServer");
      _serverThread.start();
      log.info("XMPPProtocolServer booted successfully");
    }
  }

  @Override
  public void run() {
    server = new XMPPServer(this, host, port, jid, password);
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
      } catch (NotAPubSubNodeException | XMPPErrorException | NoResponseException | InterruptedException e) {
        incrementTotalErrors();
        e.printStackTrace();
      } catch (NotConnectedException e) {
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

  public void setServer(XMPPServer toSet){ this.server = toSet; }
  public XMPPServer getServer(){return this.server;}
}
