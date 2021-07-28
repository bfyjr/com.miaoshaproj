package org.example.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.example.dao.ItemStockDOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class MqConsumer {

    private DefaultMQPushConsumer mqPushConsumer;
    @Value("${mq.nameserver.addr}")
    private String nameAddr;
    @Value("${mq.topicname}")
    private String topicName;
    @Autowired
    ItemStockDOMapper itemStockDOMapper;
    @PostConstruct
    public void init() throws MQClientException {
        mqPushConsumer=new DefaultMQPushConsumer("stock_consumer_group");
        mqPushConsumer.setNamesrvAddr(nameAddr);
        mqPushConsumer.subscribe(topicName,"*");
        mqPushConsumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                //实现库存真正扣减到数据库
                MessageExt messageExt = list.get(0);
                String jsonString=new String(messageExt.getBody());
                Map<String ,Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId= (Integer) map.get("itemId");
                Integer amount= (Integer) map.get("amount");
                itemStockDOMapper.decreaseStock(itemId,amount);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        mqPushConsumer.start();

    }
}
