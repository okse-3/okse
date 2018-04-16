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
    client = new XMPPClient(host, port, "publisher@127.0.0.1");
  }

  @Override
  protected TestClient getClient() {
    return client;
  }
}
