package org.example.controller;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.example.error.BusinessException;
import org.example.error.EnumBusinessError;
import org.example.mq.MqProducer;
import org.example.response.CommonReturnType;
import org.example.service.ItemService;
import org.example.service.Model.OrderModel;
import org.example.service.Model.UserModel;
import org.example.service.OrderService;
import org.example.service.PromoServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*",originPatterns ="*")
public class OrderController {
    public final static String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";
    @Autowired
    OrderService orderService;

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    MqProducer mqProducer;

    @Autowired
    ItemService itemService;

    @Autowired
    PromoServiceImpl promoService;

    // 线程池
    private ExecutorService executorService;

    private RateLimiter rateLimiter;

    public void init(){
        executorService= Executors.newFixedThreadPool(20);
        rateLimiter=RateLimiter.create(200);
    }


    //生成秒杀令牌
    @RequestMapping(value = "/generateToken",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(value = "promoId")Integer promoId
            ,@RequestParam("itemId")Integer itemId) throws BusinessException{
        //判断用户是否登录
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN,"用户未登录，不能下单");
        }
        UserModel userModel= (UserModel) redisTemplate.opsForValue().get(token);
//        Boolean is_login = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(userModel==null)
        {
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN,"用户未登录，不能下单");
        }
        //获取秒杀闸看是否发放令牌
        long increment = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if(increment<0){
            return null;
        }
        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if(promoToken==null){
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀参数不合法,生成令牌失败");
        }
        return CommonReturnType.create(promoToken);


    }

    //封装下单请求
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(value = "promoId",required = false)Integer promoId,
                                        @RequestParam("itemId")Integer itemId,
                                        @RequestParam("amount")Integer amount,
                                        @RequestParam(value = "promoToken",required = false)String promoToken) throws BusinessException {
        if(!rateLimiter.tryAcquire()){
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR,"活动太火爆，令牌桶爆满");
        }
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN,"用户未登录，不能下单");
        }
        UserModel userModel= (UserModel) redisTemplate.opsForValue().get(token);
//        Boolean is_login = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(userModel==null)
        {
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN,"用户未登录，不能下单");
        }

        //校验秒杀令牌是否正确
        if(promoId!=null)
        {
            String inRedisPromoToken = (String) redisTemplate.opsForValue()
                    .get("promo_token_"+promoId+"_userId_"+userModel.getId()+"_itemId_"+itemId);
            if(inRedisPromoToken==null){
                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
            if(!StringUtils.equals(promoToken,inRedisPromoToken)){
                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
        }
//        UserModel userModel=(UserModel)httpServletRequest.getSession().getAttribute("LOGIN_USER");
//        OrderModel orderModel = orderService.creatOrder(userModel.getId(),itemId,promoId, amount);
        //库存售罄则下单失败
        if(redisTemplate.hasKey("promo_item_stock_invalid"+itemId)){
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR,"下单失败，已经售罄");
        }

        Future<Object> future = executorService.submit(new Callable<Object>() {


            @Override
            public Object call() throws Exception {
                //下面两个步骤是真正耗费系统性能的
                //加入库存流水init初始状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                //事务型消息
                boolean orderResult = mqProducer.transactionAsyncReduceStock(userModel.getId(), promoId, itemId, amount, stockLogId);
                if (!orderResult) {
                    throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR, "下单失败");
                }
                return null;
            }
        });

        try{
            future.get();
        }catch (Exception e){
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR);
        }


        return CommonReturnType.create(null);
    }
}
