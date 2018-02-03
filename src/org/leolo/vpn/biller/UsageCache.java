package org.leolo.vpn.biller;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsageCache {
	
	private static UsageCache instance = null;
	Logger logger = LoggerFactory.getLogger(UsageCache.class);
	public static UsageCache getInstance(){
		if(instance==null){
			instance = new UsageCache();
		}
		return instance;
	}
	
	Hashtable<Integer, Entry> cache;
	
	private UsageCache(){
		cache = new Hashtable<>();
	}
	
	public int update(int cid, long byteRecv, long byteSent){
		if(cache.containsKey(cid)){
			Entry e = cache.get(cid);
			logger.info("cid {} used {} bytes in last period ({}/{})[{}s]"
					, cid, (byteRecv-e.totalReceived)+(byteSent-e.totalSent)
					,(byteRecv-e.totalReceived),(byteSent-e.totalSent),
					System.currentTimeMillis()-e.lastUpdate);
			e.recentReceived = e.recentReceived + (byteRecv-e.totalReceived);
			e.recentSent = e.recentSent + (byteSent-e.totalSent);
			e.totalReceived  = byteRecv;
			e.totalSent = byteSent;
			e.lastUpdate = System.currentTimeMillis();
			return 0;
		}else{
			logger.info("New entry, cid {}. {} bytes abandoned({}/{})."
					, cid, byteRecv,byteSent,byteRecv+byteSent);
			Entry e = new Entry();
			e.abandonedReceived = byteRecv;
			e.totalReceived  = byteRecv;
			e.abandonedSent = byteSent;
			e.totalSent = byteSent;
			e.lastUpdate = System.currentTimeMillis();
			cache.put(cid, e);
			return 1;
		}
	}
	
	class Entry{
		
		long abandonedReceived;
		long abandonedSent;
		
		long totalReceived;
		long totalSent;
		
		long recentReceived;
		long recentSent;
		
		long lastUpdate;
		
		String cn;
	}

	public void clear(int t) {
		for(Integer cid:cache.keySet()){
			long delta = System.currentTimeMillis() - cache.get(cid).lastUpdate;
			if(delta > t){
				logger.info("CID#{} is stale, {}B in total({}/{})"
						, cid,cache.get(cid).totalReceived+cache.get(cid).totalSent,
						cache.get(cid).totalReceived,cache.get(cid).totalSent);
			}
		}
	}
}
