package no.ntnu.okse.protocol;

import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public class FullMessageFunctionalityTest {

  protected SubscriptionChangeListener subscriptionMock;
  private final SubscriptionService subscriptionService = SubscriptionService.getInstance();

  @BeforeClass
  public void classSetUp()
      throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    CoreService cs = CoreService.getInstance();

    cs.registerService(MessageService.getInstance());
    cs.registerService(subscriptionService);

    InputStream resourceAsStream = CoreService.class
        .getResourceAsStream("/config/protocolservers.xml");
    cs.bootProtocolServers(resourceAsStream);
    cs.bootProtocolServers();

    // Wait for protocol servers to boot
    Thread.sleep(1000);

    Method bootSecondaryServers = CoreService.class.getDeclaredMethod("bootSecondaryServers");
    bootSecondaryServers.setAccessible(true);
    bootSecondaryServers.invoke(cs);

    cs.boot();

    // Make sure servers have booted properly
    Thread.sleep(3000);
  }

  @BeforeMethod
  public void setUp() {
    subscriptionMock = mock(SubscriptionChangeListener.class);
    subscriptionService.addSubscriptionChangeListener(subscriptionMock);
  }

  @AfterMethod
  public void tearDown() {
    subscriptionService.removeAllListeners();
  }

  @AfterClass
  public void classTearDown() {
    CoreService.getInstance().stop();
  }

}
