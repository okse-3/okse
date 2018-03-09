package no.ntnu.okse.protocol.xmpp;

import static org.testng.Assert.assertNotNull;

import no.ntnu.okse.core.messaging.Message;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XMPPServerTest {

  @Mock
  XMPPProtocolServer ps;
  @InjectMocks
  XMPPServer server = new XMPPServer(ps, "localhost", 5222);

  @BeforeMethod
  public void setUp() throws Exception {
    ps.boot();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateAndGetLeafNode() throws Exception {
    assertNotNull(server.getLeafNode("testTopic"));
  }

  @Test
  public void testSendMessage() throws Exception {
    server.subscribeToTopic("testTopic");
    xmpppublishclinet.sendMessage(new Message("testMessage", "testTopic", null, ps.getProtocolServerType()));
    Mockito.verify(server).onMessageReceived(null, "testTopic");
  }

  @Test
  public void testSubscribeToNode() throws Exception {

  }

  @Test
  public void testOnMessageReceived() throws Exception {

      }

}