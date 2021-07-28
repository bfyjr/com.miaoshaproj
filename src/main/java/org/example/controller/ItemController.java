package org.example.controller;

import org.example.controller.viewobject.ItemVO;
import org.example.error.BusinessException;
import org.example.response.CommonReturnType;
import org.example.service.CacheService;
import org.example.service.CacheServiceimpl;
import org.example.service.ItemServiceImpl;
import org.example.service.Model.ItemModel;
import org.example.service.PromoServiceImpl;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller()
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*",originPatterns ="*")
public class ItemController {
    @Autowired
    ItemServiceImpl itemService;

    @Autowired
    PromoServiceImpl promoService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    private CacheServiceimpl cacheService;

    @ResponseBody
    @RequestMapping("/createItem")
    //创建商品
    public CommonReturnType createItem(@RequestParam("title")String title,
                                       @RequestParam("description")String description,
                                       @RequestParam("price") BigDecimal price,
                                       @RequestParam("stock")Integer stock,
                                       @RequestParam("imgUrl")String imgUrl) throws BusinessException {
        ItemModel itemModel=new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);
        ItemModel item = itemService.createItem(itemModel);
        ItemVO itemVO=convertItemVOFromItemModel(item);

        return CommonReturnType.create(itemVO);
    }

    @ResponseBody
    @RequestMapping("/publishpromo")
    public CommonReturnType publishpromo(@RequestParam("id")Integer promoId) {
        promoService.publishPromo(promoId);
        return CommonReturnType.create(null);
    }

    @ResponseBody
    @RequestMapping("/get")
    public CommonReturnType getItem(@RequestParam("id")Integer id){
        ItemModel itemById=null;
        itemById= (ItemModel) cacheService.getFromCommonCache("item_"+id);
        if(itemById==null){
            //本地缓存没有就根据商品id到redis内获取
            itemById= (ItemModel) redisTemplate.opsForValue().get("item_"+id);
            //如果redis不存在该商品，则去数据库查找
            if(itemById==null){
                itemById = itemService.getItemById(id);
                //设置到redis内
                redisTemplate.opsForValue().set("item_"+id,itemById);
                //设置失效时间
                redisTemplate.expire("item_"+id,10, TimeUnit.MINUTES);
            }
            //填充本地缓存
            cacheService.setCommonCache("item_"+id,itemById);
        }

        ItemVO itemVO = convertItemVOFromItemModel(itemById);
        return CommonReturnType.create(itemVO);
    }
    @ResponseBody
    @RequestMapping("/list")
    public CommonReturnType listItem(){
        List<ItemModel> modelList = itemService.listItem();
        List<ItemVO> collect = modelList.stream().map(itemModel -> convertItemVOFromItemModel(itemModel)).collect(Collectors.toList());
        return CommonReturnType.create(collect);
    }



    private ItemVO convertItemVOFromItemModel(ItemModel itemModel){
        if(itemModel==null) return  null;
        ItemVO itemVO=new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        if(itemModel.getPromoModel()!=null){
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }

}
