package no.ntnu.okse.clients;

import com.beust.jcommander.Parameter;

public abstract class PublishClient extends TopicClient {

  @Parameter(names = {"--message", "-m"}, description = "Message", required = true)
  public String message;

  @Parameter(names = {"-n"}, description = "Number of messages to send")
  public int numberOfMessages = 1;

  public void run() {
    initLogger();
    createClient();
    TestClient client = getClient();
    client.connect();
    Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
    // Send n number of messages
    for (int i = 0; i < numberOfMessages; i++) {
      for (String topic : topics) {
        publish(topic, message);
      }
    }
    System.exit(0);
  }

  public void publish(String topic, String message) {
    getClient().publish(topic, message);
  }
}
