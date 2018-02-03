package org.leolo.vpn.biller;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CidMap {
	private static CidMap instance = null;
	Logger logger = LoggerFactory.getLogger(CidMap.class);
	public static CidMap getInstance(){
		if(instance==null){
			instance = new CidMap();
		}
		return instance;
	}
}

class CidMapHandler extends Thread{
	Logger logger = LoggerFactory.getLogger(CidMapHandler.class);
	private String line;
	private MainThread main;
	CidMapHandler(MainThread main,String line){
		this.line = line;
		this.main = main;
	}
	
	public void run(){
		logger.info(line);
		Tokenizer st = new Tokenizer(line);
		ArrayList<String> list = new ArrayList<>();
		while(st.hasMoreTokens()){ 
			String token = st.nextToken();
			list.add(token);
		}
		/*
		 *  0	Common Name,	
		 *  1	Real Address,
		 *  2	Virtual Address,
		 *  3	Virtual IPv6 Address,
		 *  4	Bytes Received,
		 *  5	Bytes Sent,
		 *  6	Connected Since,
		 *  7	Connected Since (time_t),
		 *  8	Username,
		 *  9	Client ID,
		 * 10	Peer ID 
		 */
		logger.info("CID:{} is {}, connected at {}", list.get(9), list.get(0), list.get(6));
	}
	
	class Tokenizer{
		private String string;
		private int startPos = 0;
		
		Tokenizer(String string){
			this.string = string;
		}

		public String nextToken() {
			int endPos = startPos;
			while(true){
				if(string.charAt(endPos++)==',') break;
				if(endPos>=string.length()) break;
			}
			String a =  string.substring(startPos, endPos<string.length()?endPos-1:endPos);
			startPos = endPos;
			return a;
		}

		public boolean hasMoreTokens() {
			return startPos < string.length();
		}
	}
}


