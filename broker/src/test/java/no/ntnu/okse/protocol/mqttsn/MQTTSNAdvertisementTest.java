package no.ntnu.okse.protocol.mqttsn;

import static org.testng.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import no.ntnu.okse.EclipsePahoMQTTSNGateway;
import no.ntnu.okse.clients.mqttsn.MQTTSNClient;
import no.ntnu.okse.protocol.mqtt.MQTTProtocolServer;
import org.eclipse.paho.mqttsn.gateway.messages.control.ControlMessage;
import org.eclipse.paho.mqttsn.gateway.timer.TimerService;
import org.eclipse.paho.mqttsn.gateway.utils.GWParameters;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.eclipse.paho.mqttsn.udpclient.SimpleMqttsClient;
import org.eclipse.paho.mqttsn.udpclient.messages.mqttsn.MqttsMessage;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class MQTTSNAdvertisementTest {

  @BeforeTest
  public void setUp() throws InterruptedException {
    MQTTProtocolServer mqttServer = new MQTTProtocolServer("localhost", 1883);
    mqttServer.boot();

    // Wait for MQTT to start, before starting MQTT-SN
    Thread.sleep(1000);
    EclipsePahoMQTTSNGateway.start();

    // Make sure that the advertisement messages are sent at a higher interval (every second)
    TimerService.getInstance()
        .unregister(GWParameters.getGatewayAddress(), ControlMessage.ADVERTISE);
    TimerService.getInstance()
        .register(GWParameters.getGatewayAddress(), ControlMessage.ADVERTISE, 1);
  }

  @Test
  public void testReceiveAdvertisementMessage() throws Exception {
    MQTTSNClient client = new MQTTSNClient();
    Field mqttsnBaseClient = client.getClass().getSuperclass().getDeclaredField("client");
    mqttsnBaseClient.setAccessible(true);
    AdvertisementCounterClient counterClient = new AdvertisementCounterClient("localhost", 20000,
        false);
    counterClient.registerHandler(client);
    mqttsnBaseClient.set(client, counterClient);
    // Must connect to make the client listen to the interface
    client.connect();
    // Wait for advertisement message
    Thread.sleep(1100);
    assertTrue(counterClient.getAdvertisementCount() >= 1);
  }

  private static class AdvertisementCounterClient extends MqttsClient {

    private int advertisementCounter;

    public AdvertisementCounterClient(String gatewayAddress, int gatewayPort, boolean auto) {
      super(gatewayAddress, gatewayPort, auto);
    }

    @Override
    protected void handleMqttsMessage(MqttsMessage message) {
      if (message.getMsgType() == MqttsMessage.ADVERTISE) {
        advertisementCounter++;
      }
      super.handleMqttsMessage(message);
    }

    public int getAdvertisementCount() {
      return advertisementCounter;
    }
  }

}
