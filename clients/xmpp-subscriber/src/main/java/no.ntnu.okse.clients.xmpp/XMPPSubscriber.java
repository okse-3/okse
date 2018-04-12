package no.ntnu.okse.clients.xmpp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;

public class XMPPSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public final int port = 5222;

  private XMPPClient client;

  public static void main(String[] args) {
    launch(new XMPPSubscriber(), args);
  }

  @Override
  protected void createClient() {
    client = new XMPPClient(host, port);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }
}
