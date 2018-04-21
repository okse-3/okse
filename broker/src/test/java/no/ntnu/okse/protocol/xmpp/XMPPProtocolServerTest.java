package no.ntnu.okse.protocol.xmpp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import no.ntnu.okse.OpenfireXMPPServerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class XMPPProtocolServerTest {

  private XMPPProtocolServer xmppProtocolServer;

  @BeforeClass
  public void classSetUp() throws IOException, InterruptedException {
    OpenfireXMPPServerFactory.start();
    // Allow Openfire to start correctly
    Thread.sleep(5000);
    xmppProtocolServer = new XMPPProtocolServer("localhost", 5222, "okse@localhost", "pass");
  }

  @AfterClass
  public void classTearDown() {
    OpenfireXMPPServerFactory.stop();
  }

  @Test
  public void testBootAndShutdown() throws InterruptedException {
    assertFalse(xmppProtocolServer.isRunning());
    xmppProtocolServer.boot();
    Thread.sleep(1000);
    assertTrue(xmppProtocolServer.isRunning());
    xmppProtocolServer.stopServer();
    assertFalse(xmppProtocolServer.isRunning());
  }

  @Test
  public void testProtocolServerType() {
    assertEquals(xmppProtocolServer.getProtocolServerType(), "xmpp");
  }

}