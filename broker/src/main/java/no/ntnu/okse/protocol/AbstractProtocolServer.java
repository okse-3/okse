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

package no.ntnu.okse.protocol;

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractProtocolServer implements ProtocolServer {

  // Holder fields for hostname and port
  protected int port;
  protected String host;

  // Run-state variables
  protected boolean _running;

  // Name and statistics
  protected String protocolServerType;
  protected final AtomicInteger totalRequests;
  protected final AtomicInteger totalBadRequests;
  protected final AtomicInteger totalErrors;
  protected final AtomicInteger totalMessagesReceived;
  protected final AtomicInteger totalMessagesSent;

  /**
   * Constructor that just initializes the fields to default values
   */
  protected AbstractProtocolServer() {
    port = 0;
    host = "";
    _running = false;
    protocolServerType = "";
    totalMessagesSent = new AtomicInteger(0);
    totalMessagesReceived = new AtomicInteger(0);
    totalRequests = new AtomicInteger(0);
    totalBadRequests = new AtomicInteger(0);
    totalErrors = new AtomicInteger(0);
  }

  // Logger singleton
  protected Logger log;

  // Server wrapping thread
  protected Thread _serverThread;

  /**
   * Total amount of requests from this WSNotificationServer that has passed through this server
   * instance.
   *
   * @return An integer representing the total amount of request.
   */
  public int getTotalRequests() {
    return totalRequests.get();
  }

  /**
   * Total amount of messages that has been sent through WSNotificationServer
   *
   * @return An integer representing the total amount of messages sent.
   */
  public int getTotalMessagesSent() {
    return totalMessagesSent.get();
  }

  /**
   * Total amount of messages that has been received on WSNotificationServer
   *
   * @return An integer representing the total amount of messages received.
   */
  public int getTotalMessagesReceived() {
    return totalMessagesReceived.get();
  }

  /**
   * This interface method must return the total amount of bad requests received by the protocol
   * server.
   *
   * @return An integer representing the total amount of received malformed or bad requests
   */
  public int getTotalBadRequests() {
    return totalBadRequests.get();
  }

  /**
   * This interface method must return the total amount of errors generated by the protocol server.
   *
   * @return An integer representing the total amount of errors in the protocol server.
   */
  public int getTotalErrors() {
    return totalErrors.get();
  }

  /**
   * Returns the Port of this ProtocolServer
   *
   * @return The Port the server is bound to
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Returns the Hostname of this ProtocolServer
   *
   * @return The Hostname the server is bound to
   */
  public String getHost() {
    return this.host;
  }

  public void incrementTotalMessagesSent() {
    totalMessagesSent.incrementAndGet();
  }

  public void incrementTotalMessagesReceived() {
    totalMessagesReceived.incrementAndGet();
  }

  public void incrementTotalRequests() {
    totalRequests.incrementAndGet();
  }

  public void incrementTotalBadRequest() {
    totalBadRequests.incrementAndGet();
  }

  public void incrementTotalErrors() {
    totalErrors.incrementAndGet();
  }

  public void decrementTotalErrors() {
    totalErrors.decrementAndGet();
  }

}
