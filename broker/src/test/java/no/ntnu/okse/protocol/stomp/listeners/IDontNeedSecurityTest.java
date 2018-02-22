package no.ntnu.okse.protocol.stomp.listeners;

import asia.stampy.common.message.StompMessageType;
import org.mockito.Mockito;
import org.testng.annotations.*;

import static org.testng.AssertJUnit.assertEquals;

public class IDontNeedSecurityTest{
    private IDontNeedSecurity listener;
    private IDontNeedSecurity listener_spy;

    @BeforeMethod
    public void setUp() {
        listener = new IDontNeedSecurity();
        listener_spy = Mockito.spy(listener);
    }

    @AfterMethod
    public void tearDown() {
        listener = null;
        listener_spy = null;
    }

    @Test
    public void isForMessage(){
        assertEquals(false, listener_spy.isForMessage(null));
    }

    @Test
    public void getMessageTypes(){
        StompMessageType[] types = listener_spy.getMessageTypes();
        assertEquals(null, types);
    }

    @Test
    public void messageReceived() {
        listener_spy.messageReceived(null, null);
    }
}