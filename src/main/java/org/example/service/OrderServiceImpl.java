package org.example.service;

import org.example.dao.OrderDOMapper;
import org.example.dao.StockLogDOMapper;
import org.example.dataobject.OrderDO;
import org.example.dataobject.StockLogDO;
import org.example.error.BusinessException;
import org.example.error.EnumBusinessError;
import org.example.mq.MqConsumer;
import org.example.mq.MqProducer;
import org.example.service.Model.ItemModel;
import org.example.service.Model.OrderModel;
import org.example.service.Model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    ItemService itemService;

    @Autowired
    UserService userService;

    @Autowired
    OrderDOMapper orderDOMapper;

    @Autowired
    StockLogDOMapper stockLogDOMapper;



    @Override
    @Transactional
    public OrderModel creatOrder(Integer userId, Integer itemId,Integer promoId,Integer amount,String stockLogId) throws BusinessException {
        //1校验下单状态，用户是否合法，商品是否存在，数量是否正确等
//        ItemModel itemById = itemService.getItemById(itemId);
        ItemModel itemById=itemService.getItemByIdInCache(itemId);
//        if(itemById==null){
//            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
//        }
//        UserModel userById = userService.getUserById(userId);
//        if(userById==null){
//            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
//        }
        if(amount<=0 || amount>1000){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"购买数量不正确");
        }
//        //校验活动信息
//        if(promoId!=null){
//            //校验前端传入的活动id是否等于传回来的商品本身属于的活动id
//            if(promoId.intValue()!=itemById.getPromoModel().getId()){
//                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//            }
//            //校验活动是否开始
//            if(itemById.getPromoModel().getStatus()!=2){
//                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"活动未开始");
//            }
//        }
        //减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result){
            throw new BusinessException(EnumBusinessError.STOCK_NOT_ENOUGH);
        }
        //订单入库
        OrderModel orderModel=new OrderModel();
        orderModel.setUserid(userId);
        orderModel.setItemid(itemId);
        orderModel.setAmount(amount);
        if(promoId!=null){
            orderModel.setItemPrice(itemById.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemById.getPrice());
        }

        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        //生成交易流水号，订单号
        orderModel.setId(generateOrderNo());
//        System.out.println(orderModel.getUserid()+" "+orderModel.getItemid());
        OrderDO orderDO = convertOrderDOFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);
        itemService.increaseSales(itemId,amount);

        StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if(stockLogDO==null)
        {
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);



//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//            @Override
//            public void afterCommit() {
//                //f返回前端之前最后一步发送异步更新库存消息
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
////                if(!mqResult){
////                    itemService.rollBackStock(itemId,amount);
////                    throw new BusinessException(EnumBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });

        return orderModel;
    }

//    public static void main(String[] args) {
//
//    }

    private String generateOrderNo(){
        StringBuilder sb=new StringBuilder();
        //16位，前八位时间信息，中间6位自增序列(暂时使用随机数代替)，最后两位分库表
        LocalDateTime now=LocalDateTime.now();
        String nowDate=now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        sb.append(nowDate);
        sb.append((int) (Math.random() * 1000000));
        sb.append("00");
        return sb.toString();
    }

    OrderDO convertOrderDOFromOrderModel(OrderModel orderModel){
        if(orderModel==null){
            return null;
        }
        OrderDO orderDO=new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);

        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        orderDO.setUserId(orderModel.getUserid());
        orderDO.setItemId(orderModel.getItemid());
        return orderDO;
    }
}
