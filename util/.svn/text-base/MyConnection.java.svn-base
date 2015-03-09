package util;

import java.sql.*;

public class MyConnection {	
	
	static GraderDatasource graderDatesource = new GraderDatasource();
	
	static TesterDatasource testerDatasource = new TesterDatasource();
	
	public static Connection getExistingDatabaseConnection() throws Exception{
		return graderDatesource.getConnection();
	}
	
	public static Connection getTestDatabaseConnection() throws Exception{
		return testerDatasource.getConnection();
	}
	
	public static void main(String[] args) {
		/*MyConnection myConn = new MyConnection();
		try {
			Connection conn = myConn.getExistingDatabaseConnection();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		for (int x=0; x < 100; x++)
	    {
	        TestThread temp= new TestThread("Thread #" + x);
	        temp.start();
	        System.out.println("Started Thread:" + x);
	    }
	}
	
}
