package org.leolo.vpn.biller;

import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteCountHandler extends Thread{

	private Logger logger = LoggerFactory.getLogger(ByteCountHandler.class);
	
	private String line;
	private MainThread main;
	ByteCountHandler(MainThread main,String line){
		this.line = line;
		this.main = main;
	}
	
	public void run(){
		StringTokenizer st = new StringTokenizer(line,",");
		int cid = Integer.parseInt(st.nextToken());
		long recv = Long.parseLong(st.nextToken());
		long sent = Long.parseLong(st.nextToken());
		logger.info("cid:{} recv:{} send:{} total:{}", cid, recv, sent, recv+sent);
		if(UsageCache.getInstance().update(cid, recv, sent)==1){
			main.pw.println("status 2");
			main.pw.flush();
		}
	}
}
