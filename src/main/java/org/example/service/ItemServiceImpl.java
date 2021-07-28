package org.example.service;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.example.dao.ItemDOMapper;
import org.example.dao.ItemStockDOMapper;
import org.example.dao.StockLogDOMapper;
import org.example.dataobject.ItemDO;
import org.example.dataobject.ItemStockDO;
import org.example.dataobject.StockLogDO;
import org.example.error.BusinessException;
import org.example.error.EnumBusinessError;
import org.example.mq.MqConsumer;
import org.example.mq.MqProducer;
import org.example.service.Model.ItemModel;
import org.example.service.Model.PromoModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImplement;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService{
    @Autowired
    private ValidatorImplement validator;
    @Autowired
    private ItemDOMapper itemDOMapper;
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    @Autowired
    PromoServiceImpl promoService;

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    MqProducer mqProducer;

    @Autowired
    MqConsumer mqConsumer;

    @Autowired
    StockLogDOMapper stockLogDOMapper;


    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        ValidationResult validate = validator.validate(itemModel);
        if(validate.isHasErrors()){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,validate.getErrorMsg());
        }
        ItemDO itemDO = convertItemDoFromItemModel(itemModel);
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());

        ItemStockDO itemStockDO=convertItemStockDOFromModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);
        return this.getItemById(itemModel.getId());
    }

    private ItemStockDO convertItemStockDOFromModel(ItemModel itemModel){
        if(itemModel==null){
            return null;
        }
        ItemStockDO itemStockDO=new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }

    private ItemDO convertItemDoFromItemModel(ItemModel itemModel){
        if(itemModel==null){
            return null;
        }
        ItemDO itemDO=new ItemDO();
        BeanUtils.copyProperties(itemModel,itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOS = itemDOMapper.listItem();
        List<ItemModel> collect = itemDOS.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return collect;

    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if(itemDO==null) return null;
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
        ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);
        PromoModel promoByItemId = promoService.getPromoByItemId(itemModel.getId());
        if(promoByItemId!=null && promoByItemId.getStatus()!=3){
            itemModel.setPromoModel(promoByItemId);
        }

        return itemModel;
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult = mqProducer.asyncReduceStock(itemId, amount);
        return mqResult;
    }

    @Override
    public boolean rollBackStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if(itemModel==null){
            itemModel=this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id,itemModel);
            redisTemplate.expire("item_validate_" + id,10, TimeUnit.MINUTES);
        }
        return itemModel;
    }


    @Override
    @Transactional
    public boolean decreaseStock(Integer itemid, Integer amount) throws BusinessException {
//        int affectedRows = itemStockDOMapper.decreaseStock(itemid, amount);
//        System.out.println(affectedRows);
        if(redisTemplate.opsForValue().get("promo_item_stock_" + itemid)==null){
            int i = itemStockDOMapper.decreaseStock(itemid, amount);
            return true;
        }
        long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemid, amount.intValue() * -1);
        if(result>0){

//            if(!mqResult){
//                redisTemplate.opsForValue().increment("promo_item_stock_" + itemid, amount.intValue());
//                return false;
//            }
            return true;
        }else if(result==0){
            //标识库存已经售罄
            redisTemplate.opsForValue().set("promo_item_stock_invalid"+itemid,"true");
            return true;
        }
        else{
            rollBackStock(itemid,amount);
            return false;
        }
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemDOMapper.increaseSales(amount,itemId);
    }

    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO=new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-",""));
        stockLogDO.setStatus(1);
        stockLogDOMapper.insertSelective(stockLogDO);

        return stockLogDO.getStockLogId();


    }

    private ItemModel convertModelFromDataObject(ItemDO itemDO,ItemStockDO itemStockDO)
    {
        ItemModel itemModel=new ItemModel();
        BeanUtils.copyProperties(itemDO,itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }
}
