package no.ntnu.okse.clients.mqtt;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MQTTSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 1883;

  private MQTTClient client;

  private static final Logger log = Logger.getLogger(MQTTSubscriber.class);

  public static void main(String[] args) {
    launch(new MQTTSubscriber(), args);
  }

  protected void createClient() {
    client = new MQTTClient(host, port, "MQTTSubscriber");
    client.setCallback(new Callback(this));
  }

  protected TestClient getClient() {
    return client;
  }

  private static class Callback implements MqttCallback {

    private SubscribeClient subscribeClient;

    public Callback(SubscribeClient subscribeClient) {
      this.subscribeClient = subscribeClient;
    }

    public void connectionLost(Throwable throwable) {
      log.warn("Connection lost", throwable);
    }

    public void messageArrived(String topic, MqttMessage message) {
      subscribeClient.receiveMessage(topic, message.toString(), true);
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
      log.debug("Message delivered");
    }
  }
}
