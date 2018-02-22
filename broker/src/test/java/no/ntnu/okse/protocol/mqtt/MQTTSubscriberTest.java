package no.ntnu.okse.protocol.mqtt;

import no.ntnu.okse.core.subscription.Subscriber;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class MQTTSubscriberTest {

    private final String host = "127.0.0.1";
    private final int port = 1883;
    private final String topic = "testing";
    private final String clientID = "ogdans3";
    private Subscriber okseSub;
    private MQTTSubscriber sub;


    @BeforeTest
    public void setUp() {
        okseSub = new Subscriber(host, port, topic, "mqtt");
        sub = new MQTTSubscriber(host, port, topic, clientID, okseSub);
    }

    @AfterTest
    public void tearDown() {
        okseSub = null;
        sub = null;
    }

    @Test
    public void getHost() {
        assertEquals(host, sub.getHost());
    }

    @Test
    public void getPort() {
        assertEquals(port, sub.getPort());
    }

    @Test
    public void getTopic() {
        assertEquals(topic, sub.getTopic());
    }

    @Test
    public void getClientID() {
        assertEquals(clientID, sub.getClientID());
    }

    @Test
    public void getSubscriber() {
        assertEquals(okseSub, sub.getSubscriber());
    }
}
