package org.leolo.vpn.biller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
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
			logger.info("cid {} used {} bytes in last period ({}/{})[{}ms]"
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
		long connected;
		
		String cn;
	}

	public void clear(int t) {
		ArrayList<Integer> pendingRemove = new ArrayList<>();
		for(Integer cid:cache.keySet()){
			long delta = System.currentTimeMillis() - cache.get(cid).lastUpdate;
			if(delta > t){
				logger.info("CID#{} is stale, {}B in total({}/{})"
						, cid,cache.get(cid).totalReceived+cache.get(cid).totalSent,
						cache.get(cid).totalReceived,cache.get(cid).totalSent);
				Entry e = cache.get(cid);
				pendingRemove.add(cid);
				SharedResource.getInstance().threadPool.execute(new Runnable(){
					Logger logger = LoggerFactory.getLogger("UsageCache.clear$1");
					@Override
					public void run() {
						try{
							logger.info("Inserting the usage record");
							Connection conn = DBManager.getInstance().getConnection();
							PreparedStatement pstmt = conn.prepareStatement("INSERT INTO "
									+ "sessions (`commonName`,`connected`,`disconnected`,`bytein`,`byteout`) VALUES "
									+ "( ? , ?, ?, ?, ?)");
							pstmt.setString(1, e.cn);
							pstmt.setTimestamp(2, new Timestamp(e.connected));
							pstmt.setTimestamp(3, new Timestamp(e.lastUpdate));
							pstmt.setLong(4, e.recentReceived);
							pstmt.setLong(5, e.recentSent);
							boolean result = pstmt.execute();
							logger.info("Finish the usage record, result {}", result);
							pstmt.close();
							conn.close();
						}catch(SQLException e){
							logger.error(e.getMessage(), e);
						}
					}
				});
			}
		}
		for(Integer i:pendingRemove){
			cache.remove(i);
		}
	}

	public void split() {
		for(Integer cid:cache.keySet()){
			long conntime = cache.get(cid).connected;
			long recv = cache.get(cid).recentReceived;
			long sent = cache.get(cid).recentSent;
			long time = System.currentTimeMillis();
			String cn = cache.get(cid).cn;
			cache.get(cid).connected = time;
			cache.get(cid).recentReceived = 0;
			cache.get(cid).recentSent = 0;
			logger.info("<>{},{},{},{},{}", cn, conntime, recv, sent, time);
			SharedResource.getInstance().threadPool.execute(new Runnable(){
				Logger logger = LoggerFactory.getLogger("UsageCache.split$1");
				@Override
				public void run() {
					try{
						logger.debug("Inserting the usage record");
						Connection conn = DBManager.getInstance().getConnection();
						PreparedStatement pstmt = conn.prepareStatement("INSERT INTO "
								+ "sessions (`commonName`,`connected`,`disconnected`,`bytein`,`byteout`) VALUES "
								+ "( ? , ?, ?, ?, ?)");
						pstmt.setString(1, cn);
						pstmt.setTimestamp(2, new Timestamp(conntime));
						pstmt.setTimestamp(3, new Timestamp(time));
						pstmt.setLong(4, recv);
						pstmt.setLong(5, sent);
						boolean result = pstmt.execute();
						logger.debug("Finish the usage record, result {}", result);
						pstmt.close();
						conn.close();
					}catch(SQLException e){
						logger.error(e.getMessage(), e);
					}
				}
			});
		}
	}
}
