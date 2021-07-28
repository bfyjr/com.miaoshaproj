package org.example.mq;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.CharSet;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.example.dao.StockLogDOMapper;
import org.example.dataobject.StockLogDO;
import org.example.error.BusinessException;
import org.example.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class MqProducer {

    private DefaultMQProducer mqProducer;
    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;
    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    OrderService orderService;

    @Autowired
    StockLogDOMapper stockLogDOMapper;


    @PostConstruct
    public void init() throws MQClientException {
        //做mqProducer初始化
        mqProducer=new DefaultMQProducer("producer_group");
        mqProducer.setNamesrvAddr(nameAddr);

        mqProducer.start();

        transactionMQProducer=new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object args) {
                Integer itemId= (Integer) ((Map)args).get("itemId");
                Integer userId= (Integer) ((Map)args).get("userId");
                Integer promoId= (Integer) ((Map)args).get("promoId");
                Integer amount= (Integer) ((Map)args).get("amount");
                String stockLogId=(String) ((Map)args).get("stockLogId");
                try {
                    StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    orderService.creatOrder(userId,itemId,promoId,amount,stockLogId);
                } catch (BusinessException e) {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                String jsonString=new String(messageExt.getBody());
                Map<String ,Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId= (Integer) map.get("itemId");
                Integer amount= (Integer) map.get("amount");
                String stockLogId= (String) map.get("stockLogId");

                StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if(stockLogDO==null){
                    return LocalTransactionState.UNKNOW;
                }
                if(stockLogDO.getStatus()==2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else if(stockLogDO.getStatus()==1){
                    return LocalTransactionState.UNKNOW;
                }else {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }
        });

    }

    //事务性同步扣库存
    public boolean transactionAsyncReduceStock(Integer userId,Integer promoId, Integer itemId,Integer amount,String stockLogId){
        Map<String,Object> bodyMap=new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        bodyMap.put("stockLogId",stockLogId);

        Map<String,Object> argsMap=new HashMap<>();
        argsMap.put("userId",userId);
        argsMap.put("promoId",promoId);
        argsMap.put("itemId",itemId);
        argsMap.put("amount",amount);
        argsMap.put("stockLogId",stockLogId);
        Message message=new Message(topicName,"increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult transactionSendResult=null;
        try {
             transactionSendResult= transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        if(transactionSendResult.getLocalTransactionState()==LocalTransactionState.ROLLBACK_MESSAGE)
        {
            return false;
        }else if(transactionSendResult.getLocalTransactionState()==LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }else {
            return false;
        }


    }

    //同步库存消息
    public boolean asyncReduceStock(Integer itemId,Integer amount){
        Map<String,Object> bodyMap=new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        Message message=new Message(topicName,"increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            mqProducer.send(message);
        } catch (MQClientException e) {
            return false;
        } catch (RemotingException e) {
            return false;
        } catch (MQBrokerException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

}
