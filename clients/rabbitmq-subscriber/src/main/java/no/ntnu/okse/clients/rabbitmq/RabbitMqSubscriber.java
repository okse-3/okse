package no.ntnu.okse.clients.rabbitmq;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class RabbitMqSubscriber  extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 20001;

  @Parameter(names = {"--id", "-i"}, description = "clientID")
  public String id = "rabbitmqTestSubscriber";

  @Parameter(names = {"--exchange", "-e"}, description = "RabbitMq exchange")
  public String exchange = "amq.topic";

  private RabbitMqClient client;

  public static void main(String[] args) {
    launch(new RabbitMqSubscriber(), args);
  }

  @Override
  protected void createClient() {
    client = new RabbitMqClient(host, port, id, exchange);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }
}
