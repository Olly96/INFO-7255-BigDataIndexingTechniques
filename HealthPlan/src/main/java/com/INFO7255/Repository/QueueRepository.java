package com.INFO7255.Repository;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

@Service
public class QueueRepository {
    private Jedis jedis;
    QueueRepository(){
        jedis = new Jedis("localhost", 6379);
    }

    public Long addToList(String listName, String msg){
        System.out.println(jedis.llen(listName));
        return jedis.lpush(listName, msg);
    }

    public String popFromList(String listName){
        return jedis.lpop(listName);
    }

}
