package no.ntnu.okse.clients.mqttsn;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class MQTTSNPublisher extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public final int port = 20000;

  private MQTTSNClient client;

  @Override
  protected void createClient() {
    client = new MQTTSNClient(host, port);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }

  public static void main(String[] args) {
    launch(new MQTTSNPublisher(), args);
  }
}
