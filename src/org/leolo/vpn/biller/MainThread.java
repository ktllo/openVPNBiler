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
	
	private Properties prop;
	private ExecutorService threadPool;
	
	MainThread(Properties prop, ExecutorService  threadPool){
		this.prop = prop;
		this.threadPool = threadPool;
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
			logger.info("Connecting to {}:{}",prop.getProperty("console.host", "localhost"), 
					Integer.parseInt(prop.getProperty("console.port")));
			Socket s = new Socket(
						prop.getProperty("console.host", "localhost"), 
						Integer.parseInt(prop.getProperty("console.port")));
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
					threadPool.submit(new ByteCountHandler(this, line.substring(15)));
					continue;
				}else if(line.startsWith("CLIENT_LIST,")){
					threadPool.submit(new CidMapHandler(this, line.substring(12)));
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
			logger.error(e.getLocalizedMessage(),e);
		}
		
	}
}
