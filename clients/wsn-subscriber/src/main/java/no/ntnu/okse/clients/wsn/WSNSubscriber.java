package no.ntnu.okse.clients.wsn;

import com.beust.jcommander.Parameter;
import no.ntnu.okse.clients.SubscribeClient;
import no.ntnu.okse.clients.TestClient;
import org.apache.cxf.wsn.client.Consumer;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.w3c.dom.Element;

public class WSNSubscriber extends SubscribeClient {

  @Parameter(names = {"--port", "-p"}, description = "Port")
  public int port = 61000;

  @Parameter(names = {"--client-host", "-ch"}, description = "Client Port")
  public String clientHost = "localhost";

  @Parameter(names = {"--client-port", "-cp"}, description = "Client Port")
  public int clientPort = 9000;

  @Parameter(names = {"--host-url-extension", "-url"}, description = "Host Notification Broker url")
  public String host_url_extension = "";

  private WSNClient client;

  public static void main(String[] args) {
    launch(new WSNSubscriber(), args);
  }

  protected void createClient() {
    client = new WSNClient(host, port, host_url_extension);
    client.setCallback(new WSNConsumer(this));
  }

  protected TestClient getClient() {
    return client;
  }

  public void subscribe(String topic) {
    client.subscribe(topic, clientHost, clientPort);
  }

  private class WSNConsumer implements Consumer.Callback {

    private SubscribeClient subscribeClient;

    public WSNConsumer(SubscribeClient subscribeClient) {
      this.subscribeClient = subscribeClient;
    }

    public void notify(NotificationMessageHolderType message) {
      Object o = message.getMessage().getAny();
      if (o instanceof Element) {
        subscribeClient.receiveMessage(message.getTopic().getContent().toString(),
            ((Element) o).getTextContent(), true);
      }
    }
  }
}
