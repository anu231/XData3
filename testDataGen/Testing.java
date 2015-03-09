package testDataGen;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import util.Configuration;
import util.MyConnection;

public class Testing {

	public static void main(String[] args) {
		PreparedStatement smt;
		String filePath = "4";	

		int rollno=1;
		String qID="";
		String quesDesc="";
		Connection conn = null;
		try {
			conn = MyConnection.getExistingDatabaseConnection();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String strFile = Configuration.homeDir+"/temp_cvc4/NewAssignment/Assign1/9.csv";
		Scanner sc = null;
		try {
			sc = new Scanner(new File(strFile));

		sc.useDelimiter("\\s*:\\s*");
		sc.next();sc.next();sc.next();
		rollno=1;
		qID=sc.next();
		quesDesc=sc.next();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String quesID = null; 
		System.out.println(quesDesc);
		while(sc.hasNext()){
			quesID=qID;
			if(quesID.startsWith("'extra'")){
				quesID=qID.substring(8);					
			}
			quesID=""+quesID.charAt(2);// + quesID.charAt(3);
			
			String insertquery="INSERT INTO queries_new VALUES ('d1',"+quesID+",'"+rollno+"',";
			//System.out.println("Question Number "+quesID);
			//System.out.println("Question Description is "+quesDesc);
			sc.next();
			sc.next();
			sc.next();
			do{
				String query=sc.next();
				query=query.trim().replaceAll(";", " ");
				// parse the query
				String parsedquery="'";
				for(int j=1;j<query.length()-1;j++){
					if(query.charAt(j)=='\n'){
						parsedquery+=" ";
					}
					else if(query.charAt(j)=='\''){
						parsedquery+="'";
					}else{
						parsedquery+=query.charAt(j);
					}
				}
				parsedquery+="'";
				
				//for oracle
				//insertquery+=parsedquery+",'true',' ')";
				
				//for postgres
				insertquery+=parsedquery+",true,true,true,false);";
				
				//System.out.println("INSERT Query ===> "+insertquery);
				
				try {
					smt=conn.prepareStatement(insertquery);
					smt.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
					
				rollno++;
				insertquery="INSERT INTO queries_new VALUES ('d1',"+quesID+",'"+rollno+"',";
				if(sc.hasNext()){
					qID=sc.next();
					quesDesc=sc.next();						
				}else{
					break;	//end of file
				}
			}while(qID.length()==0);			
		
		}
		try {
			new TestAnswer().test(filePath,quesID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
