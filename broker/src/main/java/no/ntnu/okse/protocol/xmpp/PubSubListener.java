package no.ntnu.okse.protocol.xmpp;

import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

public class PubSubListener<T> implements ItemEventListener {

  private XMPPServer server;
  private String topic;

  protected PubSubListener(XMPPServer server, String topic) {
    super();
    this.server = server;
    this.topic = topic;
  }

  @Override
  public void handlePublishedItems(ItemPublishEvent itemPublishEvent) {
    server.onMessageReceived(itemPublishEvent.getItems(), topic);
  }
}
