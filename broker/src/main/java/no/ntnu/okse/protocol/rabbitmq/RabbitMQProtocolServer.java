package no.ntnu.okse.protocol.rabbitmq;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

public class RabbitMQProtocolServer extends AbstractProtocolServer {

  protected static final String SERVERTYPE = "rabbitmq";
  private RabbitMQListenerClient client;


  /**
   * Constructor, sets the host and port to be used
   *
   * @param host the host to listen to
   * @param port the port to listen to
   */
  public RabbitMQProtocolServer(String host, Integer port, String exchange, List<String> topics) {
    log = Logger.getLogger(RabbitMQProtocolServer.class);
    log.info("Starting RabbitMQProtocolServer");
    this.host = host;
    this.port = port;
    protocolServerType = SERVERTYPE;
    try {
      client = new RabbitMQListenerClient(this, host, port, "OKSEListenerClient", exchange, topics);
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
      log.error("Could not start RabbitMQListenerClient properly");
    }
  }

  @Override
  public void boot() {
    if (!_running) {
      _running = true;
      _serverThread = new Thread(this::run);
      _serverThread.setName("RabbitMQListenerClient");
      _serverThread.start();
      log.info("RabbitMQ server booted successfully");
    }
  }

  @Override
  public void run() {}

  @Override
  public void stopServer() {
    log.info("Stopping RabbitMQListenerClient");
    try {
      client.stopClientListener();
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
      log.error("Failed to properly stop RabbitMQListenerClient");
      return;
    }
    _running = false;
    log.info("Stopping RabbitMQProtocolServer");

  }

  @Override
  public String getProtocolServerType() {
    return SERVERTYPE;
  }

  @Override
  public void sendMessage(Message message) {
    if (!message.getOriginProtocol().equals(protocolServerType)
        || message.getAttribute("duplicate") != null) {
      incrementTotalRequests();
      try {
        client.sendMessage(message);
        incrementTotalMessagesSent();
      } catch (IOException e) {
        e.printStackTrace();
        incrementTotalErrors();
      }
    }
  }
}


