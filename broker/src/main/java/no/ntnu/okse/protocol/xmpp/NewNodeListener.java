package no.ntnu.okse.protocol.xmpp;

import no.ntnu.okse.core.topic.TopicService;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public class NewNodeListener extends Thread {

  private boolean running;
  private long last;
  private XMPPServer xmppServer;
  private PubSubManager ps;

  public NewNodeListener(XMPPServer xmppServer, PubSubManager ps) {
    this.ps = ps;
    this.xmppServer = xmppServer;
  }

  @Override
  public void run() {
    running = true;
    last = System.currentTimeMillis();
    while (running) {
      if (last + 1000 < System.currentTimeMillis()) {
        TopicService ts = TopicService.getInstance();
        try {
          for (Item item: ps.discoverNodes(null).getItems()) {
            if (!ts.topicExists(item.getName())) {
              try {
                xmppServer.subscribeToTopic(item.getName());
              } catch (NotALeafNodeException e) {
                e.printStackTrace();
              }
            }
          }
        } catch (NoResponseException | XMPPErrorException | InterruptedException | NotConnectedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void stopListener() {
    running = false;
  }

}
