package no.ntnu.okse.protocol.rabbit;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqServer;
import no.ntnu.okse.core.messaging.Message;

public class RabbitMQServer extends RabbitMqServer {

  RabbitMQProtocolServer ps;

  public RabbitMQServer(RabbitMQProtocolServer ps, EmbeddedRabbitMqConfig config) {
    super(config);
    this.ps = ps;
  }

  public void boot() {
    this.start();

  }

  public void stopServer() {
  }

  public void sendMessage(Message message) {

  }

}
