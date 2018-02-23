package no.ntnu.okse.protocol.amqp091;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import no.ntnu.okse.protocol.ProtocolServer;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AMQP protocol server
 */
public class AMQP091ProtocolServer extends AbstractProtocolServer {


  private final String SERVERTYPE = "amqp091";
  private AMQP091Service amqpService;
  private SubscriptionService subscriptionService;
  private static AtomicBoolean running = new AtomicBoolean(false);


  /**
   * Instantiate protocol server
   *
   * @param host hostname
   * @param port port
   */
  public AMQP091ProtocolServer(String host, int port) {
    this.port = port;
    this.host = host;
    log = Logger.getLogger(AMQP091ProtocolServer.class.getName());
    subscriptionService = SubscriptionService.getInstance();
  }

  /**
   * Boot protocol server
   */
  @Override
  public void boot() {
    if (running.compareAndSet(false, true)) {
      amqpService = new AMQP091Service(this);
      _serverThread = new Thread(this::run);
      _serverThread.setName("AMQ091ProtocolServer");
      _serverThread.start();
      log.info("AMQ091ProtocolServer booted successfully");
    } else {
      incrementTotalErrors();
      throw new ProtocolServer.BootErrorException(
          "Another AMQP 0.9.1 server is already running. Only one server can be running at the same time."
      );
    }
  }

  /**
   * Run server
   */
  @Override
  public void run() {
    log.debug(String.format("Starting AMQP 0.9.1 service on %s:%d", host, port));
    amqpService.start();
  }

  /**
   * Stop server
   */
  @Override
  public void stopServer() {
    if (running.compareAndSet(true, false)) {
      if (amqpService != null) {
        amqpService.stop();
      }
      amqpService = null;
    } else {
      log.error("Server was already stopped");
      incrementTotalErrors();
    }
  }

  /**
   * @return protocol server type as string
   */
  @Override
  public String getProtocolServerType() {
    return SERVERTYPE;
  }

  /**
   * @return server running
   */
  public boolean isRunning() {
    return running.get();
  }

  /**
   * Send message to subscribers
   *
   * @param message An instance of Message containing the required data to distribute a message.
   */
  @Override
  public void sendMessage(Message message) {
    amqpService.sendMessage(message);
    incrementMessageSentForTopic(message.getTopic());
  }

  /**
   * Increment messages sent for a specific topic
   *
   * @param topic topic
   */
  private void incrementMessageSentForTopic(String topic) {
    HashSet<Subscriber> allSubscribers = subscriptionService.getAllSubscribers();
    allSubscribers.stream()
        .filter(subscriber -> subscriber.getOriginProtocol().equals(getProtocolServerType()))
        .filter(subscriber -> subscriber.getTopic().equals(topic))
        .forEach(subscriber -> incrementTotalMessagesSent());
  }

  /**
   * Dependency setter injection for AMQP service
   *
   * @param amqpService AMQP 0.9.1 service
   */
  void setAmqpService(AMQP091Service amqpService) {
    this.amqpService = amqpService;
  }

  /**
   * Dependency setter injection for subscription service
   *
   * @param subscriptionService subscription service
   */
  void setSubscriptionService(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }
}
