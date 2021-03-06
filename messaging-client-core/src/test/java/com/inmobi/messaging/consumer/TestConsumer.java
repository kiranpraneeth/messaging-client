package com.inmobi.messaging.consumer;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.inmobi.messaging.ClientConfig;
import com.inmobi.messaging.Message;

public class TestConsumer {
  private Date now = new Date(System.currentTimeMillis());

  @Test
  public void test() throws Exception {
    ClientConfig conf = new ClientConfig();
    conf.set(MessageConsumerFactory.CONSUMER_CLASS_NAME_KEY,
        MockConsumer.class.getName());
    conf.set(MessageConsumerFactory.TOPIC_NAME_KEY, "test");
    conf.set(MessageConsumerFactory.CONSUMER_NAME_KEY, "testconsumer");
    AbstractMessageConsumer consumer =
        (AbstractMessageConsumer) MessageConsumerFactory.create(conf);
    doTest(consumer, null);
    Assert.assertNull(consumer.getStartTime());
  }

  @Test
  public void testLoadFromClasspath() throws Exception {
    AbstractMessageConsumer consumer =
        (AbstractMessageConsumer) MessageConsumerFactory.create();
    doTest(consumer, null);
  }

  @Test
  public void testLoadFromFileName() throws Exception {
    URL url = getClass().getClassLoader().getResource(
        MessageConsumerFactory.MESSAGE_CLIENT_CONF_FILE);
    AbstractMessageConsumer consumer =
        (AbstractMessageConsumer) MessageConsumerFactory.create(
            url.getFile());
    doTest(consumer, null);
  }

  @Test
  public void testLoadFromClassName() throws Exception {
    ClientConfig conf = new ClientConfig();
    conf.set(MessageConsumerFactory.TOPIC_NAME_KEY, "test");
    conf.set(MessageConsumerFactory.CONSUMER_NAME_KEY, "testconsumer");
    AbstractMessageConsumer consumer = 
      (AbstractMessageConsumer) MessageConsumerFactory.create(
          conf, MockConsumer.class.getName());
    doTest(consumer, null);
  }

  @Test
  public void testTopicNConsumerName() throws Exception {
    AbstractMessageConsumer consumer = 
      (AbstractMessageConsumer) MessageConsumerFactory.create(
          new ClientConfig(), MockConsumer.class.getName(), "test",
          "testconsumer");
    doTest(consumer, null);
  }

  @Test
  public void testWithStartTime() throws Exception {
    ClientConfig conf = new ClientConfig();
    conf.set(MessageConsumerFactory.CONSUMER_CLASS_NAME_KEY,
        MockConsumer.class.getName());
    conf.set(MessageConsumerFactory.TOPIC_NAME_KEY, "test");
    conf.set(MessageConsumerFactory.CONSUMER_NAME_KEY, "testconsumer");
    AbstractMessageConsumer consumer =
        (AbstractMessageConsumer) MessageConsumerFactory.create(conf, now);
    doTest(consumer, now);
  }

  @Test
  public void testLoadFromClasspathWithStartTime() throws Exception {
    AbstractMessageConsumer consumer =
        (AbstractMessageConsumer) MessageConsumerFactory.create(now);
    doTest(consumer, now);
  }

  @Test
  public void testLoadFromFileNameWithStartTime() throws Exception {
    URL url = getClass().getClassLoader().getResource(
        MessageConsumerFactory.MESSAGE_CLIENT_CONF_FILE);
    AbstractMessageConsumer consumer =
        (AbstractMessageConsumer) MessageConsumerFactory.create(
            url.getFile(), now);
    doTest(consumer, now);
  }

  @Test
  public void testLoadFromClassNameWithStartTime() throws Exception {
    ClientConfig conf = new ClientConfig();
    conf.set(MessageConsumerFactory.TOPIC_NAME_KEY, "test");
    conf.set(MessageConsumerFactory.CONSUMER_NAME_KEY, "testconsumer");
    AbstractMessageConsumer consumer = 
      (AbstractMessageConsumer) MessageConsumerFactory.create(
          conf, MockConsumer.class.getName(), now);
    doTest(consumer, now);
  }

  @Test
  public void testTopicNConsumerNameWithStartTime() throws Exception {
    AbstractMessageConsumer consumer = 
      (AbstractMessageConsumer) MessageConsumerFactory.create(
          new ClientConfig(), MockConsumer.class.getName(), "test",
          "testconsumer", now);
    doTest(consumer, now);
  }

  @Test
  public void testStartTime() throws IOException, InterruptedException {
    AbstractMessageConsumer consumer = 
      (AbstractMessageConsumer) MessageConsumerFactory.create(
          new ClientConfig(), MockConsumer.class.getName(), "test",
          "testconsumer", now);
    doTest(consumer, now);
  }

  private void doTest(AbstractMessageConsumer consumer, Date startTime) 
      throws InterruptedException {
    Assert.assertTrue(consumer instanceof MockConsumer);
    Assert.assertFalse(consumer.isMarkSupported());
    Assert.assertTrue(((MockConsumer)consumer).initedConf);
    Assert.assertEquals(consumer.getTopicName(), "test");
    Assert.assertEquals(consumer.getConsumerName(), "testconsumer");
    Assert.assertEquals(consumer.getStartTime(), startTime);
    
    Message msg = consumer.next();
    consumer.close();
    Assert.assertEquals(new String(msg.getData().array()), MockConsumer.mockMsg);
  }
  
}
