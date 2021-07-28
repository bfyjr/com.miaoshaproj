package org.example.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceimpl implements CacheService{

    private Cache<String,Object> commonCache=null;

    @PostConstruct
    public void init(){
        commonCache= CacheBuilder.newBuilder()
                .initialCapacity(100)  //设置初始容量
                .maximumSize(1000)       //设置最大的对象个数，超出就清除
                //设置相对于写之后的超时时间
                .expireAfterWrite(2, TimeUnit.MINUTES).build();
    }
    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key,value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
