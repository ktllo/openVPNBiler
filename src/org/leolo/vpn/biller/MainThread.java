package org.leolo.vpn.biller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainThread extends Thread{
	private Logger logger = LoggerFactory.getLogger(MainThread.class);
	

	
	MainThread(){
	}
	
	public void run(){
		while(true){
			mainLoop();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.error(e.getLocalizedMessage(),e);
			}
		}
	}
	PrintWriter pw;
	private void mainLoop(){
		try {
			logger.info("Connecting to {}:{}",SharedResource.getInstance().prop.getProperty("console.host", "localhost"), 
					Integer.parseInt(SharedResource.getInstance().prop.getProperty("console.port")));
			Socket s = new Socket(
					SharedResource.getInstance().prop.getProperty("console.host", "localhost"), 
						Integer.parseInt(SharedResource.getInstance().prop.getProperty("console.port")));
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			logger.info("Connected!");
			pw = new PrintWriter(s.getOutputStream());
			pw.println("bytecount 5");
			pw.flush();
			while(true){
				String line = br.readLine();
				if(line==null){
					break;
				}else if(line.startsWith(">BYTECOUNT_CLI:")){
					SharedResource.getInstance().threadPool.submit(new ByteCountHandler(this, line.substring(15)));
					continue;
				}else if(line.startsWith("CLIENT_LIST,")){
					SharedResource.getInstance().threadPool.submit(new CidMapHandler(this, line.substring(12)));
					continue;
				}else if(
						line.startsWith("TITLE,") ||
						line.startsWith("TIME,") ||
						line.startsWith("HEADER,") ||
						line.startsWith("ROUTING_TABLE,") ||
						line.startsWith("GLOBAL_STATS,") ||
						line.startsWith("END") ||
						false){
					//Throws away data from status
					continue;
				}
				logger.info(line);
			}
		} catch (NumberFormatException e) {
			logger.error(e.getLocalizedMessage(),e);
		} catch (UnknownHostException e) {
			logger.error(e.getLocalizedMessage(),e);
		} catch (IOException e) {
//			logger.error(e.getLocalizedMessage(),e);
		}
		
	}
}
