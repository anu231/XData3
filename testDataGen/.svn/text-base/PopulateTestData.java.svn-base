package testDataGen;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.lang.Runtime;
import java.net.URLDecoder;















/*import mutation.Mutant;
import mutation.Mutation;*/
import parsing.*;
import util.*;
 
class CallableProcess implements Callable {
    private Process p;

    public CallableProcess(Process process) {
        p = process;
    }

    public Integer call() throws Exception {
        return p.waitFor();
    }
}


public class PopulateTestData {

	public String getParameterMapping(HashMap<String,Node> paramConstraints, HashMap<String, String> paramMap){
		String retVal = "------------------------\nPARAMETER MAPPING\n------------------------\n";
		Iterator itr = paramConstraints.keySet().iterator();
		retVal += paramMap.toString() + "\n\n";
		while(itr.hasNext()){
			String key = (String)itr.next();
			retVal += "CONSTRAINT: "+paramConstraints.get(key)+"\n";
		}
		return retVal;
	}
	
	public void fetechAndPopulateTestDatabase(Connection dbcon, Connection testCon, String query_id,String dataset_id,TableMap tableMap) throws Exception{
		
		String dataset_query="select value from datasetvalue where datasetid =? and queryid=?";
		PreparedStatement dstmt = dbcon.prepareStatement(dataset_query);
		dstmt.setString(1,dataset_id);
		dstmt.setString(2, query_id);
		System.out.println("Dataset_id is :"+dataset_id);
		System.out.println("Query_id is :"+query_id);
		ResultSet dset=dstmt.executeQuery();
		dset.next();
		String dataset=dset.getString("value");
		Map<String , String> tables=new HashMap<String , String>();
		String[] table=dataset.split(":::");
		for(String temp: table)
		{
			System.out.println("table String:::::::::::::::::::::::::::"+temp);
			String tname=temp.substring(0, temp.indexOf(".copy"));
			String values=temp.substring(temp.indexOf(".copy")+5);
			tables.put(tname, values);
		}
	
		int size = tableMap.foreignKeyGraph.topSort().size();
		for (int i=(size-1);i>=0;i--){
			String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
				String del="delete from "+tableName;
				System.out.println("DELETE::::::::::::::::::::::::"+del);
				PreparedStatement stmt=testCon.prepareStatement(del);
				try{
					stmt.executeUpdate();
				}catch(Exception e){
					e.printStackTrace();
				}
		}
		for(int i=0;i<tableMap.foreignKeyGraph.topSort().size();i++){
			String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
			if(tables.containsKey(tableName)){
				//String sel="create temporary table "+tableName+" as select * from "+tableName+" where 1=0";
/*				String del="delete from "+tableName;
				PreparedStatement stmt=testCon.prepareStatement(del);
				stmt.executeUpdate();
				*/
				String value=tables.get(tableName);
				String rows[]=value.split("::");
				for(String row:rows)
				{
					row=row.replaceAll("\\|", "','");
					String insert="insert into "+tableName+" Values ('"+row+"')";
					System.out.println("Insert statement:::::::::::::::::::::::"+insert);
					try{
						PreparedStatement inst=testCon.prepareStatement(insert);
						inst.executeUpdate();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}		
	}
	
	public void fetechAndPopulateTestDatabase(String query_id,String dataset_id,TableMap tableMap) throws Exception{
		Connection dbcon = MyConnection.getExistingDatabaseConnection();
		Connection testCon = MyConnection.getTestDatabaseConnection();
		
		fetechAndPopulateTestDatabase(dbcon, testCon, query_id, dataset_id, tableMap);
		
		dbcon.close();
		testCon.close();
	}
	
	
	public void captureACPData(String cvcFileName, String filePath, HashMap<String, Node> constraintsWithParams, HashMap<String, String> paramMap) throws Exception{
		String outputFileName = generateCvcOutput(cvcFileName, filePath);
		String copystmt = "";
		BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + outputFileName));
		String line = null; 
		File ACPFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/" + "PARAMETER_VALUES");
		if(!ACPFile.exists()){
			ACPFile.createNewFile();
		}
		copystmt = getParameterMapping(constraintsWithParams, paramMap);
		copystmt += "\n\n------------------------\nINSTANTIATIONS\n------------------------\n";
		setContents(ACPFile, copystmt+"\n", false);
		while (( line = input.readLine()) != null){
			if(line.contains("ASSERT (PARAM_")){//Output value for a parameterised aggregation
				String par = line.substring(line.indexOf("(PARAM_")+1, line.indexOf('=')-1);
				String val = line.substring(line.indexOf('=')+1,line.indexOf(')'));
				val = val.trim();
				copystmt = par + " = " + val;
				setContents(ACPFile, copystmt+"\n", true);
				//Now update the param map
				Iterator itr = paramMap.keySet().iterator();
				while(itr.hasNext()){
					String key = (String)itr.next();
					if(paramMap.get(key).equalsIgnoreCase(par)){
						paramMap.put(key, val);
					}
				}
			}				
		}
	}
	
	public String generateCvcOutput(String cvcFileName, String filePath){
		int ch;
		try{
			//Executing the CVC file generated for given query
			Runtime r = Runtime.getRuntime();
			Process myProcess = r.exec("cvc3 "+Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcFileName);				

			ExecutorService service = Executors.newSingleThreadExecutor();
			
		    try {
		        
		        
		        InputStreamReader myIStreamReader = new InputStreamReader(myProcess.getInputStream());

				//Writing output to .out file
				BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcFileName.substring(0,cvcFileName.lastIndexOf(".cvc")) + ".out"));
				while ((ch = myIStreamReader.read()) != -1) 
				{ 
					out.write((char)ch); 
				} 
				Callable<Integer> call = new CallableProcess(myProcess);
			    Future<Integer> future = service.submit(call);
			    int exitValue = future.get(60, TimeUnit.SECONDS);
				
				Utilities.closeProcessStreams(myProcess);
				
				out.close();
		        
		    } catch (ExecutionException e) {
		    	Utilities.closeProcessStreams(myProcess);
		        throw new Exception("Process failed to execute", e);
		    } catch (TimeoutException e) {
		    	Utilities.closeProcessStreams(myProcess);
		    	myProcess.destroy();		    	
		        throw new Exception("Process timed out", e);
		    } finally {
		        service.shutdown();
		    }			

		}catch(Exception e){
			e.printStackTrace();
		}

		return cvcFileName.substring(0,cvcFileName.lastIndexOf(".cvc")) + ".out";
	}

	public String cutRequiredOutput(String cvcOutputFileName, String filePath){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/cut_" + cvcOutputFileName));
			out.close();
			File testFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/cut_" + cvcOutputFileName);
			BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcOutputFileName));
			try {
				String line = null; 
				while (( line = input.readLine()) != null){
					if(line.contains("ASSERT (O_") && line.contains("] = (") && !line.contains("THEN")){
						setContents(testFile, line+"\n", true);
					}
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		return "cut_"+cvcOutputFileName;
	}

	public void setContents(File aFile, String aContents, boolean append)throws FileNotFoundException, IOException {
		if (aFile == null) {
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!aFile.exists()) {
			throw new FileNotFoundException ("File does not exist: " + aFile);
		}
		if (!aFile.isFile()) {
			throw new IllegalArgumentException("Should not be a directory: " + aFile);
		}
		if (!aFile.canWrite()) {
			throw new IllegalArgumentException("File cannot be written: " + aFile);
		}

		Writer output = new BufferedWriter(new FileWriter(aFile,append));
		try {
			output.write( aContents );
		}
		finally {
			output.flush();
			output.close();
		}
	}

	//Modified by Bhupesh
	public Vector<String> generateCopyFile (String cut_cvcOutputFileName, String filePath, 
			HashMap<String, Integer> noOfOutputTuples, TableMap tableMap,Vector<Column> columns) throws Exception {
		Vector<String> listOfCopyFiles = new Vector();
		String currentCopyFileName = "";
		File testFile = null;
		BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cut_cvcOutputFileName));
		String line = null,copystmt=null; 
		while (( line = input.readLine()) != null){
			//System.out.println("Discarding more than:"+noOfOutputTuples.get(line.substring(line.indexOf("_")+1,line.indexOf("["))));
			String tableName = line.substring(line.indexOf("_")+1,line.indexOf("["));
			if(!noOfOutputTuples.containsKey(tableName)){
				continue;
			}
			int index = Integer.parseInt(line.substring(line.indexOf('[')+1, line.indexOf(']')));
			if((index > noOfOutputTuples.get(tableName)) || (index <= 0)){
				continue;
			}
			currentCopyFileName = line.substring(line.indexOf("_")+1,line.indexOf("["));
			testFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/" + currentCopyFileName + ".copy");
			if(!testFile.exists() || !listOfCopyFiles.contains(currentCopyFileName + ".copy")){
				if(testFile.exists()){
					testFile.delete();
				}
				testFile.createNewFile();
				listOfCopyFiles.add(currentCopyFileName + ".copy");
			}
			copystmt = getCopyStmtFromCvcOutput(line);
			
			////Putting back string values in CVC
			
			Table t=tableMap.getTable(tableName);
			
			String[] copyTemp=copystmt.split("\\|");
			copystmt="";
			String out="";
			for(int i=0;i<copyTemp.length;i++){
				
				String cvcDataType=t.getColumn(i).getCvcDatatype();
				if(cvcDataType.equalsIgnoreCase("INT") )
					continue;
				else if(cvcDataType.equalsIgnoreCase("REAL")){
					String str[]=copyTemp[i].trim().split("/");
					if(str.length==1)
						continue;
					double num=Integer.parseInt(str[0]);
					double den=Integer.parseInt(str[1]);
					copyTemp[i]=(num/den)+"";
				}
				else if(cvcDataType.equalsIgnoreCase("TIMESTAMP")){
					long l=Long.parseLong(copyTemp[i].trim())*1000;
					java.sql.Timestamp timeStamp=new java.sql.Timestamp(l);
					copyTemp[i]=timeStamp.toString();
				}
				else if(cvcDataType.equalsIgnoreCase("TIME")){
					//long l=Long.parseLong(copyTemp[i].trim())*1000+86400;
					//java.sql.Time time=new java.sql.Time(l);
					int time=Integer.parseInt(copyTemp[i].trim());
					int sec=time%60;
					int min=((time-sec)/60)%60;
					int hr=(time-sec+min*60)/3600;
					copyTemp[i]=hr+":"+min+":"+sec;
				}
				else if(cvcDataType.equalsIgnoreCase("DATE")){
					long l=(long)Long.parseLong(copyTemp[i].trim())*86400000;
					
					java.sql.Date date=new java.sql.Date(l);
					copyTemp[i]=date.toString();
					
				}
				else {
					/*for(Column c:columns){
					if(c.getColumnName().equals(cvcDataType)){
						if(copyTemp[i].trim().equals("")) break;	//null value
						int pos=Integer.parseInt(copyTemp[i].trim());
						copyTemp[i]=c.getColumnValues().get(pos);
						break;
					}*/
					String copyStr=copyTemp[i].trim();
					
					
					if(copyStr.endsWith("__"))
						copyStr = "";
					else if(copyStr.contains("__"))
						copyStr = copyStr.split("__")[1];
					
					
					/*&copyStr = copyStr.replace("_p", "+");
					copyStr = copyStr.replace("_m", "-");
					copyStr = copyStr.replace("_a", "&");
					copyStr = copyStr.replace("_s", " ");
					copyStr = copyStr.replace("_d", ".");
					copyStr = copyStr.replace("_c", ",");
					copyStr = copyStr.replace("_u", "_");*/
					
					copyStr = copyStr.replace("_p", "%");
					copyStr = copyStr.replace("_s", "+");
					copyStr = copyStr.replace("_d", ".");
					copyStr = copyStr.replace("_m", "-");
					copyStr = copyStr.replace("_s", "*");
					copyStr = copyStr.replace("_u", "_");
					copyStr = URLDecoder.decode(copyStr,"UTF-8");
					copyTemp[i]=copyStr.replace("_b", " ");
					
				}
				
				
			}
			for(String s:copyTemp){
				copystmt+=s+"|";
			}
			copystmt=copystmt.substring(0, copystmt.length()-1);
			/////////////////
			
			
			//System.out.println("Copy Statement "+copystmt);
			
			
			
			setContents(testFile, copystmt+"\n", true);
		}
		input.close();
		return listOfCopyFiles;
	}
		
	
	/*
	public Vector<String> generateCopyFile(String cut_cvcOutputFileName, String filePath){
		Vector<String> listOfCopyFiles = new Vector();
		String currentCopyFileName = "";
		File testFile = null;
		
		try {
			BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cut_cvcOutputFileName));
			try {
				String line = null,copystmt=null; 
				while (( line = input.readLine()) != null){
					//System.out.println("hereeeeeeeee:" +line + " ::::::  " + currentCopyFileName);
					//System.out.println(line.indexOf("_"));
					//System.out.println(line.indexOf("["));
					
					if(!currentCopyFileName.equalsIgnoreCase(line.substring(line.indexOf("_")+1,line.indexOf("[")))){
						currentCopyFileName = line.substring(line.indexOf("_")+1,line.indexOf("["));
						BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/" + currentCopyFileName + ".copy"));
						out.close();
						testFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/" + currentCopyFileName + ".copy");
						listOfCopyFiles.add(currentCopyFileName + ".copy");
					}
					copystmt = getCopyStmtFromCvcOutput(line);
					setContents(testFile, copystmt+"\n");
				}
			}
			finally {
				input.close();
			}
		}catch (IOException ex){
			ex.printStackTrace();
		}
		return listOfCopyFiles;
	}
*/
	public String getCopyStmtFromCvcOutput(String cvcOutputLine){
		String queryString = "";
		String tableName = cvcOutputLine.substring(cvcOutputLine.indexOf("_")+1,cvcOutputLine.indexOf("["));
		String temp = cvcOutputLine.substring(cvcOutputLine.indexOf("(")+1);
		String insertTupleValues = temp.substring(temp.indexOf("(")+1,temp.indexOf(")"));
		insertTupleValues = cleanseCopyString(insertTupleValues);		
		return insertTupleValues;
	}
	
	public String cleanseCopyString(String copyStr){
		
		//System.out.println("***********************888"+copyStr);
		
		
		copyStr = copyStr.replaceAll("\\b_", "");
		copyStr = copyStr.replaceAll("\\bNULL_\\w+", "");
		copyStr = copyStr.replaceAll("\\-9999[6789]", "");
		copyStr = copyStr.replace(",", "|");
		
		return copyStr;
	}
	
	public void populateTestDataForTesting(Vector<String> listOfCopyFiles, String filePath, TableMap tableMap, Connection conn){
		try{						
			deleteAllTempTablesFromTestUser(conn);
			
			for(int i=0;i<tableMap.foreignKeyGraph.topSort().size();i++){
				String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
				if(listOfCopyFiles.contains(tableName+".copy")){
					listOfCopyFiles.remove(tableName+".copy");
					String copyFile = tableName+".copy";

					BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
					String str;
					String data="";
					while((str = br.readLine())!=null){
						data+=str+"@@";
					}
					
					uploadToTestDataToTempTables(copyFile.substring(0, copyFile.indexOf(".copy")), data, filePath, conn);
					br.close();
				}
			}
			for(int i=0;i<listOfCopyFiles.size();i++){
				String copyFile = listOfCopyFiles.get(i);
				BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
				String str;
				String data="";
				while((str = br.readLine())!=null){
					data+=str+"@@";
				}
				
				uploadToTestDataToTempTables(copyFile.substring(0,copyFile.indexOf(".copy")), data, filePath, conn);
				br.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void populateTestDatabase(Vector<String> listOfCopyFiles, String filePath, TableMap tableMap, Connection conn){
		try{			
			deleteAllTablesFromTestUser();
			
			//for(int i=0; i<listOfCopyFiles.size(); i++){
			for(int i=0;i<tableMap.foreignKeyGraph.topSort().size();i++){
				String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
				if(listOfCopyFiles.contains(tableName+".copy")){
					listOfCopyFiles.remove(tableName+".copy");
					String copyFile = tableName+".copy";
//					System.out.println(tableName);
					BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
					String str;
					String data="";
					while((str = br.readLine())!=null){
//						System.out.println(str);
						data+=str+"@@";
					}
					
					ArrayList<Boolean> success = uploadToTestUser(copyFile.substring(0,copyFile.indexOf(".copy")) , data, filePath);
					
					br.close();
					
		            BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
		            
		            String t[] = data.split("@@");
		    		for(int j = 0; j < t.length; j++){
		    			 if(success.get(j) == true){
		    				 out.write(t[j] + '\n');
		    			 }
		    		}
		            out.flush();
		            out.close();
				}
			}
			for(int i=0;i<listOfCopyFiles.size();i++){
				String copyFile = listOfCopyFiles.get(i);
//				System.out.println(copyFile.substring(0,copyFile.indexOf(".copy")));
				BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
				String str;
				String data="";
				while((str = br.readLine())!=null){
//					System.out.println(str);
					data+=str+"@@";
				}
				
				
				ArrayList<Boolean> success = uploadToTestUser(copyFile.substring(0,copyFile.indexOf(".copy")) ,data, filePath);
				
				br.close();
				
	            BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
	            
	            String t[] = data.split("@@");
	    		for(int j = 0; j < t.length; j++){
	    			 if(success.get(j) == true){
	    				 out.write(t[j] + '\n');
	    			 }
	    		}
	            out.flush();
	            out.close();
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void uploadToTestDataToTempTables(String tablename,String copyFileData,String filePath, Connection conn) throws Exception{
		
			String create="CREATE TEMPORARY TABLE "+tablename+" AS (SELECT * FROM "+tablename+" WHERE (1=0))";
			try{
				Statement st = conn.createStatement();
				st.execute(create);
			}
			catch(SQLException e){
				System.out.println(e.getMessage());
			}
			
			String t[]=copyFileData.split("@@");
			for(int i=0;i<t.length;i++){			
				t[i]=t[i].replaceAll("\\|", "','");
				try{
					PreparedStatement smt=conn.prepareStatement("Insert into "+ tablename+" Values ('"+t[i]+"')");
					smt.executeUpdate();
				}catch(Exception e){
					System.out.println("Error in "+tablename+"Insert into "+ tablename+" Values ('"+t[i]+"')");
					e.printStackTrace();
				}
				
			}				
		}
	
	public ArrayList<Boolean> uploadToTestUser(String tablename,String copyFileData,String filePath) throws Exception{
		
		ArrayList<Boolean> success = new ArrayList<Boolean>();
		
		Connection conn = MyConnection.getTestDatabaseConnection();				
		String t[]=copyFileData.split("@@");
		for(int i=0;i<t.length;i++){			
			t[i]=t[i].replaceAll("\\|", "','");
			try{
				PreparedStatement smt=conn.prepareStatement("Insert into "+ tablename+" Values ('"+t[i]+"')");
				smt.executeUpdate();
				success.add(true);
			}catch(Exception e){
				System.out.println("Error in "+tablename+"Insert into "+ tablename+" Values ('"+t[i]+"')");
				success.add(false);
				e.printStackTrace();
			}
			
		}
		
		conn.close();
		
		return success;
	}
	
	public void deleteAllTempTablesFromTestUser(Connection dbConn) throws Exception{
		Statement st = dbConn.createStatement();
		st = dbConn.createStatement();
		st.execute("DISCARD TEMPORARY");
	}
	
	public void deleteAllTablesFromTestUser() throws Exception{
		Connection conn = MyConnection.getTestDatabaseConnection();
		DatabaseMetaData dbm = conn.getMetaData();
		String[] types = {"TABLE"};
		ResultSet rs = dbm.getTables(null,Configuration.testDatabaseUser,"%",types);		  
		  
		while(rs.next()){
			String table=rs.getString("TABLE_NAME");			
			//PreparedStatement pstmt = conn.prepareStatement("delete from "+table);						
			PreparedStatement pstmt = conn.prepareStatement("Truncate table "+table +" cascade");
			pstmt.executeUpdate();
			
		}
		
		conn.close();
	}
	
	public void deleteDatasets(String filePath) throws Exception{
		Runtime r = Runtime.getRuntime();
		File f=new File(Configuration.homeDir+"/temp_cvc"+filePath+"/");
		File f2[]=f.listFiles();
		for(int i=0;i<f2.length;i++){
			if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
				Process proc = r.exec("rm -rf "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+f2[i].getName());
				Utilities.closeProcessStreams(proc);		    	
			}				
		}
	}
	
	
	//Mahesh: commented the following three methods
/*	public Vector<Mutant> generateJoinMutants(Query query){
		Vector<Mutant> mutants = new Vector<Mutant>();
		try{
			// commented for time being
//			mutants = Mutation.getAllMutants(query.getParsedQuery());
			
//			for(int i=0; i<mutants.size(); i++){
//				System.out.println("mutant = " + mutants.get(i).getString());
//			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return mutants;
	}
	
	public String mutantsKilledByDataset(Vector<Mutant> mutants, String datasetName, String queryString, String filePath) throws IOException{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection conn = null;
		String temp="";
		BufferedReader input=null;
		try{
			conn = MyConnection.getTestDatabaseConnection();
			Thread.sleep(500);
			pstmt = conn.prepareStatement("delete from dataset");
			pstmt.executeUpdate();
			Thread.sleep(500);
			pstmt = conn.prepareStatement("insert into dataset values('" + datasetName + "')");
			//System.out.println(pstmt);
			pstmt.executeUpdate();
			Thread.sleep(500);
			
//			for(int i=1; i<mutants.size(); i++){
//				System.out.println("Q"+ i + " : " + mutants.get(i).getString());
//			}
			// Auto generated Mutants 
			
			for(int i=1; i<mutants.size(); i++){
				pstmt = conn.prepareStatement("select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists (("+ queryString +") except ("+ mutants.get(i).getString() +")) or exists (("+ mutants.get(i).getString() +") except (" + queryString + "))");
				rs = pstmt.executeQuery();
				//System.out.println(pstmt);
				if(rs!=null && rs.next()){
					System.out.println(rs.getString("const") + " : : " + rs.getString("name"));
					temp += "\n"+rs.getString("const") + " : : " + rs.getString("name");
				}
			}
			
			// Now mutants getting from file
			input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/queries_mutant.txt"));
			int i=1;
			String mutant_query="";
			while (( mutant_query = input.readLine()) != null){
				queryString=queryString.replaceAll("year1", "year");
//				System.out.println("select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists (("+ queryString +") except ("+ mutant_query +")) or exists (("+ mutant_query +") except (" + queryString + "))");
				pstmt = conn.prepareStatement("select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists (("+ queryString +") except ("+ mutant_query +")) or exists (("+ mutant_query +") except (" + queryString + "))");
				//System.out.println("pstmt === >  "+"select 'Q" + i + " was killed by ' as const,dataset.name from dataset where exists (("+ queryString +") except ("+ mutant_query +")) or exists (("+ mutant_query +") except (" + queryString + "))");
				rs = pstmt.executeQuery();
				if(rs!=null && rs.next()){
//					System.out.println(rs.getString("const") + " : : " + rs.getString("name"));
					temp += "\n"+rs.getString("const") + " : : " + rs.getString("name");
				}
				i++;
			}
			
			if(conn != null)
				conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(input!=null)
				input.close();
		}
		
		return temp;
	}*/
	
	/**
	 * Executes CVC3 Constraints specified in the file "cvcOutputFileName"
	 * Stores the data set values inside the directory "datasetName"
	 * @param cvcOutputFileName
	 * @param query
	 * @param datasetName
	 * @param queryString
	 * @param filePath
	 * @param noOfOutputTuples
	 * @param tableMap
	 * @param columns
	 * @return
	 */
	public String killedMutants(String cvcOutputFileName, Query query, String datasetName, String queryString, String filePath, 
			HashMap<String, Integer> noOfOutputTuples, TableMap tableMap,Vector<Column> columns){
		String temp="";
		try{
//			System.out.println("\nCVC File: "+cvcOutputFileName);
			String test = generateCvcOutput(cvcOutputFileName, filePath);
			String cutFile = cutRequiredOutput(test, filePath);
			Vector<String> listOfCopyFiles = generateCopyFile(cutFile, filePath, noOfOutputTuples, tableMap,columns);			
			Vector<String> listOfFiles = (Vector<String>) listOfCopyFiles.clone();
			
			Runtime r = Runtime.getRuntime();
			//Process proc = r.exec("sh "+Configuration.scriptsDir+"/dir.sh "+datasetName+" "+filePath );			
			Process proc = r.exec("mkdir "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName );
			Utilities.closeProcessStreams(proc);
			
			for(String i:listOfCopyFiles){				
				proc = r.exec("cp "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+i +" "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName+"/");
				Utilities.closeProcessStreams(proc);
			}	
			Connection conn = MyConnection.getTestDatabaseConnection();
			populateTestDatabase(listOfCopyFiles, filePath, tableMap,conn);
					
			for(String i : listOfFiles){				
				proc = r.exec("cp "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+i +" "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName+"/");
				Utilities.closeProcessStreams(proc);
			}
			
			Utilities.closeProcessStreams(proc);
	    				
			//comment the two lines below if you do not want to measure how many mutants have been killed
			/*Vector<Mutant> mutants = generateJoinMutants(query);
			temp = mutantsKilledByDataset(mutants,datasetName, queryString, filePath);*/
		
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return temp;
			
	}
	
	public static void entry(String args[]) throws Exception{
		/*
		PopulateTestData ptd = new PopulateTestData();
		String test = ptd.generateCvcOutput("cvc3_temp2.cvc");
		String cutFile = ptd.cutRequiredOutput(test);
		Vector<String> listOfCopyFiles = ptd.generateCopyFile(cutFile);
		ptd.populateTestDatabase(listOfCopyFiles);
		
		try{
			PreparedStatement pstmt = null;
			TableMap tableMap = TableMap.getInstances();
			Connection conn = (new MyConnection()).getExhistingDatabaseConnection();
			QueryParser qParser = new QueryParser(tableMap);
			qParser.parseQuery("q1", "select * from instructor inner join teaches using(instructor_id) inner join crse using(course_id)");
			Query query = qParser.getQuery();
			
			Vector<Mutant> mutants = ptd.generateJoinMutants(query);
			
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		ptd.killedMutants("cvc3_"+ count +".cvc", this.query, "DS"+count, queryString, filePath);
		*/
		/*String copyFile = "a.copy";
		System.out.println(copyFile.substring(0,copyFile.indexOf(".copy")));
		BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc4/"+copyFile));
		String str;
		String data="";
		while((str = br.readLine())!=null){
			System.out.println(str);
			data+=str+"@@";
		}//Process proc = r.exec("sh "+Configuration.scriptsDir+"/upload.sh " + copyFile.substring(0,copyFile.indexOf(".copy")) + " " + copyFile + " " + filePath+" "+Configuration.databaseIP+" "+Configuration.databaseName+" "+Configuration.testDatabaseUser+" "+Configuration.testDatabaseUserPasswd);
		//int errVal = proc.waitFor();
		new PopulateTestData().uploadToTestUser(copyFile.substring(0,copyFile.indexOf(".copy")) ,data,"4");
		*/
		String datasetid=args[0];
		String questionid=args[1];
		System.out.println("------PopulateTestData------");
		System.out.println("Datasetid :"+datasetid);
		System.out.println("QuestionId :"+questionid);
		TableMap tm = (new GenerateCVC1()).getTableMap().getInstances();
		PopulateTestData p=new PopulateTestData();
		p.fetechAndPopulateTestDatabase(questionid, datasetid, tm);
		
	}
	
	public static void main(String args[]) throws Exception{
		/*
		PopulateTestData ptd = new PopulateTestData();
		String test = ptd.generateCvcOutput("cvc3_temp2.cvc");
		String cutFile = ptd.cutRequiredOutput(test);
		Vector<String> listOfCopyFiles = ptd.generateCopyFile(cutFile);
		ptd.populateTestDatabase(listOfCopyFiles);
		
		try{
			PreparedStatement pstmt = null;
			TableMap tableMap = TableMap.getInstances();
			Connection conn = (new MyConnection()).getExhistingDatabaseConnection();
			QueryParser qParser = new QueryParser(tableMap);
			qParser.parseQuery("q1", "select * from instructor inner join teaches using(instructor_id) inner join crse using(course_id)");
			Query query = qParser.getQuery();
			
			Vector<Mutant> mutants = ptd.generateJoinMutants(query);
			
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		ptd.killedMutants("cvc3_"+ count +".cvc", this.query, "DS"+count, queryString, filePath);
		*/
		/*String copyFile = "a.copy";
		System.out.println(copyFile.substring(0,copyFile.indexOf(".copy")));
		BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc4/"+copyFile));
		String str;
		String data="";
		while((str = br.readLine())!=null){
			System.out.println(str);
			data+=str+"@@";
		}//Process proc = r.exec("sh "+Configuration.scriptsDir+"/upload.sh " + copyFile.substring(0,copyFile.indexOf(".copy")) + " " + copyFile + " " + filePath+" "+Configuration.databaseIP+" "+Configuration.databaseName+" "+Configuration.testDatabaseUser+" "+Configuration.testDatabaseUserPasswd);
		//int errVal = proc.waitFor();
		new PopulateTestData().uploadToTestUser(copyFile.substring(0,copyFile.indexOf(".copy")) ,data,"4");
		*/
		String datasetid=args[0];
		String questionid=args[1];
		System.out.println("------PopulateTestData------");
		System.out.println("Datasetid :"+datasetid);
		System.out.println("QuestionId :"+questionid);
		TableMap tm = (new GenerateCVC1()).getTableMap().getInstances();
		PopulateTestData p=new PopulateTestData();
		p.fetechAndPopulateTestDatabase(questionid, datasetid, tm);
		
	}

}