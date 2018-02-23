package no.ntnu.okse.protocol;

import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import no.ntnu.okse.protocol.amqp091.AMQP091ProtocolServer;
import no.ntnu.okse.protocol.mqtt.MQTTProtocolServer;
import no.ntnu.okse.protocol.stomp.STOMPProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ProtocolServerFactory {

  public static ProtocolServer create(Node node) {
    NamedNodeMap attr = node.getAttributes();
    if (attr.getNamedItem("type") == null) {
      return null;
    }

    switch (attr.getNamedItem("type").getNodeValue()) {
      case "amqp":
        return createAMQP(attr);
      case "mqtt":
        return createMQTT(attr);
      case "wsn":
        return createWSN(attr);
      case "stomp":
        return createStomp(attr);
      case "amqp091":
        return createAMQP091(attr);
      default:
        return null;
    }
  }

  private static boolean stringToBoolean(String bool, boolean defaultValue) {
    return bool.equals("true") || !bool.equals("false") && defaultValue;
  }

  private static int stringToPort(String port, int defaultValue) {
    try {
      int p = Integer.parseInt(port);
      if (p >= 1 && p <= 65535) {
        return p;
      } else {
        return defaultValue;
      }
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static int stringToInt(String i, int defaultValue) {
    try {
      return Integer.parseInt(i);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static AMQProtocolServer createAMQP(NamedNodeMap attr) {
    final String DEFAULT_HOST = "0.0.0.0";
    final int DEFAULT_PORT = 5672;
    final boolean DEFAULT_SASL = true;
    final boolean DEFAULT_QUEUE = false;

    String host = attr.getNamedItem("host") != null ?
        attr.getNamedItem("host").getNodeValue() :
        DEFAULT_HOST;

    int port = attr.getNamedItem("port") != null ?
        stringToPort(attr.getNamedItem("port").getNodeValue(), DEFAULT_PORT) :
        DEFAULT_PORT;

    boolean sasl = attr.getNamedItem("sasl") != null ?
        stringToBoolean(attr.getNamedItem("sasl").getNodeValue(), DEFAULT_SASL) :
        DEFAULT_SASL;

    boolean queue = attr.getNamedItem("queue") != null ?
        stringToBoolean(attr.getNamedItem("queue").getNodeValue(), DEFAULT_QUEUE) :
        DEFAULT_QUEUE;

    return new AMQProtocolServer(host, port, queue, sasl);
  }

  private static AMQP091ProtocolServer createAMQP091(NamedNodeMap attr) {
    final String DEFAULT_HOST = "0.0.0.0";
    final int DEFAULT_PORT = 5672;

    String host = attr.getNamedItem("host") != null ?
        attr.getNamedItem("host").getNodeValue() :
        DEFAULT_HOST;

    int port = attr.getNamedItem("port") != null ?
        stringToPort(attr.getNamedItem("port").getNodeValue(), DEFAULT_PORT) :
        DEFAULT_PORT;

    return new AMQP091ProtocolServer(host, port);
  }

  private static MQTTProtocolServer createMQTT(NamedNodeMap attr) {
    final String DEFAULT_HOST = "0.0.0.0";
    final int DEFAULT_PORT = 1883;

    String host = attr.getNamedItem("host") != null ?
        attr.getNamedItem("host").getNodeValue() :
        DEFAULT_HOST;

    int port = attr.getNamedItem("port") != null ?
        stringToPort(attr.getNamedItem("port").getNodeValue(), DEFAULT_PORT) :
        DEFAULT_PORT;

    return new MQTTProtocolServer(host, port);
  }

  private static WSNotificationServer createWSN(NamedNodeMap attr) {
    final String DEFAULT_HOST = "0.0.0.0";
    final int DEFAULT_PORT = 61000;
    final int DEFAULT_TIMEOUT = 5;
    final int DEFAULT_POOL_SIZE = 50;
    final String DEFAULT_WRAPPER_NAME = "Content";
    final boolean DEFAULT_NAT = false;
    final String DEFAULT_WAN_HOST = "0.0.0.0";
    final int DEFAULT_WAN_PORT = 61000;

    String host = attr.getNamedItem("host") != null ?
        attr.getNamedItem("host").getNodeValue() :
        DEFAULT_HOST;

    int port = attr.getNamedItem("port") != null ?
        stringToPort(attr.getNamedItem("port").getNodeValue(), DEFAULT_PORT) :
        DEFAULT_PORT;

    int timeout = attr.getNamedItem("timeout") != null ?
        stringToInt(attr.getNamedItem("timeout").getNodeValue(), DEFAULT_TIMEOUT) :
        DEFAULT_TIMEOUT;

    int pool_size = attr.getNamedItem("pool_size") != null ?
        stringToInt(attr.getNamedItem("pool_size").getNodeValue(), DEFAULT_POOL_SIZE) :
        DEFAULT_POOL_SIZE;

    String wrapper_name = attr.getNamedItem("wrapper_name") != null ?
        attr.getNamedItem("wrapper_name").getNodeValue() :
        DEFAULT_WRAPPER_NAME;

    boolean nat = attr.getNamedItem("nat") != null ?
        stringToBoolean(attr.getNamedItem("nat").getNodeValue(), DEFAULT_NAT) :
        DEFAULT_NAT;

    String wan_host = attr.getNamedItem("wan_host") != null ?
        attr.getNamedItem("wan_host").getNodeValue() :
        DEFAULT_WAN_HOST;

    int wan_port = attr.getNamedItem("wan_port") != null ?
        stringToInt(attr.getNamedItem("wan_port").getNodeValue(), DEFAULT_WAN_PORT) :
        DEFAULT_WAN_PORT;

    return new WSNotificationServer(
        host, port, Integer.toUnsignedLong(timeout), pool_size,
        wrapper_name, nat, wan_host, wan_port);
  }

  private static ProtocolServer createStomp(NamedNodeMap attr) {
    final String DEFAULT_HOST = "0.0.0.0";
    final int DEFAULT_PORT = 61613;

    String host = attr.getNamedItem("host") != null ?
        attr.getNamedItem("host").getNodeValue() :
        DEFAULT_HOST;

    int port = attr.getNamedItem("port") != null ?
        stringToPort(attr.getNamedItem("port").getNodeValue(), DEFAULT_PORT) :
        DEFAULT_PORT;

    return new STOMPProtocolServer(host, port);
  }
}

