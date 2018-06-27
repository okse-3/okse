/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 - 2018 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.db.DB;
import no.ntnu.okse.web.Server;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ntnunotif.wsnu.base.util.Log;

import java.io.File;
import java.time.Duration;
import java.util.Properties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@SpringBootApplication
public class Application {

  // Version
  public static final String VERSION = "3.0.0";

  // Initialization time
  public static long startedAt = System.currentTimeMillis();

  /* Default global fields */
  public static final String OKSE_SYSTEM_NAME = "OKSE System";
  public static boolean BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS = false;
  public static boolean CACHE_MESSAGES = true;
  public static long DEFAULT_SUBSCRIPTION_TERMINATION_TIME = 15552000000L; // Half a year
  public static long DEFAULT_PUBLISHER_TERMINATION_TIME = 15552000000L; // Half a year

  /* Public reference to the properties object for potential custom options */
  public static Properties config = new Properties();

  private static Logger log;
  public static CoreService cs;
  public static Server webserver;

  /**
   * Main method for the OKSE Message Broker Used to initiate the complete application (CoreService
   * and WebServer)
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) throws InterruptedException {
        /*
        // Check for presence of the needed config files, if they do not exist, they must be created
        Utilities.createConfigDirectoryAndFilesIfNotExists();
        // Configure the Log4j logger
        PropertyConfigurator.configure("config/log4j.properties");
        */
    // Reads config files, creates defaults if it does not exist, and updates fields
    readConfigurationFiles();

    // Init the logger
    log = Logger.getLogger(Application.class.getName());

    // Create a file handler for database file
    File dbFile = new File("okse.db");
    if (!dbFile.exists()) {
      DB.initDB();
      log.info("okse.db initiated");
    } else {
      log.info("okse.db exists");
    }

    // Initialize main system components
    webserver = new Server();
    cs = CoreService.getInstance();

    /* REGISTER CORE SERVICES HERE */
    cs.registerService(TopicService.getInstance());
    cs.registerService(MessageService.getInstance());
    cs.registerService(SubscriptionService.getInstance());

    // This must be done here, as Spring creates certificates that result in an unwanted setup of
    // the openfire server
    log.info("Booting optional protocol support servers");
    bootOptionalProtocolSupportServers();
    log.info("Completed optional protocol support servers");

    // Start the admin console
    webserver.run();

    // Start the CoreService
    log.info("Starting OKSE " + VERSION);
    cs.boot();
  }

  /**
   * Returns a Duration instance of the time the Application has been running
   *
   * @return The amount of time the application has been running
   */
  public static Duration getRunningTime() {
    return Duration.ofMillis(System.currentTimeMillis() - startedAt);
  }

  /**
   * Resets the time at which the system was initialized. This method can be used during a system
   * restart from
   */
  public static void resetStartTime() {
    startedAt = System.currentTimeMillis();
  }

  /**
   * This public static method reads the OKSE configuration file from disk. If it does not exist, it
   * is created. Additionally, default fields in the Application class are populated after config
   * has been read.
   *
   * @return The Properties object containing the config key/value pairs.
   */
  public static Properties readConfigurationFiles() {
    // Check for presence of the needed config files, if they do not exist, they must be created
    Utilities.createConfigDirectoryAndFilesIfNotExists();
    // Configure the Log4j logger
    PropertyConfigurator.configure("config/log4j.properties");
    // Read the contents of the configuration file
    config = Utilities.readConfigurationFromFile("config/okse.properties");
    // Initialize the internal fields from the defaults
    initializeDefaultsFromConfigFile(config);

    return config;
  }

  /**
   * Initializes/updates the default field values in this Application class from the configuration
   * file
   *
   * @param properties A Properties object containing values from the configuration file.
   */
  private static void initializeDefaultsFromConfigFile(Properties properties) {
    if (properties == null) {
      log.error("Failed to update variables from config file, using internal defaults.");
      return;
    }
    // Iterate over all the keys
    for (String option : properties.stringPropertyNames()) {
      option = option.toUpperCase();
      // Update values based on what keys are provided
      switch (option) {
        case "CACHE_MESSAGES":
          CACHE_MESSAGES = properties.getProperty(option).equalsIgnoreCase("true");
          break;
        case "BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS":
          BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS = properties.getProperty(option)
              .equalsIgnoreCase("true");
          break;
        case "DEFAULT_SUBSCRIPTION_TERMINATION_TIME":
          try {
            DEFAULT_SUBSCRIPTION_TERMINATION_TIME = Long.parseLong(properties.getProperty(option));
          } catch (NumberFormatException numEx) {
            log.error("Malformed subscription termination time, using internal default");
          }
          break;
        case "DEFAULT_PUBLISHER_TERMINATION_TIME":
          try {
            DEFAULT_PUBLISHER_TERMINATION_TIME = Long.parseLong(properties.getProperty(option));
          } catch (NumberFormatException numEx) {
            log.error("Malformed subscription termination time, using internal default");
          }
          break;
        case "ENABLE_WSNU_DEBUG_OUTPUT":
          if (properties.getProperty(option).equalsIgnoreCase("true")) {
            Log.setEnableDebug(true);
          } else {
            Log.setEnableDebug(false);
          }
          break;

      }
    }
  }

  private static void bootOptionalProtocolSupportServers() {
    try {
      bootOptionalProtocolSupportServers(new FileInputStream("config/optionalprotocolsupportservers.xml"));
    } catch (FileNotFoundException e) {
      log.error("config/optionalprotocolsupportservers.xml not found");
      e.printStackTrace();
    }
  }

  /**
   * Finds all optional protocol support servers that are going to be used and starts them
   *
   * @param configStream Inputstream of the optional protocol support servers file
   */
  private static void bootOptionalProtocolSupportServers(InputStream configStream) {
    NodeList servers;
    try {
      Document cfg;
      cfg = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configStream);
      servers = cfg.getElementsByTagName("server");
      if (servers != null) {
        for (int serverIndex = 0; serverIndex < servers.getLength(); serverIndex++) {
          Node serverNode = servers.item(serverIndex);
          NamedNodeMap attributes = serverNode.getAttributes();
          if (attributes.getNamedItem("use_other") != null &&
              attributes.getNamedItem("use_other").getNodeValue().equals("no") &&
              attributes.getNamedItem("type") != null) {
            createOptionalProtocolSupportServers(attributes.getNamedItem("type").getNodeValue());
          }
        }
      } else {
        log.error("No optional protocol servers.xml");
      }
    } catch (SAXException | IOException | ParserConfigurationException e) {
      log.error("ProtocolServer configuration parsing error, message: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Starts an optional protocol support server with the given name
   *
   * @param name The name of the optional protocol support server to start
   */
  private static void createOptionalProtocolSupportServers(String name) {
    switch (name) {
      case "rabbitmq":
        try {
          RabbitMQServerManager.startRabbitMqBroker();
        } catch (Exception e) {
          e.printStackTrace(); }
        break;
      case "openfire-xmpp":
        try {
          OpenfireXMPPServerFactory.start();
          // Let the XMPP server start before continuing
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
        break;
    }
  }

}
