package no.ntnu.okse.protocol.amqp091;

import fr.dyade.aaa.agent.AgentServer;
import no.ntnu.okse.core.messaging.Message;
import org.apache.log4j.Logger;
import org.ow2.joram.mom.amqp.AMQPService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AMQP 0.9.1 wrapper for AMQPService
 */
public class AMQP091Service {

  private static final Logger log = Logger.getLogger(AMQP091Service.class.getName());
  private final AMQP091MessageListener messageListener;
  private final AMQP091ProtocolServer protocolServer;
  private final LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();;
  private Thread messageSenderThread;
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Dependency injection constructor for protocol server
   *
   * @param amqp091ProtocolServer protocol server
   */
  public AMQP091Service(AMQP091ProtocolServer amqp091ProtocolServer) {
    protocolServer = amqp091ProtocolServer;
    messageListener = new AMQP091MessageListener(amqp091ProtocolServer);
  }

  /**
   * Start server
   */
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    log.debug("AMQP 0.9.1 service is starting");
    try {
      AgentServer.init((short) 0, createAgentFolder(), null);
    } catch (Exception e) {
      log.error("An exception was thrown when starting AMQP 0.9.1 Agent Server", e);
      protocolServer.incrementTotalErrors();
    }
    try {
      AMQPService.init(getHostPort(), true);
    } catch (Exception e) {
      log.error("An exception was thrown when starting AMQP 0.9.1 service", e);
      protocolServer.incrementTotalErrors();
    }
    AMQPService.addMessageListener(messageListener);
    AMQPService.setPublishing(false);

    messageSenderThread = new Thread(() -> {
      while (running.get()) {
        try {
          Message message = messageQueue.take();
          AMQPService.internalPublish(message.getTopic(), "",
              message.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
          log.info("AMQP 0.9.1 message queue interrupted, stopping?");
        }

      }
    });
    messageSenderThread.start();

    log.debug("AMQP 0.9.1 service started successfully");
  }

  /**
   * Stop server
   */
  public void stop() {
    log.debug("AMQP 0.9.1 service is stopping");
    AMQPService.stopService();
    AgentServer.stop();
    running.set(false);
    messageSenderThread.interrupt();
    log.debug("AMQP 0.9.1 service stopped");
  }

  /**
   * Create host and port message for AMQPService
   *
   * @return string containing host and port
   */
  private String getHostPort() {
    return "" + protocolServer.getPort() + " " + protocolServer.getHost();
  }

  /**
   * Create folder for agent server
   *
   * @return folder name
   */
  private String createAgentFolder() {
    String directoryName = "agent-server";
    try {
      Path agentFolder = Files.createTempDirectory(directoryName);
      return agentFolder.toString();
    } catch (IOException e) {
      log.warn("Failed to create temporary folder, creating folder in local directory");
      File localDirectory = new File(directoryName);
      if (!localDirectory.exists()) {
        localDirectory.mkdir();
      }
      return directoryName;
    }
  }

  /**
   * Internally publish message to AMQP 0.9.1
   *
   * @param message message
   */
  public void sendMessage(Message message) {
    messageQueue.add(message);
  }
}
