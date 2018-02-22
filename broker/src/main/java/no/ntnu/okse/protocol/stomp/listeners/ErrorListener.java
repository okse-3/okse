package no.ntnu.okse.protocol.stomp.listeners;

import asia.stampy.common.gateway.HostPort;
import asia.stampy.common.message.StampyMessage;
import asia.stampy.common.message.StompMessageType;
import asia.stampy.common.message.interceptor.StampyOutgoingMessageInterceptor;
import no.ntnu.okse.protocol.stomp.STOMPProtocolServer;

public class ErrorListener implements StampyOutgoingMessageInterceptor {
    private STOMPProtocolServer protocolServer;

    @Override
    public StompMessageType[] getMessageTypes() {
        return new StompMessageType[]{StompMessageType.ERROR};
    }

    @Override
    public boolean isForMessage(StampyMessage<?> message) {
        return true;
    }

    @Override
    public void interceptMessage(StampyMessage<?> message) {
        protocolServer.incrementTotalBadRequests();
    }

    @Override
    public void interceptMessage(StampyMessage<?> message, HostPort hostPort) {
        protocolServer.incrementTotalBadRequests();
    }

    public void setProtocolServer(STOMPProtocolServer protocolServer) {
        this.protocolServer = protocolServer;
    }
}
