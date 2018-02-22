package no.ntnu.okse.protocol.stomp;

import asia.stampy.common.message.interceptor.InterceptException;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.*;

import static org.testng.AssertJUnit.assertEquals;

public class STOMPProtocolServerTest {
    private STOMPProtocolServer ps_spy;
    private STOMPServer server_spy;

    @BeforeMethod
    public void setUp() throws Exception {
        STOMPServer server = new STOMPServer();
        STOMPProtocolServer ps = new STOMPProtocolServer("localhost", 61613);
        STOMPSubscriptionManager subManager = new STOMPSubscriptionManager();

        subManager.initCoreSubscriptionService(SubscriptionService.getInstance());
        STOMPSubscriptionManager subManager_spy = Mockito.spy(subManager);
        server.setSubscriptionManager(subManager_spy);

        server_spy = Mockito.spy(server);
        server_spy.init("localhost", 0);

        ps_spy = Mockito.spy(ps);
        ps_spy.setServer(server_spy);
    }

    @AfterMethod
    public void tearDown() {
        ps_spy = null;
    }

    @Test
    public void run() throws Exception {
        ps_spy.run();
        Mockito.verify(server_spy).init("localhost", 61613);

        //Test exception
        Mockito.doThrow(new Exception()).when(server_spy).init("localhost", 61613);

        Mockito.reset(ps_spy);

        ps_spy.run();
        //Test exception
        Mockito.doThrow(new InterceptException("Uncaught test exception")).when(server_spy).init("localhost", 61613);
    }

    @Test
    public void sendMessage() {
        Message msg = new Message("testing", "testing", null, "stomp");
        ps_spy.sendMessage(msg);

        ArgumentCaptor<Message> messageArgument = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(server_spy).queueMessage(messageArgument.capture());
        assertEquals(msg, messageArgument.getValue());
    }

    @Test
    public void boot(){
        assertEquals(false, ps_spy.isRunning());
        ps_spy.boot();
        assertEquals(true, ps_spy.isRunning());
        ps_spy.boot();
        assertEquals(true, ps_spy.isRunning());
    }

    @Test
    public void stopServer(){
        ps_spy.stopServer();
        assertEquals(false, ps_spy.isRunning());
        Mockito.doNothing().when(server_spy).stopServer();
        Mockito.verify(server_spy).stopServer();
    }

    @Test
    public void isRunning(){
        assertEquals(false, ps_spy.isRunning());
        ps_spy.boot();
        assertEquals(true, ps_spy.isRunning());
        ps_spy.boot();
        assertEquals(true, ps_spy.isRunning());
    }

    @Test
    public void getProtocolServerType(){
        assertEquals("stomp", ps_spy.getProtocolServerType());
    }

    @Test
    public void incrementTotalBadRequests(){
        int last = ps_spy.getTotalBadRequests();
        ps_spy.incrementTotalBadRequests();
        assertEquals(last + 1, ps_spy.getTotalBadRequests());
    }

    @Test
    public void incrementTotalRequests(){
        int last = ps_spy.getTotalRequests();
        ps_spy.incrementTotalRequests();
        assertEquals(last + 1, ps_spy.getTotalRequests());
    }

    @Test
    public void incrementTotalMessagesReceived(){
        int last = ps_spy.getTotalMessagesReceived();
        ps_spy.incrementTotalMessagesReceived();
        assertEquals(last + 1, ps_spy.getTotalMessagesReceived());
    }

    @Test
    public void incrementTotalMessagesSent(){
        int last = ps_spy.getTotalMessagesSent();
        ps_spy.incrementTotalMessagesSent();
        assertEquals(last + 1, ps_spy.getTotalMessagesSent());
    }

    @Test
    public void incrementTotalErrors(){
        int last = ps_spy.getTotalErrors();
        ps_spy.incrementTotalErrors();
        assertEquals(last + 1, ps_spy.getTotalErrors());
    }

}
