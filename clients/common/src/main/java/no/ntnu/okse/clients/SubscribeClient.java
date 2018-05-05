package no.ntnu.okse.clients;

import com.beust.jcommander.Parameter;

public abstract class SubscribeClient extends CommandClient {

  @Parameter(names = {"-n"}, description = "Number of messages to receive")
  public int numberOfMessages = Integer.MAX_VALUE;

  private int numberOfReceivedMessages = 0;

  public void run() {
    initLogger();
    createClient();
    TestClient client = getClient();
    client.connect();
    topics.forEach(this::subscribe);
    // Graceful disconnect
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      topics.forEach(client::unsubscribe);
      client.disconnect();
    }));
    System.out.println("Listening for messages...");
    try {
      Thread.sleep(10000000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void subscribe(String topic) {
    getClient().subscribe(topic);
  }

  public void receiveMessage(String topic, String message, boolean shouldPrint) {
    numberOfReceivedMessages++;
    if (shouldPrint) {
      System.out.println(String.format("Recieved message #%d on topic %s with content %s",
          numberOfReceivedMessages, topic, message));
    }
    if (numberOfMessages == numberOfReceivedMessages) {
      System.out.println("Received indicated number of messages, un-subscribing and disconnecting");
      System.exit(0);
    }
  }
}
