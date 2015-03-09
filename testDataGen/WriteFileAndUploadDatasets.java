package testDataGen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import util.Configuration;
import util.MyConnection;

/**
 * Common methods
 * @author mahesh
 *
 */
public class WriteFileAndUploadDatasets {

	public static void writeFile(String filePath, String content){
		try{
			java.io.FileWriter fw=null;
			fw=new java.io.FileWriter(filePath, false);
			fw.write(content);
			fw.flush();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Update the query info table
	 * @param gd
	 * @param queryId
	 * @param query
	 * @param queryDesc
	 * @throws Exception
	 */
	public static void updateQueryInfo(GenerateDataset_new gd, String queryId, String query,String queryDesc) throws Exception{
		
		String insertquery="INSERT INTO queryinfo VALUES (?,?,?,?)";
		Connection conn = MyConnection.getExistingDatabaseConnection();
		PreparedStatement smt = conn.prepareStatement(insertquery);
		smt.setString(1, queryId);
		smt.setBoolean(2, true);
		smt.setString(3, query);
		smt.setString(4, queryDesc);
		smt.executeUpdate();
		smt.close();
		conn.close();
	}

	/**
	 * Upload the data sets into inout database
	 * @param gd
	 * @param queryId
	 * @param dataSets
	 */
	public static void uploadDataset(GenerateDataset_new gd, String queryId, ArrayList<String> dataSets) throws Exception{
		
		PreparedStatement smt=null;
		System.out.println("hello " + dataSets.size());
		String prevDatasets = "SELECT datasetid FROM datasetvalue WHERE queryid = '" + queryId + "'";
		Connection conn = MyConnection.getExistingDatabaseConnection(); 
		smt = conn.prepareStatement(prevDatasets);
		ResultSet rs =smt.executeQuery();
		String datasetid="";
		int maxid=0;
		while(rs.next()){
			datasetid=rs.getString(1);
			int id=Integer.parseInt(datasetid.substring(2));
			if(id > maxid)
				maxid=id;
		}
		for(int i=0;i<dataSets.size();i++){
			
			String dsPath = Configuration.homeDir+"/temp_cvc"+gd.getFilePath()+"/"+dataSets.get(i);
			String cvcPath = Configuration.homeDir+"/temp_cvc"+gd.getFilePath()+"/cvc3_"+(i)+".cvc";
			File ds=new File(dsPath);		 	
			String copyFiles[] = ds.list();
			System.out.println("dsPath=> " + dsPath);
			System.out.println("dsPath=> " + copyFiles.length);
			String datasetvalue="",st="";
			if(copyFiles.length==0){
				continue;
			}
			BufferedReader b = new BufferedReader(new FileReader(cvcPath));
			String line="";
			String tag="";
			String substr = "%MUTATION TYPE:";
			while ((line = b.readLine()) != null) {
				   if(line.startsWith(substr)){
					   tag=line.substring(line.lastIndexOf(substr) + substr.length()).trim();
					   break;
				   }
			}
			b.close();
			for(int j=0;j<copyFiles.length;j++){
			    datasetvalue+=copyFiles[j];
			    BufferedReader br=new BufferedReader(new FileReader(dsPath+"/"+copyFiles[j]));
			    while((st=br.readLine())!=null){
			    	datasetvalue+=st;
			    	datasetvalue+="::";
			    }
			    datasetvalue=datasetvalue.substring(0,datasetvalue.length()-2);
			    datasetvalue+=":::";
			}
			
			datasetvalue=datasetvalue.substring(0,datasetvalue.length()-3);
			String tt=dataSets.get(i).substring(2);
			datasetid="DS"+(Integer.parseInt(tt)+maxid);
			String insertquery="INSERT INTO datasetvalue VALUES ('"+queryId+"','"+datasetid+"','"+datasetvalue+"','"+tag+"')";
			System.out.println(insertquery);
			smt = conn.prepareStatement(insertquery);
			smt.executeUpdate();
					 	
		}
		
		smt.close();
		rs.close();
		conn.close();
	}
}
