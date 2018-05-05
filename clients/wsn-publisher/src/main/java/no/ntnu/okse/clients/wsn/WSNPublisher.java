package no.ntnu.okse.clients.wsn;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class WSNPublisher extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 61000;

  @Parameter(names = {"--host-url-extension", "-url"}, description = "Host Notification Broker url")
  public String host_url_extension = "";

  private WSNClient client;

  public static void main(String[] args) {
    launch(new WSNPublisher(), args);
  }

  public void createClient() {
    client = new WSNClient(host, port, host_url_extension);
  }

  public TestClient getClient() {
    return client;
  }
}
