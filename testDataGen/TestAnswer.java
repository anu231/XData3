package testDataGen;


import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.filechooser.FileSystemView;

import org.apache.derby.impl.sql.compile.DeleteNode;
import org.apache.derby.impl.sql.compile.InsertNode;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SQLParser;
import org.apache.derby.impl.sql.compile.StatementNode;
import org.apache.derby.impl.sql.compile.UpdateNode;

import util.Configuration;
import util.MyConnection;
import util.TableMap;






import util.Utilities;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.sql.*;

import util.Configuration;
import util.MyConnection;
import testDataGen.*;
import testDataGen.QueryStatusData.QueryStatus;

import parsing.*;

import java.util.Iterator;



public class TestAnswer {
	
	public boolean isView=false;
	public boolean isCreateView=false;

	Vector<String>  datasets;
	public TestAnswer() {
		
	}
	public static void Dataset(String mut, String ds,String filePath, String query){
		
	}
	
	public static String preParseQuery(String queryString) throws Exception{

		StringTokenizer st=new StringTokenizer(queryString.trim());
		String token=st.nextToken();

		if(!token.equalsIgnoreCase("with")){
			return queryString;
		}
		int numberOfAlias=0;
		String aliasname[]=new String[10];
		String subquery[]=new String[10];

		while(true){

			String columnname="";
			aliasname[numberOfAlias]=st.nextToken();

			if(aliasname[numberOfAlias].contains("(")){

				columnname=aliasname[numberOfAlias].substring(aliasname[numberOfAlias].indexOf("("));
				columnname=columnname.substring(1,columnname.length()-1);	//remove ( & )

				aliasname[numberOfAlias]=aliasname[numberOfAlias].substring(0,aliasname[numberOfAlias].indexOf("("));           	

			}
			token=st.nextToken();   	// should be AS key word or should start with (

			if(token.startsWith("(")){
				while(!token.contains(")")){
					columnname+=token;
					token=st.nextToken();
				}
				columnname+=token;            	
				token=st.nextToken();	// should be AS key word
			}

			if(!token.equalsIgnoreCase("as")){            	
				Exception e= new Exception("Error while preparsing the with clause AS expected");
				throw e;
			}
			
			subquery[numberOfAlias]="(";
			queryString=queryString.substring(queryString.indexOf("(")+1);
			if(columnname.length()!=0){
				queryString=queryString.substring(queryString.indexOf("(")+1);
			}

			int count=1,i=0;
			while(count!=0){
				if(queryString.charAt(i)=='('){
					count++;
				}else if(queryString.charAt(i)==')'){
					count--;
				}
				subquery[numberOfAlias]+=queryString.charAt(i);
				i++;
			}
			queryString=queryString.substring(i).trim();

			if(columnname.length()!=0){
				columnname=columnname.substring(1,columnname.length()-1);
				String columnlist[]=columnname.split(",");
				int ctr=0;
				String temp=subquery[numberOfAlias];
				subquery[numberOfAlias]="";            	
				String tok=temp.substring(0,temp.indexOf("from"));
				for(int j=0;j<tok.length();j++){
					if(tok.charAt(j)==','){
						subquery[numberOfAlias]+=" as "+columnlist[ctr++]+" , ";
					}else{
						subquery[numberOfAlias]+=tok.charAt(j);
					}

				}            	            	
				subquery[numberOfAlias]+=" as "+columnlist[ctr]+" "+temp.substring(temp.indexOf("from"));
			}

			numberOfAlias++;
			if(queryString.charAt(0)!=','){            	
				break;
			}else{
				st=new StringTokenizer(queryString.substring(1).trim());
			}

		}

		String newquery="";
		/*Add the select part to new query */
		st=new StringTokenizer(queryString);                    
		//token=st.nextToken();
		
		while(st.hasMoreTokens()){
			
			token=st.nextToken();
			
			if(token.toLowerCase().equals("from")){
				newquery+=token+ " ";
				newquery = parseFromPart(st, newquery, numberOfAlias, subquery, aliasname);				
			}
			else{			
				newquery+=token+ " ";
			}
		}

		return newquery;
	}
	
	private static String parseFromPart(StringTokenizer st, String newquery, int numberOfAlias, String subquery[], String aliasname[]){
		
		String token;
		
		while(st.hasMoreTokens()){
			token=st.nextToken();            
					
			if(token.equalsIgnoreCase("where")||token.equalsIgnoreCase("group")){
				newquery+=token+ " ";
				break;
			}			
			
			if(token.equals(",")){
				newquery+=token+ " ";
			}
			if(token.contains(",")){
				token+=" ";
				String tablenames[]=token.split(",");
				for(int j=0;j<tablenames.length;j++){
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(tablenames[j].equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=tablenames[j]+" ";
					}
					newquery+=",";
				}
				newquery=newquery.substring(0,newquery.length()-1);

			}else if(token.contains(")")){
				String relationName = token.substring(0, token.length() - 1);				
				
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(relationName.equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=relationName + " ";
					}
					newquery+=")";
				
			}else{
				boolean isPresent=false;
				for(int k=0;k<numberOfAlias;k++){
					if(token.equals(aliasname[k])){
						newquery+=subquery[k] + " " + aliasname[k]+" ";
						isPresent=true;
					}
				}
				if(!isPresent){
					newquery+=token+" ";
				}
			}

		}
		
		return newquery;		
	}
    
    public static Vector<String> checkAgainstOriginalQuery(HashMap<String,String> mutants, String datasetName, String queryString, String filePath, boolean orderIndependent, Vector<String> columnmismatch, Connection conn) throws IOException{
    	PreparedStatement pstmt = null;
		ResultSet rs = null;
		//Connection conn = null;
		String temp="";
		BufferedReader input=null;
		boolean isInsertquery=false;
		boolean isUpdatequery=false;
		boolean isDeletequery=false;
		
		Vector<String> queryIds = new Vector<String>();
		try{
			
			try{
				pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE dataset AS (SELECT * FROM dataset WHERE (1=0))");
				pstmt.executeUpdate();
			}
			catch(SQLException ex){
				int errorCode = ex.getErrorCode();
				System.out.println("SQL Exception: "+errorCode);
			}
			
			pstmt = conn.prepareStatement("insert into dataset values('" + datasetName + "')");
			pstmt.executeUpdate();
			int i=1;			
			
			queryString = preParseQuery(queryString);
			
			System.out.println("queryString" + queryString);
			queryString=queryString.trim().replaceAll("\n+", " ");
			queryString=queryString.trim().replaceAll(" +", " ");		
			if(queryString.toLowerCase().contains("year")){
				queryString=queryString.replaceAll("year","year1");
				queryString=queryString.replaceAll("Year","year1");
				queryString=queryString.replaceAll("YEAR","year1");
			}
						
			StatementNode s=new SQLParser().Statement(queryString, null);			
			if(s instanceof InsertNode){
				isInsertquery=true;
				queryString=convertInsertQueryToSelect(queryString);
			}else if(s instanceof DeleteNode){
				isDeletequery=true;
				queryString=convertDeleteQueryToSelect(queryString);
			}else if(s instanceof UpdateNode){
				isUpdatequery=true;
				queryString=convertUpdateQueryToSelect(queryString);
			}

			String mutant_query="";
			Collection c = mutants.keySet();
			Iterator itr = c.iterator();
			while(itr.hasNext()){
				Object Id = itr.next();
				String mutant_qry = mutants.get(Id);
				mutant_qry=mutant_qry.trim().replace(';', ' ');				
				
				if(isInsertquery){					
					mutant_qry=convertInsertQueryToSelect(mutant_qry);
				}else if(isDeletequery){
					mutant_qry=convertDeleteQueryToSelect(mutant_qry);
				}if(isUpdatequery){
					mutant_qry=convertUpdateQueryToSelect(mutant_qry);
				}
				
				if(queryString.toLowerCase().contains("year1")){
					queryString=queryString.replaceAll("year1","year");
				}
				
				if(mutant_qry.toLowerCase().contains("year1")){
					mutant_qry=mutant_qry.replaceAll("year1","year");
				}
				
				PreparedStatement pstmt11 = conn.prepareStatement(queryString);
				PreparedStatement pstmt22 = conn.prepareStatement(mutant_qry);			
				
				if(orderIndependent){
					try{						
						pstmt = conn.prepareStatement("with x1 as (" + queryString + "), x2 as (" + mutant_qry + ") select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists ((select * from x1) except all (select * from x2)) or exists ((select * from x2) except all (select * from x1))");
						rs = pstmt.executeQuery();
						
						ResultSet rs11 = pstmt22.executeQuery();
						
						while(rs11.next()){
							System.out.println(rs11.getString(1) );
						}
						
						System.out.println("--------------------------------------------- ");
						ResultSet rs22 = pstmt11.executeQuery();
						System.out.println("--------------------------------------------- ");
						System.out.println("Query String query "+queryString);
						System.out.println("--------------------------------------------- ");
						
						while(rs22.next()){
							System.out.println(rs22.getString(1));
						}
						
						System.out.println("--------------------------------------------- ");
						
					}catch(Exception ex){
						ex.printStackTrace();
						
						try{
							pstmt = conn.prepareStatement(mutant_qry);
							rs = pstmt.executeQuery();
							ResultSetMetaData rsmd=rs.getMetaData();
							Vector<String> projectedCols = new Vector<String>();
							pstmt =conn.prepareStatement(queryString);
							rs =pstmt.executeQuery();
							ResultSetMetaData orgRsmd=rs.getMetaData();
							for(int k=1;k<=orgRsmd.getColumnCount();k++){
								projectedCols.add(orgRsmd.getColumnName(k));
							}
							if(orgRsmd.getColumnCount()!=rsmd.getColumnCount()){
								columnmismatch.add((String)Id);
							}
							else{
								for(int k=1;k<=rsmd.getColumnCount();k++){
									if(!projectedCols.contains(rsmd.getColumnName(k))){
										columnmismatch.add((String)Id);
										break;
									}
								}
							}
						}catch(Exception e){
							e.printStackTrace();
							queryIds.add((String)Id);
						}
					}
					if(rs==null){
//						System.out.println("rs is null");
					}
					else if(rs!=null && rs.next()){
						
						//System.out.println("Adding Query Id = "+(String)Id);
						queryIds.add((String)Id);
						
					}else{
						//System.out.println("rs is empty");
					}
				}
				else{
					PreparedStatement pstmt1 = conn.prepareStatement(queryString);
					PreparedStatement pstmt2 = conn.prepareStatement(mutant_qry);
	     			ResultSet rs1 = pstmt1.executeQuery();
					ResultSet rs2 = pstmt2.executeQuery();
					boolean outputEqual = true;
					while(rs1!=null && rs1.next() && rs2!=null && rs2.next()){
						if(rs1.equals(rs2)){
							
						}
						else{
							outputEqual = false;
						}
					}
					if((rs1!=null && rs2 == null) || (rs1== null && rs2 !=null)){	
						outputEqual = false;
					}
					if(!outputEqual){
						queryIds.add((String)Id);
					}
					
					pstmt1.close();
					pstmt2.close();
					rs1.close();
					rs2.close();
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(input!=null)
				input.close();
		}
		System.out.println("--------------------------------------------- ");
		System.out.println("Dataset: "+datasetName+" Killed mutants: "+queryIds);
		
		System.out.println("--------------------------------------------- ");
		
		try {
			pstmt.close();
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryIds;
    }
    
	
	public static Vector<String> mutantsKilledByDataset1(HashMap<String,String> mutants, String datasetName, String queryString, String filePath, boolean orderIndependent, Vector<String> columnmismatch,Connection conn) throws IOException{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		//Connection conn = null;
		String temp="";
		BufferedReader input=null;
		boolean isInsertquery=false;
		boolean isUpdatequery=false;
		boolean isDeletequery=false;
		
		Vector<String> queryIds = new Vector<String>();
		try{
			conn = MyConnection.getTestDatabaseConnection();
			//Thread.sleep(500);
			pstmt = conn.prepareStatement("delete from dataset");
			pstmt.executeUpdate();
			//Thread.sleep(500);
			pstmt = conn.prepareStatement("insert into dataset values('" + datasetName + "')");
			pstmt.executeUpdate();
		//	pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE DATASET AS (SELECT * FROM DATASET WHERE (1=0))");
		//	pstmt.executeUpdate();
			//Thread.sleep(500);
			pstmt = conn.prepareStatement("insert into dataset values('" + datasetName + "')");
			pstmt.executeUpdate();
			//Thread.sleep(500);
			int i=1;
			
			//UNCOMMENT AFTER WARD
			/*
			StatementNode s=new SQLParser().Statement(queryString, null);			
			if(s instanceof InsertNode){
				isInsertquery=true;
				queryString=convertInsertQueryToSelect(queryString);
			}else if(s instanceof DeleteNode){
				isDeletequery=true;
				queryString=convertDeleteQueryToSelect(queryString);
			}else if(s instanceof UpdateNode){
				isUpdatequery=true;
				queryString=convertUpdateQueryToSelect(queryString);
			}
			*/
			String mutant_query="";
			Collection c = mutants.keySet();
			Iterator itr = c.iterator();
			while(itr.hasNext()){
				Object Id = itr.next();
				//System.out.println("\nId="+Id);
				String mutant_qry = mutants.get(Id);
				
				//System.out.println("Student: "+mutant_qry);
				mutant_qry=mutant_qry.trim().replace(';', ' ');				
				
				if(isInsertquery){					
					mutant_qry=convertInsertQueryToSelect(mutant_qry);
				}else if(isDeletequery){
					mutant_qry=convertDeleteQueryToSelect(mutant_qry);
				}if(isUpdatequery){
					mutant_qry=convertUpdateQueryToSelect(mutant_qry);
				}
				
				PreparedStatement pstmt11 = conn.prepareStatement(queryString);
				PreparedStatement pstmt22 = conn.prepareStatement(mutant_qry);
				
				
				
				
				if(orderIndependent){
					try{			
						
						//System.out.println("Student id: "+(String)Id);
						
						//pstmt = conn.prepareStatement("select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists (("+ queryString +") except all ("+ mutant_qry +")) or exists (("+ mutant_qry +") except all (" + queryString + "))");
						//System.out.println("select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists (("+ queryString +") except all ("+ mutant_qry +")) or exists (("+ mutant_qry +") except all (" + queryString + "))");
						pstmt = conn.prepareStatement("with x1 as (" + queryString + "), x2 as (" + mutant_qry + ") select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists ((select * from x1) except all (select * from x2)) or exists ((select * from x2) except all (select * from x1))");
						rs = pstmt.executeQuery();
						
						ResultSet rs11 = pstmt22.executeQuery();
						//System.out.println("--------------------------------------------- ");
						//System.out.println("Mutant query "+mutant_qry);
						
						while(rs11.next()){
							//System.out.println(rs11.getString(1) );
						}
						//System.out.println("--------------------------------------------- ");
						ResultSet rs22 = pstmt11.executeQuery();
						//System.out.println("--------------------------------------------- ");
						//System.out.println("Query String query "+queryString);
						//System.out.println("--------------------------------------------- ");
						while(rs22.next()){
							//System.out.println(rs22.getString(1));
						}
						//System.out.println("--------------------------------------------- ");
						
					}catch(Exception ex){
						ex.printStackTrace();
						//System.out.println("Adding Query Id = "+(Integer)Id);
						try{
							pstmt = conn.prepareStatement(mutant_qry);
							rs = pstmt.executeQuery();
							ResultSetMetaData rsmd=rs.getMetaData();
							Vector<String> projectedCols = new Vector<String>();
							pstmt =conn.prepareStatement(queryString);
							rs =pstmt.executeQuery();
							ResultSetMetaData orgRsmd=rs.getMetaData();
							for(int k=1;k<=orgRsmd.getColumnCount();k++){
								projectedCols.add(orgRsmd.getColumnName(k));
							}
							if(orgRsmd.getColumnCount()!=rsmd.getColumnCount()){
								columnmismatch.add((String)Id);
							}
							else{
								for(int k=1;k<=rsmd.getColumnCount();k++){
									if(!projectedCols.contains(rsmd.getColumnName(k))){
										columnmismatch.add((String)Id);
										break;
									}
								}
							}
						}catch(Exception e){
							e.printStackTrace();
							queryIds.add((String)Id);
						}
					}
					if(rs==null){
//						System.out.println("rs is null");
					}
					else if(rs!=null && rs.next()){
						
						//System.out.println("Adding Query Id = "+(String)Id);
						queryIds.add((String)Id);
						
					}else{
						//System.out.println("rs is empty");
					}
				}
				else{// order by clause is dere
					// check output of both the queries row by row
					PreparedStatement pstmt1 = conn.prepareStatement(queryString);
					PreparedStatement pstmt2 = conn.prepareStatement(mutant_qry);
					//System.out.println("Mutant query "+mutant_qry);
					//System.out.println("Query String query "+queryString);
					ResultSet rs1 = pstmt1.executeQuery();
					ResultSet rs2 = pstmt2.executeQuery();
					boolean outputEqual = true;
					while(rs1!=null && rs1.next() && rs2!=null && rs2.next()){
						if(rs1.equals(rs2)){
							
						}
						else{
							outputEqual = false;
						}
					}
					if((rs1!=null && rs2 == null) || (rs1== null && rs2 !=null)){	
						outputEqual = false;
					}
					if(outputEqual){
						queryIds.add((String)Id);
					}
				}
				
			}
		//	if(conn != null)
		//		conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(input!=null)
				input.close();
		}
		System.out.println("--------------------------------------------- ");
		System.out.println("Dataset: "+datasetName+" Killed mutants: "+queryIds);
		
		System.out.println("--------------------------------------------- ");
		return queryIds;
	}
	
	public static String convertUpdateQueryToSelect(String queryString) throws Exception {
		
		String out="SELECT ";
		String tablename=queryString.trim().replaceAll("\\s+", " ").split(" ")[1];
		String st[]=queryString.split("=");		
		for(int i=1;i<st.length;i++){
			
			if(st[i].contains(",")){
				out+=" "+st[i].substring(0,st[i].indexOf(","))+",";
			}else if(st[i].toLowerCase().contains("where")){
				out+=" "+st[i].substring(0,st[i].toLowerCase().indexOf("where"))+" FROM "+tablename+" ";
				out+=st[i].substring(st[i].toLowerCase().indexOf("where"));
			}else{
				out+=" "+st[i]+" FROM "+tablename;
			}
			
		}
		return out;
	}
	public static String convertDeleteQueryToSelect(String queryString) {
		String out=queryString;
		out="SELECT * "+queryString.substring(queryString.toLowerCase().indexOf("from"),queryString.length());		
		return out;
	}
	public static String convertInsertQueryToSelect(String queryString) {
		String out=queryString;
		out=out.substring(queryString.toLowerCase().indexOf("select"), queryString.length());
		return out;
	}
	
	public static Vector<String> downloadDatasets(String queryId,Connection conn, String filePath, boolean onlyFirst) throws Exception{
		
		Vector <String>datasets=new Vector<String>();
		
		String getDatasetQuery = null;
		
		if(!onlyFirst){
			getDatasetQuery = "Select * from datasetvalue where queryid = ?";
		}
		else{
			getDatasetQuery = "Select * from datasetvalue where queryid = ? and datasetid = 'DS0'";
		}

		PreparedStatement smt;
		smt=conn.prepareStatement(getDatasetQuery);
		smt.setString(1, queryId);
		ResultSet rs=smt.executeQuery();
		while(rs.next()){
			
			String datasetid=rs.getString("datasetid");
			String datasetvalue=rs.getString("value");
			
			datasets.add(datasetid);
	
			
			String dsPath=Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetid;
			File f=new File(dsPath);
			if(!f.exists()){
				Runtime r = Runtime.getRuntime();
				Process proc = r.exec("mkdir "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetid );
		
				proc.waitFor();
				
				Utilities.closeProcessStreams(proc);
				
				proc.destroy();
			}
			else{
				Runtime r = Runtime.getRuntime();
				Process proc = r.exec("rm "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetid+"/*");
								
				proc.waitFor();
				Utilities.closeProcessStreams(proc);
				proc.destroy();
			}
			String copyfile[]=datasetvalue.split(":::");
			for(int i=0;i<copyfile.length;i++){
				String tablename=copyfile[i].split(".copy")[0];
				String tabledata=copyfile[i].split(".copy")[1];
				
				BufferedWriter brd = new BufferedWriter(new FileWriter(dsPath+"/"+tablename+".copy"));
				String line[]=tabledata.split("::");
				String writedata="";
				for(int j=0;j<line.length;j++){
					writedata+=line[j]+"\n";					
				}
				writedata=writedata.substring(0,writedata.length()-1);
				brd.write(writedata);
				brd.close();				
			}			
		}
		
		rs.close();
		smt.close();
		
		return datasets;
	}
	
	
	public QueryStatus testQueryAnswer(String qid, String query, String user, String filePath) throws Exception{
		
		Connection conn = MyConnection.getExistingDatabaseConnection();
		Connection testConn = MyConnection.getTestDatabaseConnection();
		PopulateTestData p = new PopulateTestData();
		HashMap<String,String> mutants = new HashMap<String,String>();
		mutants.put(qid, query);
		String qry = "select * from queryinfo where queryid = ?";		
		PreparedStatement pstmt = conn.prepareStatement(qry);
		pstmt.setString(1,qid);
		ResultSet rs = pstmt.executeQuery();
		
		String sqlQuery = null;
		if(rs.next()){
			sqlQuery=rs.getString("querystring");
		}
		else{
			conn.close();
			testConn.close();
			return QueryStatus.NoDataset;
		}
		
		boolean orderIndependent = rs.getBoolean(2);
		
		Vector<String>  datasets = downloadDatasets(qid, conn, filePath, true);
		
		if(datasets.size() == 0){
			conn.close();
			testConn.close();
			return QueryStatus.NoDataset;
		}
		
		QueryStatus status = QueryStatus.Error;
		boolean flag=true;
		
		for(int i=0;i<datasets.size();i++){
			
			String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
		 	File ds=new File(dsPath);
		 	GenerateCVC1 c = new GenerateCVC1();
			String copyFiles[] = ds.list();
			
			Vector<String> vs = new Vector<String>();
			for(int m=0;m<copyFiles.length;m++){
			    vs.add(copyFiles[m]);		    
			}
			
		 	// query output handling
			TableMap tm = ((GenerateCVC1) c).getTableMap().getInstances();
			p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn);
			Vector<String> cmismatch = new Vector<String>();
			System.out.println(datasets.get(i));
			Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, datasets.get(i), sqlQuery, dsPath, orderIndependent, cmismatch, testConn);
			System.out.println("******************");
			System.out.println(datasets.get(i) + " " + killedMutants.size());
			System.out.println("******");
			for(int l=0;l<killedMutants.size();l++){
				if(mutants.containsKey(killedMutants.get(l))){
					flag = false;
					status = QueryStatus.Incorrect;
					break;
				}
			}
			
			if(!flag){
				break;
			}
		 }
		
		if(flag){
			status = QueryStatus.Correct;
		}
		
		p.deleteAllTempTablesFromTestUser(testConn);
		conn.close();
		testConn.close();
		return status;
	}
	
	
public String testAnswerAgainstAllDatasets(Connection conn, Connection testConn, String qid, String query, String user, String filePath) throws Exception{
		
		String out="";
			
		HashMap<String,String> mutants = new HashMap<String,String>();
		mutants.put(qid, query);
		String qry = "select * from queryinfo where queryid = ?";		
		PreparedStatement pstmt = conn.prepareStatement(qry);
		pstmt.setString(1,qid);
		ResultSet rs = pstmt.executeQuery();	
		rs.next();
		String sqlQuery = rs.getString("querystring");
		boolean orderIndependent = rs.getBoolean(2);
		boolean incorrect = false;
	
		Vector<String> datasets = downloadDatasets(qid, conn, filePath, false);
	
		for(int i = 0; i < datasets.size(); i++){
			boolean flag = true;
			
			//load the contents of DS
			String dsPath = Configuration.homeDir + "/temp_cvc" + filePath + "/" + datasets.get(i);
			File ds=new File(dsPath);
		 	GenerateCVC1 c = new GenerateCVC1();
			String copyFiles[] = ds.list();
			
			Vector<String> vs = new Vector<String>();
			for(int m=0;m<copyFiles.length;m++){
			    vs.add(copyFiles[m]);
			}				 		
		 	
			TableMap tm = ((GenerateCVC1) c).getTableMap().getInstances();
			PopulateTestData p = new PopulateTestData();
			p.populateTestDataForTesting(vs, filePath + "/" + datasets.get(i), tm, testConn);
			Vector<String> cmismatch=new Vector<String>();
			System.out.println(datasets.get(i));
			Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, datasets.get(i), sqlQuery, dsPath, orderIndependent, cmismatch, testConn);
			System.out.println("******************");
			System.out.println(datasets.get(i) + " " + killedMutants.size());
			System.out.println("******");
			
			for(int l=0;l<killedMutants.size();l++){
				if(mutants.containsKey(killedMutants.get(l))){
					System.out.println("false" +killedMutants.get(l));
					incorrect = true;
					String qryUpdate = "update queries set verifiedcorrect = false where queryid = '"+qid+"' and rollnum = '"+user+"'";		
					PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate);
					pstmt2.executeUpdate();
					out+=datasets.get(i)+"::Failed:::";
					flag=false;
					pstmt2.close();
				}
			}
			if(flag){
				out+="Passed:::";
			}
		 }
		
		if(!incorrect){
			String qryUpdate = "update queries set verifiedcorrect = true where queryid = '"+qid+"' and rollnum = '"+user+"'";
			PreparedStatement pstmt3 = conn.prepareStatement(qryUpdate);
			pstmt3.executeUpdate();
			pstmt3.close();
		}
				
		return out.trim();		
	}
	

	public String testAnswer(String qid, String query, String user, String filePath) throws Exception{
		
		Connection conn = MyConnection.getExistingDatabaseConnection();
		Connection testConn = MyConnection.getTestDatabaseConnection();
		
		String out="";
		query=checkForViews(qid,query,user);
		
		String insertquery="INSERT INTO queries VALUES ('d1',?,?,?,true)";		
		
		HashMap<String,String> mutants = new HashMap<String,String>();
		mutants.put(qid, query);
		String qry = "select * from queryinfo where queryid = ?";		
		PreparedStatement pstmt = conn.prepareStatement(qry);
		pstmt.setString(1,qid);
		ResultSet rs = pstmt.executeQuery();	
		rs.next();
		String sqlQuery=rs.getString("querystring");
		boolean orderIndependent = rs.getBoolean(2);
		boolean incorrect=false;
		
		
		//COMMENTED FOR TAKING RESULTS UNCOMMENT IT AFTERWARD
		//delete previous dataset
		/*Runtime r = Runtime.getRuntime();
		File f=new File(Configuration.homeDir+"/temp_cvc"+filePath+"/");
		File f2[]=f.listFiles();
		for(int i=0;i<f2.length;i++){
			if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
				Process proc = r.exec("rm -rf "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+f2[i].getName());
			}
				
		}
		*/
		
		
		Vector<String>  datasets=downloadDatasets(qid,conn,filePath, false);
				
		
		for(int i=0;i<datasets.size();i++){
			
			boolean flag=true;
			//load the contents of DS
			String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
			//System.out.println(dsPath);
		 	File ds=new File(dsPath);
		 	GenerateCVC1 c = new GenerateCVC1();
			String copyFiles[] = ds.list();
			
			Vector<String> vs = new Vector<String>();
			for(int m=0;m<copyFiles.length;m++){
			    vs.add(copyFiles[m]);
			    //System.out.println("FILE NAME "+copyFiles[m]);
			    
			}				 		
		 	// query output handling
			TableMap tm = ((GenerateCVC1) c).getTableMap().getInstances();
			PopulateTestData p = new PopulateTestData();
			p.populateTestDatabase(vs, filePath+"/"+datasets.get(i), tm, testConn);
			Vector<String> cmismatch=new Vector<String>();
			System.out.println(datasets.get(i));
			Vector<String> killedMutants = mutantsKilledByDataset1(mutants,datasets.get(i), sqlQuery, dsPath,orderIndependent,cmismatch,testConn);
			System.out.println("******************");
			System.out.println(datasets.get(i)+" "+killedMutants.size());
			System.out.println("******");
			//System.out.println();
			for(int l=0;l<killedMutants.size();l++){
				if(mutants.containsKey(killedMutants.get(l))){
					//set the verifiedcorrect as false
					String qryUpdate = "update queries set verifiedcorrect = false where queryid = '"+qid+"' and rollnum = '"+user+"'";		
					PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate);
					pstmt2.executeUpdate();
					//mutants.remove(killedMutants.get(l));
					incorrect =true;
					System.out.println("false" +killedMutants.get(l));
					out+=datasets.get(i)+"::Failed:::";
					flag=false;
				}
			}
			if(flag){
				out+="Passed:::";
			}
		 }
		
		
		if(!incorrect){
			String qryUpdate = "update queries set verifiedcorrect = true where queryid = '"+qid+"' and rollnum = '"+user+"'";
			PreparedStatement pstmt3 = conn.prepareStatement(qryUpdate);
			pstmt3.executeUpdate();
			
		}
		
		
		
		insertquery="INSERT INTO score VALUES (?,?,?)";		
		try{
			PreparedStatement smt;
			smt=conn.prepareStatement(insertquery);
			smt.setString(1,qid);
			smt.setString(2,user);
			smt.setString(3,out.trim());
			smt.executeUpdate();
			
		}catch (SQLException e) {
			String update="update score set result = ? where queryid=? and rollnum=?;";
			PreparedStatement smt=conn.prepareStatement(update);
			smt.setString(1, out.trim());
			smt.setString(2,qid);
			smt.setString(3,user);			
			smt.executeUpdate();
			
		}
		
		testConn.close();
		conn.close();
		return out.trim();
		
	}
	
	public String checkForViews(String qid, String query, String user) throws Exception{
		
		Connection conn = MyConnection.getExistingDatabaseConnection();
		
		String out=query;
		PreparedStatement smt;
		
		String qry=query.replaceAll("\n"," ").replaceAll("\\s+", " ").toLowerCase();
		if(qry.startsWith("create view ")){
			isCreateView=true;
			
			String vname=query.substring(12).split("\\s")[0];
			String vquery=query.substring(12).split("\\s")[2];
			vquery=vquery.replaceAll("\n", " ").replaceAll("\\s+", " ").replaceAll("'", "''");
			String ins="INSERT INTO views VALUES ("+vname+",'"+user+"',"+vquery+");";			
			smt=conn.prepareStatement(ins);
			smt.executeUpdate();
			return out;
			
		}
		String q="Select * from views where rollnum = ?";
		conn = MyConnection.getExistingDatabaseConnection();
		smt=conn.prepareStatement(q);
		smt.setString(1, user);
		ResultSet rs=smt.executeQuery();
		HashMap<String, String> hm=new HashMap<String,String>();
		while(rs.next()){
			hm.put(rs.getString("vname"), rs.getString("viewquery"));
		}
		
		
		String newquery="";
        /*Add the select part to new query */
        StringTokenizer st=new StringTokenizer(query);                    
        String token=st.nextToken();        
        while(!token.equalsIgnoreCase("from")){
            
            newquery+=token+" ";
            token=st.nextToken();
        }

        newquery+="from ";
        /*Add the new from part*/
        while(st.hasMoreTokens()){
            token=st.nextToken();            
            if(token.equalsIgnoreCase("where")||token.equalsIgnoreCase("group")){
                newquery+=token+ " ";
                break;
            }
            if(token.equals(",")){
                newquery+=token+ " ";
            }
            if(token.contains(",")){
                String tablenames[]=token.split(",");
                for(int j=0;j<tablenames.length;j++){
                    boolean isPresent=false;
                    if(hm.containsKey(tablenames[j])){
                        newquery+=hm.get(tablenames[j]) + " " + tablenames[j]+" ";
                        isPresent=true;            
                    }
                    if(!isPresent){
                        newquery+=tablenames[j]+" ";
                    }
                    newquery+=" ,";
                }
                newquery=newquery.substring(0,newquery.length()-1);
                
            }else{
                boolean isPresent=false;
                if(hm.containsKey(token)){
                    newquery+=hm.get(token) + " " + token+" ";
                    isPresent=true;            
                }
                if(!isPresent){
                    newquery+=token+" ";
                }
            }
            
        }
        /*Add the remaning part of query*/
        while(st.hasMoreTokens()){
            token=st.nextToken();
            newquery+=token+ " ";
        }
        
        conn.close();
		return out;
	}
	public void test(String filePath, String quesID) throws Exception{
		
		String qry = "select * from queryinfo where queryid = '"+quesID+"'";    	
		Connection conn = MyConnection.getExistingDatabaseConnection();
		Connection testConn = MyConnection.getTestDatabaseConnection();
		PreparedStatement pstmt = conn.prepareStatement(qry);
		ResultSet rs = pstmt.executeQuery();	
		
		
		int counter = 0;
		while(rs.next()){
			
			
			//delete the previous datasets		
			Runtime r = Runtime.getRuntime();
			File f=new File(Configuration.homeDir+"/temp_cvc"+filePath+"/");
			File f2[]=f.listFiles();
			for(int i=0;i<f2.length;i++){
				if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
					Process proc = r.exec("rm -rf "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+f2[i].getName());
					Utilities.closeProcessStreams(proc);
				}
					
			}
			
			String id = rs.getString(1);
			
			boolean orderIndependent;
			//for postgres boolean is supported
			orderIndependent = rs.getBoolean(2);
			
			//for oracle
			//String st=rs.getString(2);
			//if(st.equalsIgnoreCase("false")||st.equalsIgnoreCase("f"))
			//	orderIndependent = false;
			//else
			//	orderIndependent = true;
			
			String englishDescription = rs.getString(4);			
			String sqlQuery = rs.getString(3);
			//System.out.println(sqlQuery);
			
			//Vector<String>  datasets=downloadDatasets(id,conn,filePath);
			
	        // datasets contain all the datasets		        
	        // Get all the mutants
	        //String getMutant = "select * from queries where queryid = "+id;
	        String getMutant = "select * from queries_new where queryid = '"+id+"'";
	        PreparedStatement pstmt1 = conn.prepareStatement(getMutant);
			ResultSet rs1 = pstmt1.executeQuery();
			HashMap<String,String> mutants = new HashMap<String,String>();
			while(rs1.next()){
				String queryvariantid = rs1.getString(3);
				String qryString = rs1.getString(4);
				mutants.put(queryvariantid, qryString);
			}
			Vector<String> planKilledMutants=mutantsKilledByQueryPlan(mutants,sqlQuery);
			for(int l=0;l<planKilledMutants.size();l++){
				//if(mutantsdups.containsKey(planKilledMutants.get(l))){
					String qryUpdate = "update queries_new set verifiedbyplan = false where queryid = '"+id+"' and rollnum = '"+planKilledMutants.get(l)+"'";
					PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate);
					pstmt2.executeUpdate();
					//mutantsdups.remove(planKilledMutants.get(l));
				//}
			}
			//all the mutants are in <mutants>
			//loop for each DS and 
			Vector<String>  datasets=downloadDatasets(quesID,conn,filePath, false);
			
			Vector<String> cmismatch=new Vector<String>();
			for(int i=0;i<datasets.size();i++){
				//load the contents of DS
				String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
				//System.out.println(dsPath);
			 	File ds=new File(dsPath);
			 	GenerateCVC1 c = new GenerateCVC1();
				String copyFiles[] = ds.list();
				
				Vector<String> vs = new Vector<String>();
				for(int m=0;m<copyFiles.length;m++){
				    vs.add(copyFiles[m]);
				    //System.out.println("FILE NAME "+copyFiles[m]);
				    
				}				 		
			 	// query output handling
				TableMap tm = ((GenerateCVC1) c).getTableMap().getInstances();
				PopulateTestData p = new PopulateTestData();
				
				p.populateTestDatabase(vs, filePath+"/"+datasets.get(i), tm, testConn);
				
				Vector<String> killedMutants = mutantsKilledByDataset1(mutants,datasets.get(i), sqlQuery, dsPath,orderIndependent,cmismatch, testConn);

				//System.out.println("******************");
				System.out.println(datasets.get(i)+" "+killedMutants.size());
				System.out.println();
				HashMap<String,String> mutantsdups=(HashMap<String, String>) mutants.clone();
				for(int l=0;l<killedMutants.size();l++){
					if(mutants.containsKey(killedMutants.get(l))){
						//set the verifiedcorrect as false
						// remove the entry from <mutants>
						//String qryUpdate = "update queries set verifiedcorrect = false where queryid = "+id+" and queryvariantid = "+killedMutants.get(l);
						//String qryUpdate = "update queries set verifiedcorrect = false where queryid = "+id+" and variantid = "+killedMutants.get(l);
						String qryUpdate = "update queries_new set verifiedcorrect = false where queryid = '"+id+"' and rollnum = '"+killedMutants.get(l)+"'";
						//String qryUpdate = "update queries set verifiedcorrect = false where queryid = "+id+" and rollnum = "+killedMutants.get(l);
						PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate);
						pstmt2.executeUpdate();
						mutants.remove(killedMutants.get(l));
//						System.out.println("false" +killedMutants.get(l));
					}
				}
				for(int l=0;l<cmismatch.size();l++){
					String qryUpdate = "update queries_new set columnmismatch = true where queryid = '"+id+"' and rollnum = '"+cmismatch.get(l)+"'";
					PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate);
					pstmt2.executeUpdate();
				}
				cmismatch.clear();
				//Thread.sleep(1000);
			 }
			Collection cl = mutants.keySet();
			Iterator itr = cl.iterator();
			int ctr=0;
			while(itr.hasNext()){
				String temp = (String) itr.next();
				int qvid=Integer.parseInt(temp);
				//System.out.println(temp);
				//String qryUpdate = "update queries set verifiedcorrect = true where queryid = "+id+" and rollnum = "+qvid;
				String qryUpdate = "update queries set verifiedcorrect = true where queryid = '"+id+"' and rollnum = '"+qvid+"'";
				//String qryUpdate = "update queries set verifiedcorrect = true where queryid = "+id+" and variantid = "+qvid;
				//String qryUpdate = "update queries set verifiedcorrect = true where queryid = "+id+" and queryvariantid = "+qvid;
				PreparedStatement pstmt3 = conn.prepareStatement(qryUpdate);
				pstmt3.executeUpdate();
				ctr++;
				//System.out.println("true");
			}
			System.out.println(ctr);
			//}
			counter+=1;
		}
		
		conn.close();
		testConn.close();
	}
	
	
	private Vector<String> mutantsKilledByQueryPlan(
			HashMap<String, String> mutants, String sqlQuery) {
		// TODO Auto-generated method stub
		Vector<String> queryIds = new Vector<String>();
		try {
			Connection conn = MyConnection.getTestDatabaseConnection();
			String OriginalPlan="explain analyze "+sqlQuery;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(OriginalPlan);
			OriginalPlan="";
			while(rs.next()){
				OriginalPlan+=rs.getString(1)+"\n";
				int last=OriginalPlan.lastIndexOf("(actual time");
				if(last != -1)
					OriginalPlan=OriginalPlan.substring(0, last);
				last=OriginalPlan.lastIndexOf("Total runtime");
				if(last!= -1)
					OriginalPlan=OriginalPlan.substring(0, last);
			}

			String mutant_query="";
			Collection c = mutants.keySet();
			Iterator itr = c.iterator();
			while(itr.hasNext()){
				Object Id = itr.next();
				//System.out.println("\nId="+Id);
				String mutant_qry = mutants.get(Id);
				
				//System.out.println("Student: "+mutant_qry);
				mutant_qry=mutant_qry.trim().replace(';', ' ');				
				mutant_qry="explain analyze "+mutant_qry;
				Statement pstmt=conn.createStatement();
				try{
					ResultSet rset=pstmt.executeQuery(mutant_qry);
					String mutant_plan="";
					while(rset.next()){
						mutant_plan+=rset.getString(1)+"\n";
						int last=mutant_plan.lastIndexOf("(actual time");
						if(last != -1)
							mutant_plan=mutant_plan.substring(0, last);
						last=mutant_plan.lastIndexOf("Total runtime");
						if(last!= -1)
							mutant_plan=mutant_plan.substring(0, last);
					}
					if(!OriginalPlan.equalsIgnoreCase(mutant_plan)){
						queryIds.add((String)Id);
					}
				} catch (Exception e){
					queryIds.add((String)Id);
				}
			}
			
			conn.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryIds;
	}
	public void generateResults(String file, BufferedWriter bfrd) throws Exception {			//bfrd written to write no of incorrect queries. Delete if it gives errors
		
		Connection conn = MyConnection.getExistingDatabaseConnection();

		String filePath = "4";			
		//String fileName="OldAssignment/Assign"+file;
		//String fileName="Assignment/Assign"+file;
		String fileName="NewAssignment/Assign"+file;
		//String strFile = Configuration.homeDir+"/temp_cvc"+filePath+"/CS_387_Quiz_0.csv";
		String strFile = Configuration.homeDir+"/temp_cvc"+filePath+"/"+fileName;
		
		
		Scanner sc = new Scanner(new File(strFile));
		sc.useDelimiter("\\s*:\\s*");
		//sc.nextLine();
		
		sc.next();sc.next();sc.next();
		
		int rollno=1;
		String qID=sc.next();
		String quesDesc=sc.next();
		String quesID; 
		
		
		
		
		while(sc.hasNext()){
			int total=0,incorrect=0;
			quesID=qID.trim();
//			System.out.println("quesID " + quesID);
			if(quesID.startsWith("'extra'")){
				quesID=qID.substring(8);					
			}
			
			System.out.println("Question Number "+quesID);
			System.out.println("Question Description is "+quesDesc);
			sc.next();
			sc.next();
			sc.next();
			datasets = downloadDatasets(quesID.substring(1, quesID.length()-1),conn,filePath, false);
			
			do{
				String query=sc.next();
				query=query.replaceAll(";", " ").trim();				
				query=query.substring(1,query.length()-1).replaceAll("\n", " ").replaceAll("''","'").trim();
				System.out.println(rollno+") : "+query);
				rollno++;
				/*try{
					PreparedStatement smt;
					Connection conn = (new MyConnection()).getExhistingDatabaseConnection();
					smt=conn.prepareStatement(query);
					smt.executeQuery();
					
				}catch(Exception e){
					
					e.printStackTrace();
				}*/
				try{
					total++;
//					System.out.println("total " + total);
					String res=testAnswer(quesID.substring(1, quesID.length()-1),query,rollno+"",filePath);
					System.out.println("Result "+res);
					if(res.contains("Failed")){
						incorrect++;
						System.out.println("incorrect " + incorrect);
					}
					System.out.println();
				}catch(Exception e){
					System.out.println("Result Failed");
					incorrect++;
					System.out.println("incorrect " + incorrect);
					e.printStackTrace();
				}
				
				if(sc.hasNext()){
					qID=sc.next();
					quesDesc=sc.next();
				}else{
					break;	//end of file
				}
			}while(qID.length()==0);	
			bfrd.write(quesID + " Total Queries :" + total + " ");
			int correct = total - incorrect;
			bfrd.write(quesID + " Correct Queries :" + correct + " ");
			bfrd.write(quesID + " Incorrect Queries :" + incorrect + "\n");
		}
		
		conn.close();
	}

	public static void main(String args[])throws Exception{
		
//			System.out.println("Started");
			/*Connection conn = (new MyConnection()).getExhistingDatabaseConnection();
			PreparedStatement smt;
			String filePath = "4";			
			String strFile = Configuration.homeDir+"/temp_cvc"+filePath+"/CS_387_Quiz_0.csv";
			
			
			Scanner sc = new Scanner(new File(strFile));
			sc.useDelimiter("\\s*:\\s*");
			//sc.nextLine();
			
			sc.next();sc.next();sc.next();
			
			int rollno=1;
			String qID=sc.next();
			String quesDesc=sc.next();
			String quesID; 
			
			while(sc.hasNext()){
				
				quesID=qID;
				if(quesID.startsWith("'extra'")){
					quesID=qID.substring(8);					
				}
				quesID=""+quesID.charAt(2);
				
				String insertquery="INSERT INTO queries VALUES ('d1',"+quesID+",'"+rollno+"',";
				System.out.println("Question Number "+quesID);
				System.out.println("Question Description is "+quesDesc);
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
					insertquery+=parsedquery+",true);";
					
					System.out.println("INSERT Query ===> "+insertquery);
					smt=conn.prepareStatement(insertquery);
					smt.executeUpdate();
					
					
					rollno++;
					insertquery="INSERT INTO queries VALUES ('d1',"+quesID+",'"+rollno+"',";
					if(sc.hasNext()){
						qID=sc.next();
						quesDesc=sc.next();						
					}else{
						break;	//end of file
					}
				}while(qID.length()==0);			
			}
			new TestAnswer().test(filePath);
			//PopulateTestData.populateTestDatabase(vs, Configuration.fpath+"/"+DS, tm);			 
	        /*
	         * For each query in queriinfo
	         * 	Get all its mumtants from queries table and store it in <mutants>
	         * 	For each mutants in <mutant>
	         * 		For each DS
	         * 			Upload data--Copy Files
	         * 			if(OrderDependent)
	         * 			{
	         * 				killedFlag = true;
	         * 			}
	         * 			else
	         * 			{
	         * 				--
	         * 			}
	         * 		if(killedFlag){
	         * 			update correctness in the table
	         * 		}	
	         * 			
	         */
			
	        
			TestAnswer t=new TestAnswer();
			String filePath = "4";
			
//			BufferedWriter bfrd = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/results.txt"));
////			t.generateResults(0 + "/" + 3 + ".csv", bfrd);
////			t.generateResults(1 + "/" + 7 + ".csv", bfrd);
////			t.generateResults(1 + "/" + 8 + ".csv", bfrd);
//			for(int i = 0; i < 3; i++)
//				for(int j = 1; j <= 5; j++)
//				{
//					//long start=System.currentTimeMillis();
//					//t.generateResults("0/1.csv", bfrd);
////					if(i == 2 && j == 2)
//						t.generateResults(i + "/" + j + ".csv", bfrd);
//					//long end=System.currentTimeMillis();
//					//bfrd.write(i + " " + j + "**********"+start);
//					//bfrd.write(i + " " + j + "**********"+end);
//					//bfrd.write(i + " " + j + "**********"+(end-start));
//					//bfrd.write(i + " " + j + " COMPLETED\n\n\n");
//				}
//			bfrd.close();
			
			/*start=System.currentTimeMillis();
			t.generateResults("2/6.csv");
			end=System.currentTimeMillis();
			System.out.println("**********"+start);
			System.out.println("**********"+end);
			System.out.println("**********"+(end-start));
			System.out.println("2 COMPLETED\n\n\n");
			
			/*start=System.currentTimeMillis();
			t.generateResults("2/4.csv");
			end=System.currentTimeMillis();
			System.out.println("**********"+start);
			System.out.println("**********"+end);
			System.out.println("**********"+(end-start));
			System.out.println("3 COMPLETED\n\n\n");
			*/
			
//			String s=t.testAnswer("Q16","with s as (select id, name, course_id, semester, year, day, start_hr, start_min, end_hr, end_min from student natural join takes natural join section natural join time_slot) SELECT A.id, A.name from s A, s B where A.id = B.id and A.course_id != B.course_id and A.semester = B.semester and A.year = B.year and A.day=B.day and not((A.end_hr < B.start_hr or (A.end_hr = B.start_hr and A.end_min <= B.start_min)) or (A.start_hr > B.end_hr or (A.start_hr = B.end_hr and A.start_min >= B.end_min)))", "student","4");
//			String s=t.testAnswer("Q17","select course_id, count(id) from course natural left outer join takes group by course_id", "student","4");
//			String s=t.testAnswer("Q18","select time_slot_id from time_slot where time_slot_id not in (select time_slot_id from section natural join takes where id in (select id from takes where course_id = 'CS-101' and semester = 'Spring' and year = 2010) and semester = 'Spring' and year = 2010)", "student","4");
//			String s=t.testAnswer("Q20","select dept_name from course group by dept_name", "student","4");
//			s=t.testAnswer("Q21","select sum(credits) from course group by dept_name", "student","4");
			//String s=t.testAnswer("Q22","select sum(tot_cred) from student natural join course group by name having count(tot_cred) = 2", "student","4");
			//String s=t.testAnswer("Q232","select min(sec_id) from (select course_id as T,sec_id from teaches where year=2009  group by  course_id, sec_id, semester, year ) as temp where T='CS-101'", "student","4");
			//String s=t.testAnswer("Q18"," select time_slot_id from section natural join takes where id not in (select id from takes where course_id = 'CS-101' and semester = 'Spring' and year = 2010)", "student","4");
			//String s=t.testAnswer("9","with A as (SELECT ID, name, semester, year, time_slot_id FROM student natural join takes natural join section) SELECT distinct ID, name FROM A group by ID, name, semester, year, time_slot_id having count (*) > 1", "student","4");
			//String s = t.testAnswer("14","SELECT DISTINCT * FROM takes t WHERE (NOT EXISTS (SELECT id,course_id FROM takes s WHERE s.grade != 'F' AND t.id = s.id ) AND t.grade IS NOT NULL) OR (grade != 'F' AND     grade IS NOT NULL)", "student","4");
//			/String s=t.testAnswer("Q91","SELECT takes.id FROM takes, section", "student","4");
//			String s=t.testAnswer("Q91","SELECT count(*) FROM time_slot", "student","4");
//			String s=t.testAnswer("Q23","select id, course_id from student natural join course", "student","4");
//			String s=t.testAnswer("Q24","select sum(tot_cred) from student natural join course where credits + 10 = tot_cred group by credits", "student","4");
			
			
			/**Mutation Killing Experiments*/
			//String s=t.testAnswer("M1","SELECT * FROM (SELECT id,time_slot_id,section.year,section.semester FROM takes, section GROUP BY id,time_slot_id,section.year,section.semester HAVING COUNT(time_slot_id)>1) as s", "student","4");
			//String s=t.testAnswer("M1","SELECT * FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING COUNT(time_slot_id) = 1) as s", "student","4");
			//String s=t.testAnswer("M1","SELECT * FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester,sec_id HAVING COUNT(time_slot_id) > 1) as s", "student","4");
			//This mutation is not suitable String s=t.testAnswer("M1","SELECT * FROM (SELECT id,time_slot_id,year FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year HAVING COUNT(time_slot_id) > 1) as s", "student","4");
			
			
			//String s=t.testAnswer("M2","SELECT distinct dept_name FROM course WHERE credits IN (SELECT SUM(credits) FROM course , department WHERE  title='CS' GROUP BY dept_name,   building HAVING COUNT(course_id)> 2)", "student","4");
			//String s=t.testAnswer("M2","SELECT distinct dept_name FROM course WHERE credits IN (SELECT SUM(credits) FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY dept_name, building HAVING COUNT(course_id) < 2)", "student","4");
			//String s=t.testAnswer("M2","SELECT distinct dept_name FROM course WHERE credits IN (SELECT SUM(credits) FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY  building HAVING COUNT(course_id)> 2)", "student","4");
			String s=t.testAnswer("M2","SELECT distinct dept_name FROM course WHERE credits IN (SELECT SUM(distinct credits) FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY dept_name, building HAVING COUNT(course_id)> 2)", "student","4");



			
					
			System.out.println("Finished" +s);
			}
	
			
	
	
	
	
}