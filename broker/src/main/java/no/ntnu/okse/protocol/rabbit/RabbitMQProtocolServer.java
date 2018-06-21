package no.ntnu.okse.protocol.rabbit;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig.Builder;
import io.arivera.oss.embedded.rabbitmq.PredefinedVersion;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

public class RabbitMQProtocolServer extends AbstractProtocolServer {

  protected static final String SERVERTYPE = "rabbitmq_mqtt";
  private RabbitMQServer server;



  /**
   * Constructor, sets the host and port to be used
   *
   * @param host the host to listen to
   * @param port the port to listen to
   */
  public RabbitMQProtocolServer(String host, Integer port) {
    this.host = host;
    this.port = port;
    log = Logger.getLogger(RabbitMQProtocolServer.class);
    protocolServerType = SERVERTYPE;
  }

  @Override
  public void boot() {
    if (!_running) {
      _running = true;
      _serverThread = new Thread(this::run);
      _serverThread.setName("RabbitMQServer");
      _serverThread.start();
      log.info("RabbitMQ server booted successfully");
    }
  }

  @Override
  public void run() {
    EmbeddedRabbitMqConfig.Builder builder = new Builder();
    builder.version(PredefinedVersion.V3_6_9);
    EmbeddedRabbitMqConfig config = builder.build();
    RabbitMqPlugins mqttPlugin = new RabbitMqPlugins(config);
    mqttPlugin.enable("rabbitmq_mqtt");

    server = new RabbitMQServer(this, config);
  }

  @Override
  public void stopServer() {
    log.info("Stopping RabbitMQServer");
    server.stopServer();
    log.info("Stopping RabbitMQProtocolServer");
    _running = false;

  }

  @Override
  public String getProtocolServerType() {
    return SERVERTYPE;
  }

  @Override
  public void sendMessage(Message message) {

  }

}
