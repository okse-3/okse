package no.ntnu.okse.protocol;

import no.ntnu.okse.OpenfireXMPPServerFactory;
import no.ntnu.okse.clients.amqp091.AMQP091Callback;
import no.ntnu.okse.clients.stomp.StompCallback;
import no.ntnu.okse.clients.stomp.StompClient;
import no.ntnu.okse.clients.xmpp.XMPPClient;
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
import org.testng.annotations.*;
import static org.testng.Assert.*;

import java.io.InputStream;

import static org.mockito.Mockito.*;

public class MessageSendingTest {

  private SubscriptionChangeListener subscriptionMock;
  private final SubscriptionService subscriptionService = SubscriptionService.getInstance();
  private CoreService cs;

  @BeforeClass
  public void classSetUp() throws Exception {
    OpenfireXMPPServerFactory.start();
    Thread.sleep(5000);

    cs = CoreService.getInstance();

    cs.registerService(MessageService.getInstance());
    cs.registerService(subscriptionService);

    InputStream resourceAsStream = CoreService.class
        .getResourceAsStream("/config/protocolservers.xml");
    cs.bootProtocolServers(resourceAsStream);
    cs.bootProtocolServers();
    cs.boot();

    Thread.sleep(5000);
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

  @AfterClass
  public void tearDownClass() {
    OpenfireXMPPServerFactory.stop();
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
  public void xmppToXmpp() throws Exception{
    XMPPClient client1 = new XMPPClient("localhost", 5222, "okse1@localhost");
    XMPPClient client2 = new XMPPClient("localhost", 5222, "okse2@localhost");
    client1.connect();
    client2.connect();
    client1.subscribe("SujetDeTest");
    client2.subscribe("SujetDeTest");
    client1.publish("SujetDeTest", "This is a test message.");
    client1.publish("SujetDeTest", "This is a test messagex.");
    client2.publish("SujetDeTest", "This is a test message.");

    Thread.sleep(1000);

    assertEquals(client1.messageCounter, 3);
    assertEquals(client2.messageCounter,  3);

    client1.unsubscribe("SujetDeTest");
    client2.unsubscribe("SujetDeTest");
    client1.disconnect();
    client2.disconnect();
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

    // XMPP
    XMPPClient xmppClient = new XMPPClient("localhost", 5222, "okse1@localhost");

    // Connecting
    mqttClient.connect();
    amqp091Client.connect();
    amqpClient.connect();
    amqpSender.connect();
    stompClient.connect();
    xmppClient.connect();

    Thread.sleep(300);

    // Subscribing
    wsnClient.subscribe("all", "localhost", 9002);
    mqttClient.subscribe("all");
    amqp091Client.subscribe("all");
    amqpClient.subscribe("all");
    stompClient.subscribe("all");
    xmppClient.subscribe("all");

    Thread.sleep(300);

    // Due to the nature of the XMPP setup, the protocol does not update the subscription service
    verify(subscriptionMock, timeout(500).atLeast(numberOfProtocols - 1)).subscriptionChanged(any());

    // Publishing
    xmppClient.publish("all", "XMPP");
    wsnClient.publish("all", "WSN");
    mqttClient.publish("all", "MQTT");
    amqp091Client.publish("all", "AMQP 0.9.1");
    amqpSender.publish("all", "AMQP 1.0");
    stompClient.publish("all", "STOMP");

    // Wait for messages to arrive
    Thread.sleep(3000);

    // Unsubscribing/disconnecting
    wsnClient.unsubscribe("all");
    mqttClient.disconnect();
    amqp091Client.disconnect();
    amqpClient.disconnect();
    amqpSender.disconnect();
    stompClient.disconnect();
    xmppClient.unsubscribe("all");
    xmppClient.disconnect();

    // Verifying that all messages were sent
    verify(amqpCallback, times(numberOfProtocols)).onReceive(any());
    verify(mqttCallback, times(numberOfProtocols)).messageArrived(anyString(), any(MqttMessage.class));
    verify(amqp091Callback, times(numberOfProtocols)).messageReceived(any(), any());
    verify(wsnCallback, times(numberOfProtocols)).notify(any());
    assertEquals(xmppClient.messageCounter, numberOfProtocols);
    verify(stompCallback, times(numberOfProtocols)).messageReceived(any());
  }
}
