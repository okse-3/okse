package no.ntnu.okse.protocol.rabbitmq;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;

public class RabbitMQListenerClient {

  RabbitMQProtocolServer ps;
  private Connection connection;
  private Channel channel;
  private String clientID;
  private String exchangeName;
  private Logger logger = Logger.getLogger(RabbitMQListenerClient.class.getName());


  /**
   * Initialises a RabbitMq client that will listen to messages sent to the RabbitMq broker
   * @param ps, the responsible RabbitMQProtocolServer
   * @param host, the host of the rabbitMq broker
   * @param port, the port of the rabbitMq broker
   * @param clientID, a string identifier for this client, used to filter out own messages
   * @param exchangeName, the exchange to connect to
   * @param topics, what topics to listen for messages at
   * @throws IOException
   * @throws TimeoutException
   */
  public RabbitMQListenerClient(RabbitMQProtocolServer ps, String host, int port, String clientID,
      String exchangeName, List<String> topics)
      throws IOException, TimeoutException {
    logger.info("Starting RabbitMQListenerClient");
    this.ps = ps;
    this.clientID = clientID;
    this.exchangeName = exchangeName;
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);

    connection = factory.newConnection();
    channel = connection.createChannel();
    channel.exchangeDeclare(exchangeName, "topic", true);
    String queueName = channel.queueDeclare().getQueue();

    // sets us the topics we are gonna listen to, default is "#" (wildcard: all topics)
    for (String t : topics) {
      channel.queueBind(queueName, exchangeName, t);
    }

    // set up listener
    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope,
          AMQP.BasicProperties properties, byte[] body) throws IOException {

        // check sender in order to ignore own messages
        if (Objects.equals(properties.getHeaders().get("sender"),
            LongStringHelper.asLongString(clientID))) {
          return;
        }

        logger.info("Rabbit received message at topic " + envelope.getRoutingKey());
        if (ps != null) {
          ps.incrementTotalMessagesReceived();
          String message = new String(body, "UTF-8");
          MessageService.getInstance()
              .distributeMessage(new Message(message, envelope.getRoutingKey(), null, "rabbitmq"));
          logger.info("message distributed");
        }

      }
    };
    channel.basicConsume(queueName, true, consumer);
    logger.info("RabbitMQListenerClient started successfully");
  }


  /**
   * Stops the Listener client
   * @throws IOException
   * @throws TimeoutException
   */
  public void stopListenerClient() throws IOException, TimeoutException {
    logger.info("Stopping RabbitMQListenerClient");
    channel.close();
    connection.close();
    logger.info("RabbitMQListenerClient stopped");
  }

  /**
   * Formats an OKSE {@link Message} and sends it as a rabbitMq publish
   *
   * @param message, the message to be sent
   * @throws IOException
   */
  public void sendMessage(Message message) throws IOException {
    Map<String, Object> headerMap = new HashMap<>();
    headerMap.put("sender", clientID);
    BasicProperties properties = new BasicProperties().builder().headers(headerMap).build();

    channel.basicPublish(exchangeName, message.getTopic(), properties,
        message.getMessage().getBytes());
    logger.info("Rabbit sent message on topic " + message.getTopic());
  }

}
