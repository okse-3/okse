package no.ntnu.okse.clients.xmpp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;

public class XMPPSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 5222;

  @Parameter(names = {"--jid"}, description = "JID")
  public String jid = "subscriber@localhost";

  private XMPPClient client;

  public static void main(String[] args) {
    launch(new XMPPSubscriber(), args);
  }

  @Override
  protected void createClient() {
    client = new XMPPClient(host, port, jid);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }
}
