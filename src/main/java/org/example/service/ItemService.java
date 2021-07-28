package org.example.service;

import org.example.error.BusinessException;
import org.example.service.Model.ItemModel;
import org.example.validator.ValidatorImplement;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public interface ItemService {
    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);

    //减库存
    boolean asyncDecreaseStock(Integer itemId,Integer amount);
    //库存回滚
    boolean rollBackStock(Integer itemId,Integer amount);

    ItemModel getItemByIdInCache(Integer id);

    boolean decreaseStock(Integer itemid,Integer amount) throws BusinessException;

    void increaseSales(Integer itemId,Integer amount);

    String initStockLog(Integer itemId,Integer amount);

}
