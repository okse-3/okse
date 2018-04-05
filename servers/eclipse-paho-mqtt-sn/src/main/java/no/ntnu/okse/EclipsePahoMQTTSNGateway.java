package no.ntnu.okse;

import org.eclipse.paho.mqttsn.gateway.Gateway;

public class EclipsePahoMQTTSNGateway {

  private static boolean running;

  /**
   * Starts the MQTT-SN Gateway if it is running
   */
  public static void start() {
    if (running) {
      throw new IllegalStateException("Server already running");
    }
    new Gateway().start("gateway.properties");
    running = true;
  }

  /**
   * Stops the MQTT-SN Gateway if it is running
   */
  public static void stop() {
    if (!running) {
      throw new IllegalStateException("Server is not running");
    }
    Gateway.shutDown();
    running = false;
  }

}
