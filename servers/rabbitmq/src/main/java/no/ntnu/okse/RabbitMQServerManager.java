package no.ntnu.okse;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig.Builder;
import io.arivera.oss.embedded.rabbitmq.PredefinedVersion;
import io.arivera.oss.embedded.rabbitmq.RabbitMqEnvVar;
import io.arivera.oss.embedded.rabbitmq.bin.RabbitMqPlugins;
import io.arivera.oss.embedded.rabbitmq.bin.plugins.Plugin;
import io.arivera.oss.embedded.rabbitmq.bin.plugins.Plugin.State;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.log4j.BasicConfigurator;

public class RabbitMQServerManager {

  private static EmbeddedRabbitMq rabbitMq;
  private static Logger logger = Logger.getLogger(RabbitMQServerManager.class.getName());


  public static void startRabbitMqBroker() {
    if (isRunning()) {
      logger.warning("RabbitMq broker already started");
      return;
    }

    logger.info("Initializing RabbitMq");

    EmbeddedRabbitMqConfig config = new Builder()
        .version(PredefinedVersion.V3_6_9)
        .envVar(RabbitMqEnvVar.NODENAME, "Okse")
        .envVar(RabbitMqEnvVar.CONF_ENV_FILE, "/etc/rabbitmq/rabbitmq")
        .rabbitMqServerInitializationTimeoutInMillis(5000)
        .defaultRabbitMqCtlTimeoutInMillis(8000)
        .downloadFolder(new File("/tmp/okse/"))
        .extractionFolder(new File("/tmp/okse/"))
        .build();

    try {
      Path confFile = Paths.get(config.getAppFolder().getAbsolutePath() + "/etc/rabbitmq/rabbitmq.config");
      Files.createDirectories(confFile.getParent());
      try {
        Files.createFile(confFile);
      } catch (FileAlreadyExistsException ignored) {
      }
      Files.copy(RabbitMQServerManager.class.getResourceAsStream("/rabbitmq.config"),
          confFile, StandardCopyOption.REPLACE_EXISTING);
      logger.info("copied config to app folder");
    } catch (IOException e) {
      e.printStackTrace();
    }

    rabbitMq = new EmbeddedRabbitMq(config);
    rabbitMq.start();

    // plugins assumes files are present, files are fetched after initial start, rebooted after plugins
    RabbitMqPlugins plugins = new RabbitMqPlugins(config);
    plugins.enable("rabbitmq_amqp1_0");
    plugins.enable("rabbitmq_mqtt");
    plugins.enable("rabbitmq_stomp");
    plugins.enable("rabbitmq_management");

    logger.info("Running RabbitMq " + config.getVersion());
    logPlugins(plugins.groupedList());

    //Runtime.getRuntime().addShutdownHook(new Thread(RabbitMQServerManager::stopRabbitMqBroker));
    //logger.info("Added shutdown hook for RabbitMq server");
  }

  private static void logPlugins(Map<Plugin.State, Set<Plugin>> groupedPlugins) {
    Set<String> activePlugins =
        groupedPlugins.get(State.RUNNING).stream()
            .map(Plugin::getName)
            .collect(Collectors.toSet());
    logger.info("RabbitMq broker initialized with plugins: " + String.join(",", activePlugins));
  }


  public static void stopRabbitMqBroker() {
    if (!isRunning()) {
      logger.warning("Error shutting down rabbitMq server, server not running");
      return;
    }
    rabbitMq.stop();
    rabbitMq = null;
    logger.info("RabbitMq server shutdown successfully");
  }

  public static boolean isRunning() {
    return rabbitMq != null;
  }

  public static void main(String[] args) {
    BasicConfigurator.configure(); // to remove log4j warnings
    try {
      startRabbitMqBroker();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(158);
    }
    // Shutdown
    Scanner scn = new Scanner(System.in);
    boolean _run = true;
    while (_run) {
      System.out.println("Enter 'stop' to shut down the rabbitMq broker");
      _run = !scn.nextLine().equals("stop");
    }
    stopRabbitMqBroker();
  }

}
