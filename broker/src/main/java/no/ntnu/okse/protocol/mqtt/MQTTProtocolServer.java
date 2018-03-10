package no.ntnu.okse.protocol.mqtt;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

public class MQTTProtocolServer extends AbstractProtocolServer {

  protected static final String SERVERTYPE = "mqtt";

  private static final Logger log = Logger.getLogger(MQTTProtocolServer.class.getName());

  private MQTTServer server;

  /**
   * Constructor, sets the host and port to be used
   *
   * @param host the host to listen to
   * @param port the port to listen to
   */
  public MQTTProtocolServer(String host, Integer port) {
    this.host = host;
    this.port = port;
    protocolServerType = SERVERTYPE;
  }

  @Override
  public void boot() {
    if (!_running) {
      server = new MQTTServer(this, host, port);
      _serverThread = new Thread(this::run);
      _serverThread.setName("MQTTProtocolServer");
      _serverThread.start();
      _running = true;
      log.info("MQTTProtocolServer booted successfully");
    }
  }

  @Override
  public void run() {
    MQTTSubscriptionManager subscriptionManager = new MQTTSubscriptionManager();
    subscriptionManager.initCoreSubscriptionService(SubscriptionService.getInstance());
    SubscriptionService.getInstance().addSubscriptionChangeListener(subscriptionManager);
    server.start();
    server.setSubscriptionManager(subscriptionManager);
  }

  @Override
  public void stopServer() {
    log.info("Stopping MQTTProtocolServer");
    server.stopMessageThread();
    server.stopServer();
    _running = false;
    server = null;
    log.info("MQTTProtocolServer is stopped");
  }

  @Override
  public String getProtocolServerType() {
    return SERVERTYPE;
  }

  @Override
  public void sendMessage(Message message) {
    log.info("Received message on topic " + message.getMessage());
    server.queueMessage(message);

  }

  /**
   * Method for determining if the server is running
   */
  public boolean isRunning() {
    return _running;
  }
}
