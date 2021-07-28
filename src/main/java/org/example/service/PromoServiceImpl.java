package org.example.service;


import org.example.dao.PromoDOMapper;
import org.example.dataobject.PromoDO;
import org.example.error.BusinessException;
import org.example.error.EnumBusinessError;
import org.example.service.Model.ItemModel;
import org.example.service.Model.PromoModel;
import org.example.service.Model.UserModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService{
    @Autowired
    PromoDOMapper promoDOMapper;
    @Autowired
    ItemService itemService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserService userService;
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertModelFromPromoDO(promoDO);
        if(promoModel==null){
            return null;
        }
        //判断当前秒杀活动是否进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        //通过活动id获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if(promoDO.getItemId()==null|promoDO.getItemId().intValue()==0)
        {
            return;
        }
        ItemModel itemById = itemService.getItemById(promoDO.getItemId());
        redisTemplate.opsForValue().set("promo_item_stock_"+itemById.getId(),itemById.getStock());

        //设置秒杀大闸值，设置为5倍库存
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemById.getStock().intValue()*5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId) {

        if(redisTemplate.hasKey("promo_item_stock_invalid"+itemId)){
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertModelFromPromoDO(promoDO);
        if(promoModel==null){
            return null;
        }
        //判断当前秒杀活动是否进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }
        //判断活动是否正在进行
        if(promoModel.getStatus()!=2){
            return null;
        }
        //判断item是否存在
        ItemModel itemById=itemService.getItemByIdInCache(itemId);
        if(itemById==null){
            return null;
        }
        //判断用户是否存在
        UserModel userById = userService.getUserById(userId);
        if(userById==null){
            return null;
        }
        String token= UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userId_"+userId+"_itemId_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_userId_"+userId+"_itemId_"+itemId,5, TimeUnit.MINUTES);
        return token;
    }

    private PromoModel convertModelFromPromoDO(PromoDO promoDO){
        if(promoDO==null){
            return null;
        }
        PromoModel promoModel=new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));

        return promoModel;
    }
}
