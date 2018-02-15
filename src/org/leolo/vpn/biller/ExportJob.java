package org.leolo.vpn.biller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.TimeZone;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportJob implements Job {
	
	Logger logger = LoggerFactory.getLogger(ExportJob.class);
	
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.debug("Job started");
		Connection conn = null;
		PreparedStatement pstmt1 = null;
		PreparedStatement pstmt2 = null;
		PreparedStatement pstmt3 = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		
		try{
			ArrayList<Entry> list = new ArrayList<>();
			conn = DBManager.getInstance().getConnection();
			conn.setAutoCommit(false);
			pstmt1 = conn.prepareStatement(
					"SELECT DISTINCT DATE(disconnected) FROM sessions "
					+ "WHERE status = 'PEND' AND DATE(disconnected) <> DATE(NOW())");
			pstmt2 = conn.prepareStatement(
					"SELECT sid, commonName, connected, disconnected, bytein, byteout "
					+ "FROM sessions WHERE DATE(disconnected) = ?");
			pstmt3 = conn.prepareStatement("UPDATE sessions SET status = 'EXPT' WHERE sid = ?");
			rs1 = pstmt1.executeQuery();
			while(rs1.next()){
				logger.info("Processing {}", sdf1.format(rs1.getDate(1)));
				pstmt2.setDate(1, rs1.getDate(1));
				rs2 = pstmt2.executeQuery();
				ArrayList<Entry> entries = new ArrayList<>();
				while(rs2.next()){
					entries.add(new Entry(
						rs2.getInt(1),
						rs2.getString(2),
						rs2.getTimestamp(3).getTime(),
						rs2.getTimestamp(4).getTime(),
						rs2.getLong(5),
						rs2.getLong(6)
							));
					pstmt3.setInt(1, rs2.getInt(1));
					pstmt3.addBatch();
				}
				export(rs1.getDate(1), entries);
				pstmt3.executeBatch();
				if(rs2!=null){
					rs2.close();
					rs2 = null;
				}
			}
			conn.commit();
		}catch(SQLException e){
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}finally{
			try{
				if(rs1!=null){
					rs1.close();
					rs1 = null;
				}
				if(rs2!=null){
					rs2.close();
					rs2 = null;
				}
				if(pstmt1!=null){
					pstmt1.close();
					pstmt1 = null;
				}
				if(pstmt2!=null){
					pstmt2.close();
					pstmt2 = null;
				}
				if(pstmt3!=null){
					pstmt3.close();
					pstmt3 = null;
				}
				if(conn!=null){
					conn.rollback();
					conn.close();
					conn = null;
				}
			}catch(SQLException e){
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	private void export(Date date,ArrayList<Entry> entries) throws IOException{
		PrintWriter out = new PrintWriter("export/"+HOSTNAME+"_"+sdf2.format(date)+"_0001.xml");
		out.println("<?xml version='1.0'?>");
		out.print("<usage>");
		out.print("<head><hostname>"+HOSTNAME+"</hostname>");
		out.print("<generated>"+getISO8601StringForDate(new Date())+"</generated>");
		out.print("<size>"+entries.size()+"</size></head>");
		for(Entry e:entries){
			out.print("<entry id='"+e.sid+"'>");
			out.print("<commonName><![CDATA["+e.cn+"]]></commonName>");
			out.print("<time>");
			out.print("<from timestamp='"+e.start/1000+"'>"+getISO8601StringForDate(new Date(e.start))+"</from>");
			out.print("<to timestamp='"+e.end/1000+"'>"+getISO8601StringForDate(new Date(e.end))+"</to>");
			out.print("</time>");
			out.print("<data><in>"+e.bytein+"</in><out>"+e.byteout+"</out><total>"+(e.bytein+e.byteout)+"</total></data>");
			out.print("</entry>");
		}
		out.print("</usage>");
		out.println();
		
		out.close();
	}
	
	
	private static String getISO8601StringForDate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat.format(date);
	}
	private final String HOSTNAME;
	
	public ExportJob(){
		Properties prop = SharedResource.getInstance().prop;
		if(prop.containsKey("host.name")){
			HOSTNAME = prop.getProperty("host.name");
		}else{
			String hostname = null;
			try {
				hostname = execReadToString("hostname");
			} catch (IOException e) {
				hostname = Integer.toHexString(new java.util.Random().nextInt());
			}
			HOSTNAME = hostname;
		}
	}
	
	public static String execReadToString(String execCommand) throws IOException {
	    try (Scanner s = new Scanner(Runtime.getRuntime().exec(execCommand).getInputStream()).useDelimiter("\\A")) {
	        return s.hasNext() ? s.next() : "";
	    }
	}
	
	private class Entry{
		public Entry(int sid, String cn, long start, long end, long bytein, long byteout) {
			super();
			this.sid = sid;
			this.cn = cn;
			this.start = start;
			this.end = end;
			this.bytein = bytein;
			this.byteout = byteout;
		}
		int sid;
		String cn;
		long start;
		long end;
		long bytein;
		long byteout;
	}
	
}
