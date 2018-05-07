package no.ntnu.okse.clients.xmpp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;
import no.ntnu.okse.clients.xmpp.XMPPClient.Callback;

public class XMPPSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 5222;

  @Parameter(names = {"--jid"}, description = "JID")
  public String jid = "subscriber@localhost";

  @Parameter(names = {"--password", "--pass"}, description = "User password")
  public String password = "password";

  private XMPPClient client;

  public static void main(String[] args) {
    launch(new XMPPSubscriber(), args);
  }

  @Override
  protected void createClient() {
    client = new XMPPClient(host, port, jid, password);
    client.callback = new SubscriberCallback(this);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }

  private static class SubscriberCallback implements Callback {

    private SubscribeClient subscribeClient;

    public SubscriberCallback(SubscribeClient subscribeClient) {
      this.subscribeClient = subscribeClient;
    }

    @Override
    public void onMessageReceived(String topic, String message) {
      subscribeClient.receiveMessage(topic, message, true);
    }
  }
}
