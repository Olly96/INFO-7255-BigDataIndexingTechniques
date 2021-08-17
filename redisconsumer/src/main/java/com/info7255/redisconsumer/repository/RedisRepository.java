package com.info7255.redisconsumer.repository;

import java.util.Set;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

@Service
public class RedisRepository {
  
    private Jedis jedis;
    RedisRepository(){
        jedis = new Jedis("localhost", 6379);
    }

    public String popFromList(String listName){
        String foo = "foo";
        try{
            foo = jedis.lpop(listName);
        } catch(Exception e){
            System.out.println("Exception1:" + e.getMessage());
        }
        // System.out.println("foo " +  foo);
        // try{
        //     jedis.lpush(listName, foo);
        // } catch(Exception e){
        //     System.out.println("Exception2:" + e.getMessage());
        // }
        if(foo != null){
            System.out.println("foo:" + foo);
        }
        return foo;
    }

    public String getKey(String key){
        return jedis.get(key);
    }

    public String getHashKey(String key, String field) {
		return jedis.hget(key, field);
	}

    public Set<String> getAllKeysInSet(String setKey){
		return jedis.smembers(setKey);
	}

}
