package no.ntnu.okse.clients.stomp;

import asia.stampy.server.message.message.MessageMessage;
import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;

public class STOMPSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 61613;

  private StompClient client;

  public static void main(String[] args) {
    launch(new STOMPSubscriber(), args);
  }

  protected void createClient() {
    client = new StompClient(host, port);
    client.setCallback(new Callback(this));
  }

  protected TestClient getClient() {
    return client;
  }

  private static class Callback extends StompCallback {

    private SubscribeClient subscribeClient;

    public Callback(SubscribeClient subscribeClient) {
      this.subscribeClient = subscribeClient;
    }

    @Override
    public void messageReceived(MessageMessage message) {
      subscribeClient
          .receiveMessage(message.getHeader().getDestination(), message.getBody().toString(), true);
    }
  }
}
