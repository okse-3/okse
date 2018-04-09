package no.ntnu.okse.clients.xmpp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class XMPPPublisher extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public final int port = 5222;

  private XMPPClient client;

  public static void main(String[] args) {
    launch(new XMPPPublisher(), args);
  }

  @Override
  protected void createClient() {
    client = new XMPPClient("okse@okse", host, port);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }
}
