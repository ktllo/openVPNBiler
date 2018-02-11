package org.leolo.vpn.biller;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class SharedResource {
	private static SharedResource instance = null;
	
	public static SharedResource getInstance(){
		if(instance == null){
			instance = new SharedResource();
		}
		return instance;
	}
	
	private SharedResource(){
		
	}
	
	Properties prop;
	ExecutorService threadPool;
}
