package no.ntnu.okse.clients.amqp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class AMQPPublisher extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 5672;

  private AMQPClient client;

  public static void main(String[] args) {
    launch(new AMQPPublisher(), args);
  }

  protected void createClient() {
    client = new AMQPClient(host, port);
  }

  protected TestClient getClient() {
    return client;
  }
}
