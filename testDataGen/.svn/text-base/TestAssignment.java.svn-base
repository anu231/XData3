package testDataGen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.impl.sql.compile.DeleteNode;
import org.apache.derby.impl.sql.compile.DropTableNode;
import org.apache.derby.impl.sql.compile.InsertNode;
import org.apache.derby.impl.sql.compile.ParseException;
import org.apache.derby.impl.sql.compile.SQLParser;
import org.apache.derby.impl.sql.compile.StatementNode;
import org.apache.derby.impl.sql.compile.UpdateNode;

import testDataGen.QueryStatusData.QueryStatus;
import util.Configuration;
import util.MyConnection;
import util.TableMap;

public class TestAssignment {


	public void evaluateAssignment(int assignment_id) throws Exception{
		String done="SELECT assignment_id from assignment where assignment_id=? and status='NC'";		
        
        try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
             System.out.println("Testing started");
        
	    Connection dbcon=null;	
	   // System.out.println(LdapAuthentication());
       	try {
    	     // Class.forName("org.postgresql.Driver");
    	      dbcon = MyConnection.getExistingDatabaseConnection();
    	      if(dbcon!=null){
    	    	  System.out.println("Connected successfullly");
    	    	  
    	      }
    			PreparedStatement donestmt=dbcon.prepareStatement(done);
    			donestmt.setInt(1, assignment_id );
    			ResultSet rs = donestmt.executeQuery();
    			if(!rs.next())
    			{
    				System.out.println("Assignment id not valid");
    				return;
    			}
    			String QueryString="select q_id,query from qinfo where assignment_id =?";
    			PreparedStatement qstmt=dbcon.prepareStatement(QueryString);
    			String updateString="update query set status=? where assignment_id=? and q_id=? and user_id=?";
    			PreparedStatement upstmt=dbcon.prepareStatement(updateString);
    			qstmt.setInt(1, assignment_id);
    			ResultSet Queries=qstmt.executeQuery();
    			TestAnswer test=new TestAnswer();
    			while(Queries.next())
    			{
    				String filePath="4";
    				GenerateDataset_new g=new GenerateDataset_new(filePath);
    				g.generateDatasetForQuery("Q"+Queries.getInt(1)+"A"+assignment_id, "true", Queries.getString(2), "");
    				String StudQueryString="select user_id,sql from query where q_id=? and assignment_id=?";
    				PreparedStatement studQueriesStmt=dbcon.prepareStatement(StudQueryString);
    				studQueriesStmt.setInt(1,Queries.getInt(1));
    				studQueriesStmt.setInt(2, assignment_id);
    				ResultSet StudQueries=studQueriesStmt.executeQuery();
    				while(StudQueries.next())
    				{
    					int flag=0;
    					SQLParser sqlParser = new SQLParser();
    					StatementNode s = null;
    					String qry=StudQueries.getString("sql");
    					String OriginalQry=qry.replaceAll("''", "'");
    					System.out.println("queryString" + OriginalQry);
    					qry=OriginalQry.trim().replaceAll("\n+", " ");
    					qry=qry.trim().replaceAll(" +", " ");		
    					if(qry.toLowerCase().contains("year")){
    						qry=qry.replaceAll("year","year1");
    						qry=qry.replaceAll("Year","year1");
    						qry=qry.replaceAll("YEAR","year1");
    			    	}
    					System.out.println("Cleansed Query is "+qry);
    					try{
    					s = sqlParser.Statement(qry, null);
    					}
    					catch (ParseException e)
    					{
    						e.printStackTrace();
    						upstmt.setString(1, "I");
    						flag=1;
    					}
    					if(s instanceof DropTableNode)
    					{
    						upstmt.setString(1, "I");
    						flag=1;
    					}
    					if(flag==0)
    					{
    					String ans=test.testAnswer("Q"+Queries.getInt(1)+"A"+assignment_id, OriginalQry ,"student","4");
    					if(ans.contains("Failed"))
    					{
    						upstmt.setString(1, "W");
    						
    					}
    					else
    					{
    						upstmt.setString(1,"C");
    					}
    					}
    					upstmt.setInt(2, assignment_id);
						upstmt.setInt(3, Queries.getInt(1));
						upstmt.setString(4,StudQueries.getString(1));
						upstmt.executeUpdate();
    				}
    			}
    			String assignmentUpdate="update assignment set status='c' where assignment_id=?";
    			PreparedStatement a=dbcon.prepareStatement(assignmentUpdate);
    			a.setInt(1, assignment_id);
    			a.executeUpdate();
    	      
    	}catch (SQLException ex) {
    	       System.err.println("SQLEEException: " + ex.getMessage());
    	     ex.printStackTrace();
    	}
	}
	
	public QueryStatusData evaluateQuestion(Connection dbcon, Connection testCon, String[] args) throws SQLException, StandardException {
		String StudQueryString = "select querystring from queries where queryid=? and rollnum=?";    
		ResultSet StudQueries=null;
		TestAnswer test = null;
		QueryStatus queryStatus = QueryStatus.Correct;
		QueryStatusData statusData = new QueryStatusData();
		
		int assignment_id=Integer.parseInt(args[0]);
		int question_id=Integer.parseInt(args[1]);
		String rollNum = args[2];
		
		try {			
			test=new TestAnswer();			
			
			PreparedStatement studQueriesStmt = dbcon.prepareStatement(StudQueryString);
			studQueriesStmt.setString(1,"A"+assignment_id+"Q"+question_id);
			studQueriesStmt.setString(2, rollNum);
			System.out.println("A"+assignment_id+"Q"+question_id);
			StudQueries = studQueriesStmt.executeQuery();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
       
		HashSet<String> hs=new HashSet<String>();
		
		if(StudQueries.next())
		{
				int flag=0;
				hs.clear();
				SQLParser sqlParser = new SQLParser();
				StatementNode s = null;
				String qry = StudQueries.getString("querystring");
				String testQuery = StudQueries.getString("querystring");
				String OriginalQry = qry.replaceAll("''", "'");
				System.out.println("queryString" + OriginalQry);
				qry = OriginalQry.trim().replaceAll("\n+", " ");
				qry = qry.trim().replaceAll(" +", " ");		
				if(qry.toLowerCase().contains("year")){
					qry=qry.replaceAll("year","year1");
					qry=qry.replaceAll("Year","year1");
					qry=qry.replaceAll("YEAR","year1");
				}
				System.out.println("Cleansed Query is "+qry);
				
				try{				
					testQuery = TestAnswer.preParseQuery(testQuery);
					
					System.out.println("queryString" + testQuery);
					testQuery = testQuery.trim().replaceAll("\n+", " ");
					testQuery = testQuery.trim().replaceAll(" +", " ");		
					if(testQuery.toLowerCase().contains("year")){
						testQuery = testQuery.replaceAll("year","year1");
						testQuery = testQuery.replaceAll("Year","year1");
						testQuery = testQuery.replaceAll("YEAR","year1");
					}
								
					s= sqlParser.Statement(testQuery, null);			
					if(s instanceof InsertNode){
						testQuery = TestAnswer.convertInsertQueryToSelect(testQuery);
					}else if(s instanceof DeleteNode){
						testQuery = TestAnswer.convertDeleteQueryToSelect(testQuery);
					}else if(s instanceof UpdateNode){
						testQuery = TestAnswer.convertUpdateQueryToSelect(testQuery);
					}
					
					if(testQuery.toLowerCase().contains("year1")){
						testQuery = testQuery.replaceAll("year1","year");
					}
					
					PreparedStatement testStatement = testCon.prepareStatement(testQuery);
					testStatement.executeQuery();
					testStatement.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
					statusData.ErrorMessage = e.getMessage();
					queryStatus = QueryStatus.Error;
					flag=1;
				}
				if(s instanceof DropTableNode)
				{
					queryStatus = QueryStatus.Incorrect;
					flag=1;
				}
				if(flag==0)
				{
					String ans="";
					try {
						ans=test.testAnswerAgainstAllDatasets(dbcon, testCon, "A"+assignment_id+"Q"+question_id, OriginalQry, rollNum, "4");
						System.out.println("Answer is :"+ans);
						if(ans.contains("Failed"))
						{
							queryStatus = QueryStatus.Incorrect;
							String status[]=ans.split(":::");
							for(String sts: status){
								if(sts.contains("Failed")){
									System.out.println("Inside failed");
									String dataset[]=sts.split("::");
									System.out.println("Here ->"+sts);
									hs.add(dataset[0]);
								}
							}
							
							try{
								PreparedStatement pstmt = dbcon.prepareStatement("CREATE TEMPORARY TABLE detectdataset AS (SELECT * FROM detectdataset WHERE (1=0))");
								pstmt.executeUpdate();
							}
							catch(SQLException ex){
								int errorCode = ex.getErrorCode();
								System.out.println("SQL Exception: "+errorCode);
							}
							
							String add_datasets = "insert into detectdataset values (?,?,?,?)";
							PreparedStatement add = dbcon.prepareStatement(add_datasets);
							Iterator<String> setIterator = hs.iterator();
							while(setIterator.hasNext()){
								System.out.println("Inside operator");
								String dataset=setIterator.next();
								PopulateTestData p = new PopulateTestData();
								TableMap tm = (new GenerateCVC1()).getTableMap().getInstances();
								p.fetechAndPopulateTestDatabase(dbcon, testCon, "A"+assignment_id + "Q" + question_id, dataset, tm);
								
								PreparedStatement pp=testCon.prepareStatement(OriginalQry);
								ResultSet rr=pp.executeQuery();
								ResultSetMetaData metadata = rr.getMetaData();
								int no_of_columns=metadata.getColumnCount();
								String result="";
								for(int cl=1;cl<=no_of_columns;cl++)
								{
									//	out_assignment.println("<th>"+metadata.getColumnLabel(cl)+"</th>");
									result+=metadata.getColumnName(cl)+"@@";
								}
								result+=":::";
								while(rr.next())
								{
									for(int j=1;j<=no_of_columns;j++)
									{
										int type = metadata.getColumnType(j);
										if (type == Types.VARCHAR || type == Types.CHAR) {
											result+=rr.getString(j)+"@@";
										} else {
											result+=rr.getLong(j)+"@@";
										}
										
									}
									result+=":::";
								}
								System.out.println("Failed result is :"+result);
								add.setString(1, rollNum);
								add.setString(2,"A"+assignment_id+"Q"+question_id);
								add.setString(3, dataset);
								add.setString(4, result);
								//	add.setString(4, );
								add.executeUpdate();
							}
					
						}
						else
						{
							queryStatus = QueryStatus.Correct;
						}
					} catch (Exception e) {
						System.out.println("Exception caught here ");
						e.printStackTrace();
						queryStatus = QueryStatus.Incorrect;
					}
				}						
		}
		
		statusData.Status = queryStatus;
		return statusData;
	}
	
	void evaluateQuestion(int assignment_id,int question_id) throws SQLException, StandardException 
	{
		String StudQueryString="select rollnum,querystring from queries where queryid=?";    
		Connection testCon = null;
		ResultSet StudQueries=null;
		PreparedStatement upstmt=null;
		TestAnswer test = null;
		Connection dbcon = null;
		
		try {
			testCon = MyConnection.getTestDatabaseConnection();
			test=new TestAnswer();
			
			dbcon =  MyConnection.getExistingDatabaseConnection();
		    if(dbcon!=null){
		      	  System.out.println("Connected successfullly");
		    	    	  
		    }
		    String updateString="update queries set tajudgement=? where queryid=? and rollnum=?";
		  	upstmt=dbcon.prepareStatement(updateString);
			PreparedStatement studQueriesStmt=dbcon.prepareStatement(StudQueryString);
			studQueriesStmt.setString(1,"A"+assignment_id+"Q"+question_id);
			System.out.println("A"+assignment_id+"Q"+question_id);
		//	studQueriesStmt.setInt(2, assignment_id);
			StudQueries=studQueriesStmt.executeQuery();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
       
		HashSet<String> hs=new HashSet<String>();
		//HashMap< String, String> hm=new HashMap<String, String>();
	 	while(StudQueries.next())
		{
						int flag=0;
						hs.clear();
						SQLParser sqlParser = new SQLParser();
						StatementNode s = null;
						String qry=StudQueries.getString("querystring");
						String OriginalQry=qry.replaceAll("''", "'");
						System.out.println("queryString" + OriginalQry);
						qry=OriginalQry.trim().replaceAll("\n+", " ");
						qry=qry.trim().replaceAll(" +", " ");		
						if(qry.toLowerCase().contains("year")){
							qry=qry.replaceAll("year","year1");
							qry=qry.replaceAll("Year","year1");
							qry=qry.replaceAll("YEAR","year1");
						}
						System.out.println("Cleansed Query is "+qry);
						try{
						s = sqlParser.Statement(qry, null);
						}
						catch (ParseException e)
						{
							e.printStackTrace();
							upstmt.setBoolean(1, false);
							flag=1;
						}
						if(s instanceof DropTableNode)
						{
							upstmt.setBoolean(1, false);
							flag=1;
						}
						if(flag==0)
						{
							String ans="";
							try {
								ans=test.testAnswer("A"+assignment_id+"Q"+question_id, OriginalQry ,StudQueries.getString("rollnum"),"4");
								System.out.println("Answer is :"+ans);
								if(ans.contains("Failed"))
								{
									upstmt.setBoolean(1, false);
									String status[]=ans.split(":::");
									for(String sts: status){
										if(sts.contains("Failed")){
											System.out.println("Inside failed");
											String dataset[]=sts.split("::");
											System.out.println("Here ->"+sts);
											hs.add(dataset[0]);
										}
									}
									String add_datasets="insert into detectdataset values (?,?,?,?)";
									PreparedStatement add=dbcon.prepareStatement(add_datasets);
									Iterator<String> setIterator = hs.iterator();
									while(setIterator.hasNext()){
										System.out.println("Inside operator");
										String dataset=setIterator.next();
										PopulateTestData p=new PopulateTestData();
										TableMap tm = (new GenerateCVC1()).getTableMap().getInstances();
										p.fetechAndPopulateTestDatabase("A"+assignment_id+"Q"+question_id, dataset, tm);
										
										PreparedStatement pp=testCon.prepareStatement(OriginalQry);
										ResultSet rr=pp.executeQuery();
										ResultSetMetaData metadata = rr.getMetaData();
										int no_of_columns=metadata.getColumnCount();
										String result="";
										for(int cl=1;cl<=no_of_columns;cl++)
										{
											//	out_assignment.println("<th>"+metadata.getColumnLabel(cl)+"</th>");
											result+=metadata.getColumnName(cl)+"@@";
										}
										result+=":::";
										while(rr.next())
										{
											for(int j=1;j<=no_of_columns;j++)
											{
												int type = metadata.getColumnType(j);
												if (type == Types.VARCHAR || type == Types.CHAR) {
													result+=rr.getString(j)+"@@";
												} else {
													result+=rr.getLong(j)+"@@";
												}
												
											}
											result+=":::";
										}
										System.out.println("Fialed result is :"+result);
										add.setString(1,StudQueries.getString("rollnum"));
										add.setString(2,"A"+assignment_id+"Q"+question_id);
										add.setString(3, dataset);
										add.setString(4, result);
										//	add.setString(4, );
										add.executeUpdate();
									}
							
								}
								else
								{
									upstmt.setBoolean(1, true);
								}
							} catch (Exception e) {
								System.out.println("Exception caught here ");
								e.printStackTrace();
								upstmt.setBoolean(1, false);
							}
						}
						upstmt.setString(2, "A"+assignment_id+"Q"+question_id);
						//upstmt.setInt(3, question_id);
						upstmt.setString(3,StudQueries.getString(1));
						upstmt.executeUpdate();
		}
	 	
	 	dbcon.close();
	 	testCon.close();
	}
	
	
	public QueryStatusData testQuery(String[] args){

		QueryStatusData queryStatus = new QueryStatusData();
		queryStatus.Status = QueryStatus.Correct; 
		
		try {
			
			int assignment_id=Integer.parseInt(args[0]);
			int question_id=Integer.parseInt(args[1]);
			String rollnum = args[2];
			QueryStatus status = QueryStatus.Incorrect;
			SQLParser sqlParser = new SQLParser();
		
			String StudQueryString = "select querystring from queries where queryid=? and rollnum=?";
			
			ResultSet studentQuery = null;
			TestAnswer test = null;
			Connection dbcon = null;
			Connection testCon = null;
			PreparedStatement studQueryStmt = null;
			
			try {
				
				test=new TestAnswer();
				
				dbcon =  MyConnection.getExistingDatabaseConnection();
				testCon = MyConnection.getTestDatabaseConnection();
			    if(dbcon!=null){
			      	  System.out.println("Connected successfullly");			    	    	  
			    }
			   
				studQueryStmt = dbcon.prepareStatement(StudQueryString);
				studQueryStmt.setString(1,"A"+assignment_id+"Q"+question_id);
				studQueryStmt.setString(2, rollnum);
				System.out.println("A"+assignment_id+"Q"+question_id);

				studentQuery = studQueryStmt.executeQuery();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				queryStatus.Status = QueryStatus.Error; 
				return queryStatus;
			}
			
		 	if(studentQuery.next())
			{
		 			String qry = studentQuery.getString("querystring");
		 			String testQuery = qry;
					int flag=0;
					//SQLParser sqlParser = new SQLParser();
					StatementNode s = null;
					
					String OriginalQry=qry.replaceAll("''", "'");
					System.out.println("queryString" + OriginalQry);
					qry=OriginalQry.trim().replaceAll("\n+", " ");
					qry=qry.trim().replaceAll(" +", " ");		
					if(qry.toLowerCase().contains("year")){
						qry=qry.replaceAll("year","year1");
						qry=qry.replaceAll("Year","year1");
						qry=qry.replaceAll("YEAR","year1");
					}
					System.out.println("Cleansed Query is "+qry);
					try{				
						testQuery = TestAnswer.preParseQuery(testQuery);
						
						System.out.println("queryString" + testQuery);
						testQuery = testQuery.trim().replaceAll("\n+", " ");
						testQuery = testQuery.trim().replaceAll(" +", " ");		
						if(testQuery.toLowerCase().contains("year")){
							testQuery = testQuery.replaceAll("year","year1");
							testQuery = testQuery.replaceAll("Year","year1");
							testQuery = testQuery.replaceAll("YEAR","year1");
						}
									
						s= sqlParser.Statement(testQuery, null);			
						if(s instanceof InsertNode){
							testQuery = TestAnswer.convertInsertQueryToSelect(testQuery);
						}else if(s instanceof DeleteNode){
							testQuery = TestAnswer.convertDeleteQueryToSelect(testQuery);
						}else if(s instanceof UpdateNode){
							testQuery = TestAnswer.convertUpdateQueryToSelect(testQuery);
						}
						
						if(testQuery.toLowerCase().contains("year1")){
							testQuery = testQuery.replaceAll("year1","year");
						}
						
						PreparedStatement testStatement = testCon.prepareStatement(testQuery);
						testStatement.executeQuery();
						testStatement.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						queryStatus.ErrorMessage = e.getMessage();
						queryStatus.Status = QueryStatus.Error;
						flag = 1;
					}
					
					if(flag == 0){
						try {
							status = test.testQueryAnswer("A"+assignment_id+"Q"+question_id, OriginalQry, rollnum, "4");
						} catch (Exception e) {
							System.out.println("Exception caught here ");
							e.printStackTrace();
						}
					}
			}
		 	
		 	studQueryStmt.close();
		 	studentQuery.close();
		 	testCon.close();
		 	dbcon.close();
		 	
		 	queryStatus.Status = status;
			return queryStatus;
		} catch (Exception e) {
			e.printStackTrace();
			queryStatus.Status = QueryStatus.Error; 
			return queryStatus;
		}
	}
	
	
	public static void entry(String[] args) {
		TestAssignment ta=new TestAssignment();
		try {
			int assignment_id=Integer.parseInt(args[0]);
			int question_id=Integer.parseInt(args[1]);
			ta.evaluateQuestion(assignment_id, question_id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		TestAssignment ta=new TestAssignment();
		try {
			int assignment_id=Integer.parseInt(args[0]);
			int question_id=Integer.parseInt(args[1]);
			ta.evaluateQuestion(assignment_id, question_id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
