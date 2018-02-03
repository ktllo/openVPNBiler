package org.leolo.vpn.biller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBManager {
	private static DBManager instance = null;
	Logger logger = LoggerFactory.getLogger(DBManager.class);
	public static DBManager getInstance(){
		if(instance==null){
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			instance = new DBManager();
		}
		return instance;
	}
}
