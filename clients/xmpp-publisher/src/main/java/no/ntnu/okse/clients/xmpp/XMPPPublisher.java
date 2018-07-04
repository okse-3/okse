package no.ntnu.okse.clients.xmpp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class XMPPPublisher extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 5222;

  @Parameter(names = {"--jid"}, description = "JID")
  public String jid = "publisher@localhost";

  @Parameter(names = {"--password", "--pass"}, description = "User password")
  public String password = "password";

  private XMPPClient client;

  public static void main(String[] args) {
    launch(new XMPPPublisher(), args);
  }

  @Override
  protected void createClient() {
    client = new XMPPClient(host, port, jid, password);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }
}
