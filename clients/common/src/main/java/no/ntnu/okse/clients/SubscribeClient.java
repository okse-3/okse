package no.ntnu.okse.clients;

import com.beust.jcommander.Parameter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SubscribeClient extends CommandClient {

  @Parameter(names = {"-n"}, description = "Number of messages to receive")
  public int numberOfMessages = Integer.MAX_VALUE;

  private AtomicInteger numberOfReceivedMessages = new AtomicInteger(0);
  private Thread shutdownHook;
  private TestClient client;

  public void run() {
    initLogger();
    createClient();
    client = getClient();
    client.connect();
    topics.forEach(this::subscribe);
    // Graceful disconnect
    shutdownHook = new Thread(() -> shutdown(true));
    Runtime.getRuntime().addShutdownHook(shutdownHook);
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
    int messageNumber = numberOfReceivedMessages.incrementAndGet();
    if (shouldPrint) {
      System.out.println(String.format("Recieved message #%d on topic %s with content %s",
          messageNumber, topic, message));
    }
    if (numberOfMessages == messageNumber) {
      System.out.println("Received indicated number of messages, un-subscribing and disconnecting");
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      // In case the unsubscribe/disconnect is not working correctly
      new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          System.out
              .println("Un-subscribing and disconnect is taking too long. Forcing a shutdown");
          System.exit(1);
        }
      }, new Date(System.currentTimeMillis() + 10000));
      new Thread(() -> shutdown(false)).start();
    }
  }

  private void shutdown(boolean isHook) {
    try {
      topics.forEach(client::unsubscribe);
    } catch (Exception ignored) {
    }
    client.disconnect();
    if (!isHook) {
      System.exit(0);
    }
  }
}
