package com.INFO7255.Repository;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.apache.commons.codec.digest.DigestUtils;

import redis.clients.jedis.Jedis;

@Repository
public class HealthPlanRepository {
	Jedis jedis;
	
	public HealthPlanRepository() {
		jedis = new Jedis("localhost", 6379);
		
	}

	public void saveKeyVal(String key, String val) {
		jedis.set(key, val);
	}
	
	public void addToSet(String setKey, String val){
		jedis.sadd(setKey, val);
	}

	public Set<String> getAllKeysInSet(String setKey){
		return jedis.smembers(setKey);
	}

	public String getKey(String key) {
		return jedis.get(key);
	}
	
	public Map<String, String> getAllPlans(String key) {
            return jedis.hgetAll(key);
	}

	public Boolean checkIfHashNodeExists(String key) {
			return jedis.exists(key);
		
	}
	
	// public String getEtag(String key, String field) {
	// 	return jedis.hget(key, field);
	// }

	public void setHashValue(String key, String field, String plan) {
		jedis.hset(key, field, plan);
		// String etagVal = DigestUtils.md5Hex(plan);
		// jedis.hset(key+"_ETAG", field, etagVal);
		// return etagVal;
		
	}

	public String getHashKey(String key, String field) {
		return jedis.hget(key, field);
	}

	public void deleteHashKey(String key) {
		jedis.del(key);
	}

	
	
}
