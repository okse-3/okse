package no.ntnu.okse.protocol.xmpp;

import no.ntnu.okse.core.topic.Topic;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

public class PubSubListener<T> implements ItemEventListener {

  private XMPPServer server;
  private Topic topic;

  protected PubSubListener(XMPPServer server, Topic topic) {
    super();
    this.server = server;
    this.topic = topic;
  }

  @Override
  public void handlePublishedItems(ItemPublishEvent itemPublishEvent) {
    server.onMessageReceived(itemPublishEvent.getItems(), topic);
  }
}
