package no.ntnu.okse.clients.xmpp;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.CommandClient;
import no.ntnu.okse.clients.TestClient;

public class XMPPAccountCreator extends CommandClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 5222;

  @Parameter(names = {"--jid"}, description = "JID: username@network-identificator", required = true)
  public String jid;

  @Parameter(names = {"--password", "--pass"}, description = "Account password", required = true)
  public String password;

  private XMPPClient client;

  public static void main(String[] args) {
    launch(new XMPPAccountCreator(), args);
  }

  @Override
  protected void createClient() {
    client = new XMPPClient(host, port, jid, password);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }

  @Override
  public void run() {
    createClient();
    client.connectAndCreateAccount();
    client.disconnect();
  }
}
