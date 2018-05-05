/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.ntnu.okse.clients.wsn;

import no.ntnu.okse.clients.TestClient;
import org.apache.cxf.wsn.client.NotificationBroker;
import org.apache.log4j.Logger;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;
import org.apache.cxf.wsn.client.Consumer;
import org.apache.cxf.wsn.client.Subscription;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

public final class WSNClient implements TestClient {

  private static final Logger log = Logger.getLogger(WSNClient.class);
  private static final String DEFAULT_URL_EXTENSION = "";
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 61000;
  private final TestNotificationBroker notificationBroker;
  private final Map<String, Subscription> subscriptions = new HashMap<>();
  private final Map<String, Consumer> consumers = new HashMap<>();
  private Consumer.Callback callback;


  public WSNClient() {
    this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_URL_EXTENSION);
  }

  public WSNClient(String host, int port, String url_extension) {
    notificationBroker = new TestNotificationBroker(
        String.format("http://%s:%d/%s", host, port, url_extension));
  }

  @Override
  public void connect() {
    // Do nothing
  }

  @Override
  public void disconnect() {
    // Do nothing
  }

  @Override
  public void subscribe(String topic) {
    subscribe(topic, "localhost", 9001);
  }

  public void subscribe(String topic, String host, int port) {
    log.debug("Subscribing to topic: " + topic);
    Consumer consumer = new Consumer(callback,
        String.format("http://%s:%d/Consume/%s", host, port, topic));
    consumers.put(topic, consumer);
    try {
      subscriptions.put(topic, notificationBroker.subscribe(consumer, topic));
      log.debug("Subscribed to topic: " + topic);
    } catch (Exception e) {
      log.error("Failed to subscribe", e);
    }
  }

  @Override
  public void unsubscribe(String topic) {
    log.debug("Unsubscribing from topic: " + topic);
    if (subscriptions.containsKey(topic)) {
      try {
        Subscription subscription = subscriptions.get(topic);
        subscription.unsubscribe();
        log.debug("Unsubscribed from topic: " + topic);
      } catch (UnableToDestroySubscriptionFault | ResourceUnknownFault e) {
        log.error("Failed to unsubscribe", e);
      }
    } else {
      log.debug("Topic not found");
    }
    if (consumers.containsKey(topic)) {
      log.debug("Stopping consumer for topic: " + topic);
      Consumer consumer = consumers.get(topic);
      consumer.stop();
      log.debug("Consumer stopped");
    }
  }

  @Override
  public void publish(String topic, String content) {
    log.debug(String.format("Publishing to topic %s with content %s", topic, content));
    notificationBroker.notify(topic, new JAXBElement<>(new QName("string"), String.class, content));
    log.debug("Published message successfully");
  }

  public void setCallback(Consumer.Callback callback) {
    this.callback = callback;
  }
}
