package no.ntnu.okse.clients.mqttsn;

import no.ntnu.okse.clients.TestClient;
import org.eclipse.paho.mqttsn.udpclient.SimpleMqttsClient;

/**
 * By extending SimpleMqttsClient we gain almost everything for free
 */
public class MQTTSNClient extends SimpleMqttsClient implements TestClient {

  public MQTTSNClient() {
    this("localhost", 20000);
  }

  public MQTTSNClient(String host, int port) {
    super(host, port);
  }

  /**
   * Need to convert content to bytes to be able to use the base class
   */
  @Override
  public void publish(String topic, String content) {
    publish(topic, content.getBytes());
  }
}
