package com.INFO7255.Service;

import com.INFO7255.Repository.QueueRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

@Service
public class PublisherService {
    
    @Autowired
    QueueRepository queueRepository;
    // @Autowired
    // ConsumerService consumerService;
    String listName = "IndexQueue";
    public void publishMessage(String msg){
        Long queueLength = queueRepository.addToList(listName, msg);
        System.out.println("Successfully pushed to queue " +  queueLength);
    }
}
