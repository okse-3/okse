package no.ntnu.okse.protocol.xmpp;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XMPPServerTest {

  XMPPProtocolServer ps;

  @BeforeMethod
  public void setUp() throws Exception {
    ps = new XMPPProtocolServer("localhost", 5222 );
    ps.boot();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    ps.stopServer();
    ps = null;
  }

  @Test
  public void testStopServer() throws Exception {
  }

  @Test
  public void testCreateLeafNode() throws Exception {
  }

  @Test
  public void testSendMessage() throws Exception {
  }

  @Test
  public void testSubscribeToNode() throws Exception {
  }

  @Test
  public void testOnMessageReceived() throws Exception {
  }

}