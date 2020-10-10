package com.eden.seckill.common.redis;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisHashUtil {

    @Resource
    private RedisTemplate redisTemplate;

    //设置一个值
    //参数: hash名字，hash中的key,hash中key对应的value
    public boolean setHashValue(String hashName,String hashKey,Object hashValue) {

        boolean flag = false;
        try{
            System.out.println("=============================:hashName:"+hashName);
            redisTemplate.opsForHash().put(hashName, hashKey, hashValue);
            flag = true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    //判断一个hash中的某个key是否存在
    //参数:hash名字，hash中的key
    public boolean isHashkey(String hashName,String hashkey) {
        boolean bool = redisTemplate.opsForHash().hasKey(hashName, hashkey);
        return bool;
    }

    //得到某个hash中的某个key的值
    //参数:hash名字，hash中的key
    public Object getHashValue(String hashName,String hashkey) {
        //boolean flag = false;

        Object value=redisTemplate.opsForHash().get(hashName, hashkey);

        return value;
    }

}
