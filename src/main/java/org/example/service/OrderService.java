package org.example.service;

import org.example.error.BusinessException;
import org.example.service.Model.OrderModel;

public interface OrderService {

    OrderModel creatOrder(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId) throws BusinessException;

}
