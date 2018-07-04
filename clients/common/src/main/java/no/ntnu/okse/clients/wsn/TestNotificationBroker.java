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

import static org.apache.cxf.wsn.jms.JmsTopicExpressionConverter.SIMPLE_DIALECT;

import javax.xml.bind.JAXBElement;
import org.apache.cxf.wsn.client.Consumer;
import org.apache.cxf.wsn.client.NotificationBroker;
import org.apache.cxf.wsn.client.Referencable;
import org.apache.cxf.wsn.client.Subscription;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;

public class TestNotificationBroker extends NotificationBroker {

  public TestNotificationBroker(String address, Class<?>... cls) {
    super(address, cls);
  }

  public Subscription subscribe(Consumer consumer, String topic) throws Exception {
    Subscribe subscribeRequest = new Subscribe();
    subscribeRequest.setConsumerReference(consumer.getEpr());
    subscribeRequest.setFilter(new FilterType());
    TopicExpressionType topicExp = new TopicExpressionType();
    topicExp.setDialect("http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple");
    topicExp.getContent().add(topic);
    subscribeRequest.getFilter().getAny()
        .add(new JAXBElement<>(QNAME_TOPIC_EXPRESSION, TopicExpressionType.class, topicExp));

    SubscribeResponse response = getBroker().subscribe(subscribeRequest);
    return new Subscription(response.getSubscriptionReference());
  }

  public void notify(String topic, Object msg) {
    notify(null, topic, msg);
  }

  public void notify(Referencable publisher, String topic, Object msg) {
    getBroker();

    Notify notify = new Notify();
    NotificationMessageHolderType holder = new NotificationMessageHolderType();
    if (publisher != null) {
      holder.setProducerReference(publisher.getEpr());
    }
    if (topic != null) {
      TopicExpressionType topicExp = new TopicExpressionType();
      topicExp.setDialect("http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple");
      topicExp.getContent().add(topic);
      holder.setTopic(topicExp);
    }
    holder.setMessage(new NotificationMessageHolderType.Message());
    holder.getMessage().setAny(msg);
    notify.getNotificationMessage().add(holder);
    getBroker().notify(notify);
  }
}
