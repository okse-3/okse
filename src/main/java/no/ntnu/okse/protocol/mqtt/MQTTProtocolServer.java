package no.ntnu.okse.protocol.mqtt;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

public class MQTTProtocolServer extends AbstractProtocolServer {
	protected static final String SERVERTYPE = "mqtt";

	private MQTTServer server;

	public MQTTProtocolServer(String host, Integer port) {
		this.host = host;
		this.port = port;
		log = Logger.getLogger(MQTTProtocolServer.class.getName());
	}

	@Override
	public void boot() {
		if(!_running) {
			server = new MQTTServer(this, host, port);
			_serverThread = new Thread(this::run);
			_serverThread.setName("MQTTProtocolServer");
			_serverThread.start();
			_running = true;
			log.info("MQTTProtocolServer booted successfully");
		}
	}

	@Override
	public void run() {
		server.start();
	}

	@Override
	public void stopServer() {
		log.info("Stopping MQTTProtocolServer");
		server.stopServer();
		_running = false;
		server = null;
		log.info("MQTTProtocolServer is stopped");
	}

	@Override
	public String getProtocolServerType() {
		return SERVERTYPE;
	}

	@Override
	public void sendMessage(Message message) {
		log.info("Received message on topic " + message.getMessage() );
		server.sendMessage( message );

	}

	public void incrementTotalRequests() {
		totalRequests.incrementAndGet();
	}

	public void incrementTotalBadRequests() {
		totalBadRequests.incrementAndGet();
	}

	public void incrementTotalErrors() {
		totalErrors.incrementAndGet();
	}

	public void incrementTotalMessagesReceived() {
		totalMessagesReceived.incrementAndGet();
	}

	public void incrementTotalMessagesSent() {
		totalMessagesSent.incrementAndGet();
	}



	public boolean isRunning() {
		return _running;
	}
}
