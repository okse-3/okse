package no.ntnu.okse.clients.mqttsn;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.PublishClient;
import no.ntnu.okse.clients.TestClient;

public class MQTTSNPublisher extends PublishClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 20000;

  @Parameter(names = {"--qos", "-q"}, description = "Quality of Service")
  public int qos = 0;

  private MQTTSNClient client;

  @Override
  protected void createClient() {
    client = new MQTTSNClient(host, port);
  }

  @Override
  protected TestClient getClient() {
    return client;
  }

  @Override
  public void publish(String topic, String message) {
    client.publish(topic, message, qos);
  }

  public static void main(String[] args) {
    launch(new MQTTSNPublisher(), args);
  }
}
