package org.leolo.vpn.biller;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
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
	BasicDataSource ds;
	private DBManager(){
		ds = new BasicDataSource();
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setUsername(SharedResource.getInstance().prop.getProperty("db.user"));
		ds.setPassword(SharedResource.getInstance().prop.getProperty("db.pass"));
		ds.setUrl("jdbc:mysql://"+SharedResource.getInstance().prop.getProperty("db.host")+"/"+SharedResource.getInstance().prop.getProperty("db.name"));
	}
	
	public Connection getConnection() throws SQLException{
		return ds.getConnection();
	}
}
