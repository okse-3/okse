package no.ntnu.okse.clients.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.LongStringHelper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import no.ntnu.okse.clients.TestClient;

public class RabbitMqClient implements TestClient {

  private String host;
  private Integer port;
  private String id;
  private String exchangeName;

  private Connection connection;
  private Channel channel;


  public RabbitMqClient(String host, Integer port, String id, String exchangeName) {
    this.host = host;
    this.port = port;
    this.id = id;
    this.exchangeName = exchangeName;
  }

  @Override
  public void connect() {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);

    try {
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.exchangeDeclare(exchangeName, "topic", true);
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void disconnect() {
    try {
      channel.close();
      connection.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void subscribe(String topic) {
    try {
      String queueName = channel.queueDeclare().getQueue();
      channel.queueBind(queueName, exchangeName, topic);

      Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
            AMQP.BasicProperties properties, byte[] body) throws IOException {

          // check sender in order to ignore own messages
          if (Objects.equals(properties.getHeaders().get("sender"),
              LongStringHelper.asLongString(id))) {
            return;
          }

          System.out.println("Got message: [" + envelope.getRoutingKey() + "] " + new String(body, "UTF-8"));
        }
      };
      channel.basicConsume(queueName, true, consumer);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void unsubscribe(String topic) {
    try {
      String queueName = channel.queueDeclare().getQueue();
      channel.queueUnbind(queueName, exchangeName, topic);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void publish(String topic, String content) {
    try {
      Map<String, Object> headerMap = new HashMap<>();
      headerMap.put("sender", id);
      BasicProperties properties = new BasicProperties().builder().headers(headerMap).build();
      channel.basicPublish(exchangeName, topic, properties, content.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
