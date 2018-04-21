package no.ntnu.okse.clients.mqttsn;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;
import org.apache.log4j.Logger;
import org.eclipse.paho.mqttsn.udpclient.SimpleMqttsCallback;

public class MQTTSNSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public final int port = 20000;

  private MQTTSNClient client;

  private static final Logger log = Logger.getLogger(MQTTSNSubscriber.class);

  @Override
  protected void createClient() {
    client = new MQTTSNClient(host, port);
    client.setCallback(new Callback());
  }

  @Override
  protected TestClient getClient() {
    return client;
  }

  public static void main(String[] args) {
    launch(new MQTTSNSubscriber(), args);
  }

  private static class Callback implements SimpleMqttsCallback {

    @Override
    public void publishArrived(boolean ret, int qualityOfService, String topic, byte[] msg) {
      System.out.println(String.format("Received message %s on topic %s", new String(msg), topic));
    }

    @Override
    public void disconnected(int returnType) {
      log.warn(String.format("Disconnected from server with status %d", returnType));
    }
  }

}
