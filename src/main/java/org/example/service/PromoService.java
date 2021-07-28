package org.example.service;

import org.example.service.Model.PromoModel;

public interface PromoService {
    PromoModel getPromoByItemId(Integer itemId);

    void publishPromo(Integer promoId);

    //生成秒杀令牌
    String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId);

}
