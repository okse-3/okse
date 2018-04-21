package no.ntnu.okse.protocol.mqttsn;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import no.ntnu.okse.clients.mqttsn.MQTTSNClient;
import no.ntnu.okse.protocol.FullMessageFunctionalityTest;
import org.eclipse.paho.mqttsn.udpclient.SimpleMqttsCallback;
import org.testng.annotations.Test;

public class MQTTSNSleepTest extends FullMessageFunctionalityTest {

  @Test
  public void testSleepMessagesQueuedUp() throws InterruptedException {
    MQTTSNClient publisher = new MQTTSNClient("localhost", 20000);
    publisher.connect();
    MQTTSNClient subscriber = new MQTTSNClient("localhost", 20000);
    SimpleMqttsCallback mqttsnCallback = mock(SimpleMqttsCallback.class);
    subscriber.setCallback(mqttsnCallback);
    subscriber.connect();

    subscriber.subscribe("MQTTSN");
    subscriber.sleep(10);
    Thread.sleep(1000);
    publisher.publish("MQTTSN", "test");
    Thread.sleep(1000);
    verify(mqttsnCallback, times(0)).publishArrived(anyBoolean(), anyInt(), anyString(), any());
    subscriber.wakeupForNewMessages();
    Thread.sleep(1000);
    verify(mqttsnCallback, times(1)).publishArrived(anyBoolean(), anyInt(), anyString(), any());
    publisher.publish("MQTTSN", "test2");
    Thread.sleep(1000);
    verify(mqttsnCallback, times(1)).publishArrived(anyBoolean(), anyInt(), anyString(), any());
    Thread.sleep(1000);
    subscriber.stopSleep();
    Thread.sleep(1000);
    verify(mqttsnCallback, times(2)).publishArrived(anyBoolean(), anyInt(), anyString(), any());
  }

}
