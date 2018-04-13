package no.ntnu.okse.protocol.xmpp;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public class NewNodeListener extends Thread {

  private boolean running;
  private long last;
  private XMPPServer xmppServer;
  private PubSubManager ps;
  private String jid;
  private Logger log = Logger.getLogger(this.getClass());

  public NewNodeListener(XMPPServer xmppServer, PubSubManager ps, String jid) {
    this.ps = ps;
    this.jid = jid;
    this.xmppServer = xmppServer;
  }

  @Override
  public void run() {
    running = true;
    last = System.currentTimeMillis();
    while (running) {
      if (last + 1000 < System.currentTimeMillis()) {
        try {
          for (Item item: ps.discoverNodes(null).getItems()) {
            try {
              LeafNode node = xmppServer.getLeafNode(item.getNode());
              if (node.getSubscriptions().stream().noneMatch(s -> s.getJid().equals(jid))) {
                xmppServer.subscribeToNode(node);
                log.info("Found and subscribed to node: " + item.getNode());
              }
            } catch (NotAPubSubNodeException e) {
              e.printStackTrace();
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
