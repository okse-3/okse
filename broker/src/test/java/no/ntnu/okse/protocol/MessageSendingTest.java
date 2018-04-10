package no.ntnu.okse.protocol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import no.ntnu.okse.clients.amqp091.AMQP091Callback;
import no.ntnu.okse.clients.mqttsn.MQTTSNClient;
import no.ntnu.okse.clients.stomp.StompCallback;
import no.ntnu.okse.clients.stomp.StompClient;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.clients.amqp.AMQPCallback;
import no.ntnu.okse.clients.amqp.AMQPClient;
import no.ntnu.okse.clients.amqp091.AMQP091Client;
import no.ntnu.okse.clients.mqtt.MQTTClient;
import no.ntnu.okse.clients.wsn.WSNClient;
import org.apache.cxf.wsn.client.Consumer;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.mqttsn.udpclient.SimpleMqttsCallback;
import org.testng.annotations.*;

import java.io.InputStream;

import static org.mockito.Mockito.*;

public class MessageSendingTest {

  private SubscriptionChangeListener subscriptionMock;
  private final SubscriptionService subscriptionService = SubscriptionService.getInstance();

  @BeforeClass
  public void classSetUp()
      throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CoreService cs = CoreService.getInstance();

    cs.registerService(MessageService.getInstance());
    cs.registerService(subscriptionService);

    InputStream resourceAsStream = CoreService.class
        .getResourceAsStream("/config/protocolservers.xml");
    cs.bootProtocolServers(resourceAsStream);
    cs.bootProtocolServers();

    // Wait for protocol servers to boot
    Thread.sleep(1000);

    Method bootSecondaryServers = CoreService.class.getDeclaredMethod("bootSecondaryServers");
    bootSecondaryServers.setAccessible(true);
    bootSecondaryServers.invoke(cs);

    cs.boot();

    // Make sure servers have booted properly
    Thread.sleep(3000);
  }

  @AfterClass
  public void classTearDown() {
    CoreService.getInstance().stop();
  }

  @BeforeMethod
  public void setUp() {
    subscriptionMock = mock(SubscriptionChangeListener.class);
    subscriptionService.addSubscriptionChangeListener(subscriptionMock);
  }

  @AfterMethod
  public void tearDown() {
    subscriptionService.removeAllListeners();
  }

  @Test
  public void mqttToMqtt() throws Exception {
    MQTTClient subscriber = new MQTTClient("localhost", 1883, "client1");
    MQTTClient publisher = new MQTTClient("localhost", 1883, "client2");
    subscriber.connect();
    MqttCallback callback = mock(MqttCallback.class);
    subscriber.setCallback(callback);
    publisher.connect();
    subscriber.subscribe("mqtt");

    verify(subscriptionMock, timeout(500).atLeastOnce()).subscriptionChanged(any());

    publisher.publish("mqtt", "Text content");
    Thread.sleep(2000);
    subscriber.disconnect();
    publisher.disconnect();
    verify(callback).messageArrived(anyString(), any(MqttMessage.class));
  }

  @Test
  public void mqttSNToMqttSN() throws Exception {
    MQTTSNClient subscriber = new MQTTSNClient("localhost", 20000);
    MQTTSNClient publisher = new MQTTSNClient("localhost", 20000);

    subscriber.connect();
    publisher.connect();

    SimpleMqttsCallback callback = mock(SimpleMqttsCallback.class);
    subscriber.setCallback(callback);
    subscriber.subscribe("mqtt-sn");

    verify(subscriptionMock, timeout(500).atLeastOnce()).subscriptionChanged(any());

    publisher.publish("mqtt-sn", "Text content");
    Thread.sleep(2000);
    verify(callback).publishArrived(anyBoolean(), anyInt(), anyString(), any());
    publisher.disconnect();
    subscriber.disconnect();
  }

  @Test
  public void amqpToAmqp() throws Exception {
    AMQPClient publisher = new AMQPClient();
    AMQPClient subscriber = new AMQPClient();
    publisher.connect();
    subscriber.connect();
    AMQPCallback callback = mock(AMQPCallback.class);
    subscriber.setCallback(callback);
    subscriber.subscribe("amqp");

    verify(subscriptionMock, timeout(500).atLeastOnce()).subscriptionChanged(any());

    publisher.publish("amqp", "Text content");
    publisher.disconnect();
    Thread.sleep(2000);
    subscriber.disconnect();
    verify(callback).onReceive(any());
  }

  @Test
  public void amqp091ToAmqp091() throws Exception {
    AMQP091Client subscriber = new AMQP091Client();
    AMQP091Client publisher = new AMQP091Client();
    subscriber.connect();
    publisher.connect();
    AMQP091Callback callback = mock(AMQP091Callback.class);
    subscriber.setCallback(callback);
    subscriber.subscribe("amqp091");

    verify(subscriptionMock, timeout(500).atLeastOnce()).subscriptionChanged(any());

    publisher.publish("amqp091", "Text content");
    Thread.sleep(2000);
    subscriber.disconnect();
    publisher.disconnect();
    verify(callback).messageReceived(anyString(), anyString());
  }

  @Test
  public void wsnToWsn() throws InterruptedException {
    WSNClient subscriber = new WSNClient();
    WSNClient publisher = new WSNClient();
    Consumer.Callback callback = mock(Consumer.Callback.class);
    subscriber.setCallback(callback);
    subscriber.subscribe("wsn");

    verify(subscriptionMock, timeout(1000).atLeastOnce()).subscriptionChanged(any());

    publisher.publish("wsn", "Text content");
    Thread.sleep(2000);
    subscriber.unsubscribe("wsn");
    verify(callback).notify(any());
  }

  @Test
  public void stompToStomp() throws InterruptedException {
    StompClient subscriber = new StompClient();
    StompClient publisher = new StompClient();
    subscriber.connect();
    publisher.connect();
    StompCallback callback = mock(StompCallback.class);
    subscriber.setCallback(callback);
    subscriber.subscribe("stomp");

    verify(subscriptionMock, timeout(1000).atLeastOnce()).subscriptionChanged(any());

    publisher.publish("stomp", "Text content");
    Thread.sleep(2000);
    subscriber.unsubscribe("stomp");
    subscriber.disconnect();
    publisher.disconnect();
    verify(callback).messageReceived(any());
  }

  @Test
  public void allToAll() throws Exception {
    int numberOfProtocols = 6;
    // WSN
    WSNClient wsnClient = new WSNClient();
    Consumer.Callback wsnCallback = mock(Consumer.Callback.class);
    wsnClient.setCallback(wsnCallback);

    // MQTT
    MQTTClient mqttClient = new MQTTClient("localhost", 1883, "clientAll");
    MqttCallback mqttCallback = mock(MqttCallback.class);
    mqttClient.setCallback(mqttCallback);

    // MQTT-SN
    MQTTSNClient mqttsnClient = new MQTTSNClient("localhost", 20000);
    SimpleMqttsCallback mqttsnCallback = mock(SimpleMqttsCallback.class);
    mqttsnClient.setCallback(mqttsnCallback);

    // AMQP 0.9.1
    AMQP091Client amqp091Client = new AMQP091Client();
    AMQP091Callback amqp091Callback = mock(AMQP091Callback.class);
    amqp091Client.setCallback(amqp091Callback);

    // AMQP 1.0
    AMQPClient amqpClient = new AMQPClient();
    // AMQP test client is unable to receive and send messages at the same time
    AMQPClient amqpSender = new AMQPClient();
    AMQPCallback amqpCallback = mock(AMQPCallback.class);
    amqpClient.setCallback(amqpCallback);

    // Stomp
    StompClient stompClient = new StompClient();
    StompCallback stompCallback = mock(StompCallback.class);
    stompClient.setCallback(stompCallback);

    // Connecting
    mqttClient.connect();
    mqttsnClient.connect();
    amqp091Client.connect();
    amqpClient.connect();
    amqpSender.connect();
    stompClient.connect();

    // Subscribing
    wsnClient.subscribe("all", "localhost", 9002);
    mqttClient.subscribe("all");
    mqttsnClient.subscribe("all");
    amqp091Client.subscribe("all");
    amqpClient.subscribe("all");
    stompClient.subscribe("all");

    verify(subscriptionMock, timeout(500).atLeast(numberOfProtocols)).subscriptionChanged(any());

    // Publishing
    wsnClient.publish("all", "WSN");
    mqttClient.publish("all", "MQTT");
    mqttsnClient.publish("all", "MQTT-SN");
    amqp091Client.publish("all", "AMQP 0.9.1");
    amqpSender.publish("all", "AMQP 1.0");
    stompClient.publish("all", "STOMP");

    // Wait for messages to arrive
    Thread.sleep(2000);

    // Unsubscribing/disconnecting
    wsnClient.unsubscribe("all");
    mqttClient.disconnect();
    mqttsnClient.disconnect();
    amqp091Client.disconnect();
    amqpClient.disconnect();
    amqpSender.disconnect();
    stompClient.disconnect();

    // Verifying that all messages were sent
    verify(amqpCallback, times(numberOfProtocols)).onReceive(any());
    verify(mqttCallback, times(numberOfProtocols))
        .messageArrived(anyString(), any(MqttMessage.class));
    // MQTT-SN test client will not receive its own messages to a topic it is subscribed to
    verify(mqttsnCallback, times(numberOfProtocols - 1))
        .publishArrived(anyBoolean(), anyInt(), anyString(), any());
    verify(amqp091Callback, times(numberOfProtocols)).messageReceived(any(), any());
    verify(wsnCallback, times(numberOfProtocols)).notify(any());
    verify(stompCallback, times(numberOfProtocols)).messageReceived(any());
  }
}
