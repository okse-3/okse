package no.ntnu.okse.clients.amqp091;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;

public class AMQP091Subscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 56720;

  private AMQP091Client client;

  public static void main(String[] args) {
    launch(new AMQP091Subscriber(), args);
  }

  protected void createClient() {
    client = new AMQP091Client(host, port);
    client.setCallback(new SubscriberCallback(this));
  }

  protected TestClient getClient() {
    return client;
  }

  private class SubscriberCallback implements AMQP091Callback {

    private SubscribeClient subscribeClient;

    public SubscriberCallback(SubscribeClient subscribeClient) {
      this.subscribeClient = subscribeClient;
    }

    public void messageReceived(String topic, String message) {
      subscribeClient.receiveMessage(topic, message, true);
    }
  }
}
