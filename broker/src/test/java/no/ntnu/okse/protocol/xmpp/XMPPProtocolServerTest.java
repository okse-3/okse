package no.ntnu.okse.protocol.xmpp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import no.ntnu.okse.core.messaging.Message;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XMPPProtocolServerTest {

  @InjectMocks
  XMPPProtocolServer xmppPS = new XMPPProtocolServer("localhost", 5222, "user@test", "pass");
  @Mock(name = "server")
  XMPPServer xmppServerSpy;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testBoot() throws Exception {
    assertFalse(xmppPS.isRunning());
    xmppPS.boot();
    assertTrue(xmppPS.isRunning());
  }

  @Test
  public void testStopServer() throws Exception {
    xmppPS.boot();
    Thread.sleep(1000);
    assertTrue(xmppPS.isRunning());
    xmppPS.stopServer();
    assertFalse(xmppPS.isRunning());
  }

  @Test
  public void testGetProtocolServerType() throws Exception {
    assertNotNull(xmppPS.getProtocolServerType());
    assertEquals(xmppPS.getProtocolServerType(), "xmpp");
  }

  @Test
  public void testSendMessage() throws Exception {
    Message message = new Message("testMessage", "testTopic", null, xmppPS.getProtocolServerType());
    xmppPS.sendMessage(message);
    Mockito.verify(xmppServerSpy).onMessageReceived(null, message.getTopic());
  }

}