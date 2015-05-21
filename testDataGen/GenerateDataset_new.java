package testDataGen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import util.Configuration;
import util.MyConnection;



/**
 * The main class that is called to get data sets for the query
 * @author mahesh
 *
 */
public class GenerateDataset_new {

	String filePath;

	public GenerateDataset_new(String filePath) throws Exception{		
		this.filePath = filePath;
	}


	public void generateDatasetForQuery(String queryId, String orderindependent, String query, String queryDesc) throws Exception{


		System.out.println("------------------------------------------------------------------------------------------\n\n");
		System.out.println("QueryID:  "+queryId);
		System.out.println("GENERATING DATASET FOR QUERY: \n" + query);
		System.out.println("------------------------------------------------------------------------------------------\n\n");
		
		
		/** delete previous data sets*/		
		//RelatedToPreprocessing.deletePreviousDatasets(this, query);
		
		/**Create object for generating data sets */
		GenerateCVC1 cvc = new GenerateCVC1();
		
		cvc.setFne(false);
		cvc.setIpdb(false);
		cvc.setFilePath(this.getFilePath());
		
		/**Call pre processing functions before calling data generation methods */
		PreProcessingActivity.preProcessingActivity(cvc);
		
		/** Check the data sets generated for this query */
		ArrayList<String> dataSets = RelatedToPreprocessing.getListOfDataset(this);
		
		System.out.println("\n\n***********************************************************************\n");
		System.out.println("DATA SETS FOR QUERY "+queryId+" ARE GENERATED");
		System.out.println("\n\n***********************************************************************\n");

		/**Update query info table */
		WriteFileAndUploadDatasets.updateQueryInfo(this, queryId, query, queryDesc);
		
		
		/**Upload the data sets into the database */
		WriteFileAndUploadDatasets.uploadDataset(this, queryId, dataSets);			

		System.out.println("\n***********************************************************************\n\n");
		System.out.println("DATASET FOR QUERY "+queryId+" ARE UPLOADED");
		System.out.println("\n***********************************************************************\n\n");
	}

	public void generateDatasetForBranchQuery(String queryId, String[] branchQuery,String[] branchResult, String query, String queryDesc) throws Exception
	{
		System.out.println("------------------------------------------------------------------------------------------\n\n");
		System.out.println("QueryID "+queryId);
		System.out.println("GENERATING DATASET FOR QUERY: \n" + query);
		
		
			
		/** delete previous data sets*/		
		RelatedToPreprocessing.deletePreviousDatasets(this, query);
	
		/**upload the details about the list of queries and the conditions between the branch queries*/
		BufferedWriter ord2 = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/branchQuery.txt"));
		for(String str : branchQuery)
		{
			ord2.write(str);
			ord2.newLine();
		}
		ord2.close();
		
		BufferedWriter ord3 = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/branchResult.txt"));
		for(String str : branchResult)
		{
			ord3.write(str);
			ord3.newLine();
		}
		ord3.close();
			
		/**Create object for generating data sets */
		GenerateCVC1 cvc = new GenerateCVC1();
		
		cvc.setFne(false);
		cvc.setIpdb(false);
		cvc.setFilePath(this.getFilePath());
		
		/**Call pre processing functions before calling data generation methods */
		PreProcessingActivity.preProcessingActivity(cvc);
		
		
		/** Check the data sets generated for this query */
		ArrayList<String> dataSets = RelatedToPreprocessing.getListOfDataset(this);
		
		System.out.println("\n\n***********************************************************************\n");
		System.out.println("DATA SETS FOR QUERY "+queryId+" ARE GENERATED");
		System.out.println("\n\n***********************************************************************\n");

		/**Update query info table */
		WriteFileAndUploadDatasets.updateQueryInfo(this, queryId, query, queryDesc);
		
		
		/**Upload the data sets into the database */
		WriteFileAndUploadDatasets.uploadDataset(this, queryId, dataSets);			

		System.out.println("\n***********************************************************************\n\n");
		System.out.println("DATASET FOR QUERY "+queryId+" ARE UPLOADED");
		System.out.println("\n***********************************************************************\n\n");
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public static void entry(String[] args) throws Exception{
		String filePath="4";
		GenerateDataset_new g=new GenerateDataset_new(filePath);
		
        try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        String assignment_id = args[0];
		String question_id = args[1];     
        TestAnswer test=new TestAnswer();
	    Connection dbcon=null;	
	    dbcon = MyConnection.getExistingDatabaseConnection();
    	if(dbcon!=null){
    	    	  System.out.println("Connected successfullly");
    	    	  
    	 }
		String sel="Select correctquery from qinfo where assignmentid=? and questionid=?";
		PreparedStatement stmt=dbcon.prepareStatement(sel);
		stmt.setString(1,assignment_id);
		stmt.setString(2, question_id);
		ResultSet rs=stmt.executeQuery();
		rs.next();
		String sql=rs.getString("correctquery");
		g.generateDatasetForQuery("A"+args[0]+"Q"+args[1], "true", sql, "");
		rs.close();
		dbcon.close();
	}
	

	public static void main(String[] args) throws Exception{
		String filePath="4";
		GenerateDataset_new g=new GenerateDataset_new(filePath);
	/*	String loginUser = Configuration.existingDatabaseUser; //change user name according to your db user
        String loginPasswd = Configuration.existingDatabaseUserPasswd; //change user passwd according to your db user passwd
        String hostname = Configuration.databaseIP;
        String dbName = Configuration.databaseName;
        try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        String loginUrl = "jdbc:postgresql://" + hostname +  "/" + dbName;
        String assignment_id = args[0];
		String question_id = args[1];     
        TestAnswer test=new TestAnswer();
	    Connection dbcon=null;	
	    dbcon = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
    	if(dbcon!=null){
    	    	  System.out.println("Connected successfullly");
    	    	  
    	 }
		String sel="Select correctquery from qinfo where assignmentid=? and questionid=?";
		PreparedStatement stmt=dbcon.prepareStatement(sel);
		stmt.setString(1,assignment_id);
		stmt.setString(2, question_id);
		ResultSet rs=stmt.executeQuery();
		rs.next();
		String sql=rs.getString("correctquery");
		g.generateDatasetForQuery("A"+args[0]+"Q"+args[1], "true", sql, "");*/

	//	g.generateDatasetForQuery("Q1", "true", "select credits from course where credits in ( select sum(credits) from course where dept_name='Biology'  group by dept_name having sum(credits)>3 and count(dept_name)>3)", "");

		//g.generateDatasetForQuery("SQ1", "true", "select name from student where ( tot_cred > 5 AND (dept_name > 'D' OR dept_name='Comp. Sci.')) OR (tot_cred=6 AND dept_name <'p' AND name > 'C')", "");
		//g.generateDatasetForQuery("HQ22", "true", "SELECT COUNT(course_id) FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING COUNT(time_slot_id)>1) as s NATURAL JOIN course WHERE credits = (SELECT MAX(credits)  FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY dept_name, building HAVING COUNT(course_id)> 2) GROUP BY id,dept_name HAVING COUNT(title)>1", "");
	//	g.generateDatasetForQuery("SDF43", "true", "SELECT id FROM (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING COUNT(time_slot_id)>1) as s", "");
	//	g.generateDatasetForQuery("Qzz21", "true", "SELECT course_id, title FROM course WHERE dept_name= 'Comp. Sci.'", "");
	//	g.generateDatasetForQuery("Q2", "true", "select id from takes where semester = 'Spring' or year>2010", "");
		
//		g.generateDatasetForQuery("Q3", "true", "SELECT id,name FROM (select id,time_slot_id,year,semester from takes natural join section group by id,time_slot_id,year,semester having count(time_slot_id)>1) as s natural join student group by id, name having count(id)>1", "");
		
	//	g.generateDatasetForQuery("13", "true", "SELECT id, name FROM student NATURAL LEFT OUTER JOIN (SELECT id, name, course_id FROM student NATURAL LEFT OUTER JOIN takes WHERE year = 2010 and semester = 'Spring') S WHERE course_id IS NULL", "");
		
	// g.generateDatasetForQuery("1", "true", "SELECT course_id, title FROM course", "");
		
	//	g.generateDatasetForQuery("3", "true", "SELECT DISTINCT course.course_id, course.title, ID from course natural join teaches WHERE teaches.semester='Spring' AND teaches.year='2010'", "");
		
	//	g.generateDatasetForQuery("4", "true", "SELECT DISTINCT student.id, student.name FROM takes natural join student WHERE course_id ='CS-101'", "");
		
	//	g.generateDatasetForQuery("5", "true", "SELECT DISTINCT course.dept_name FROM course NATURAL JOIN section WHERE section.semester='Spring' AND section.year='2010'", "");
		
	//	g.generateDatasetForQuery("6", "true", "SELECT course_id, title FROM course WHERE credits>3", "");
		
	//	g.generateDatasetForQuery("7", "true", "select course_id, count(distinct id) from course natural left outer join takes group by course_id", "");
	
	//    g.generateDatasetForQuery("8", "true", "SELECT DISTINCT course_id, title FROM course NATURAL JOIN section WHERE semester = 'Spring' AND year = 2010 AND course_id NOT IN (SELECT course_id FROM prereq)", "");
		
	//	g.generateDatasetForQuery("9", "true", "select distinct A.id, A.name from (SELECT  * from student natural join takes natural join section) A, ( SELECT  * from section natural join takes natural join student) B where A.id = B.id and A.time_slot_id = B.time_slot_id and A.semester= B.semester and A.year = B.year and A.course_id <> B.course_id", "");
		
	  // g.generateDatasetForQuery("9", "true", "WITH s as (SELECT id, time_slot_id, year, semester FROM takes NATURAL JOIN section GROUP BY id, time_slot_id, year, semester HAVING COUNT(time_slot_id) > 1) SELECT DISTINCT id, name FROM s NATURAL JOIN student where s.id = '23'", "");
		
		//g.generateDatasetForQuery("13", "true", "SELECT id, name FROM student natural left outer join (SELECT id, name, course_id FROM student natural left outer join takes WHERE year = 2010 and semester = 'Spring') a ", "");
		
	//	g.generateDatasetForQuery("14", "true", "SELECT DISTINCT * FROM takes t WHERE (NOT EXISTS (SELECT id,course_id FROM takes s WHERE s.grade != 'F' AND t.id = s.id AND	   t.course_id = s.course_id) AND t.grade IS NOT NULL) OR (grade != 'F' AND     grade IS NOT NULL)", "");
		
	//	g.generateDatasetForQuery("Q91", "true", "SELECT distinct dept_name FROM course WHERE credits =(SELECT MAX(credits) FROM course WHERE  title='CS' group by dept_name HAVING count(course_id)> 2)", "");
		
		//g.generateDatasetForQuery("10", "true", "SELECT DISTINCT dept_name FROM course WHERE credits = (SELECT MAX(credits) FROM course)", "");
		
	
		//g.generateDatasetForQuery("11", "true", "SELECT DISTINCT instructor.id,name,course_id FROM instructor LEFT OUTER JOIN teaches ON instructor.id = teaches.id", "");
		//g.generateDatasetForQuery("12", "true", "SELECT student.id, student.name FROM student WHERE lower(student.name) like '%sr%'", "");
		
		// g.generateDatasetForQuery("7", "true", "select building from course natural join section natural join takes group by building having count(building)>1", "");
	
		//		g.generateDatasetForQuery("7", "true", "select * from student where dept_name = \"CSE\"", "");
		
		g.generateDatasetForQuery("8", "true", "select name from student where name like 'B%' and name like '%ro%' and dept_name = 'Comp. Sci.'", "");
		
		//		g.generateDatasetForQuery("7", "true", "select student.id,sum(credits) from student, takes, course where takes.course_id = course.course_id and takes.year=2010 group by student.id having sum(credits) >= 30", "");

		//g.generateDatasetForQuery("7", "true", "select distinct teaches.course_id from (select * from teaches where semester = 'Fall' and year = '2009') A right outer join teaches on teaches.id = a.id where a.id is null and teaches.semester = 'Spring' and teaches.year = '2010'", "");
		
		//g.generateDatasetForQuery("10", "true", "select A.course_id from (select distinct course.course_id from section inner join course ON section.course_id =course.course_id where building = 'Taylor' and dept_name = 'Comp. Sci.') A,takes,student where A.course_id = takes.course_id and student.id = takes.id and student.dept_name = 'Physics'", "");
		
	//g.generateDatasetForQuery("8", "true", "with s as (select * from teaches where semester = 'Fall' and year = '2009') select DISTINCT course_id from teaches where semester ='Spring' and year = '2010' and id NOT IN (select id from s)", "");
		
		//g.generateDatasetForQuery("7", "true", "select * from takes t where t.semester = 'Spring' and t.year = '2010' and  exists (select takes.course_id from takes inner join course ON takes.course_id = course.course_id where course.dept_name = 'Comp. Sci.' and takes.semester = 'Spring' and takes.year = '2010' and t.id = takes.id)", "");
	
		

	//g.generateDatasetForQuery("7", "true", "select * from takes t where t.semester = 'Spring' and t.year = '2010' and  exists (select takes.course_id from takes inner join course ON takes.course_id = course.course_id where course.dept_name = 'Comp. Sci.' and takes.semester = 'Spring' and takes.year = '2010' and t.id = takes.id group by takes.course_id having count(id) < 2)", "");
	  //g.generateDatasetForQuery("8", "true", "delete   from takes t where t.semester = 'Spring' and t.year = '2010' and  exists (select takes.course_id from takes inner join course ON takes.course_id = course.course_id where course.dept_name = 'Comp. Sci.' and takes.semester = 'Spring' and takes.year = '2010' and t.id = takes.id group by takes.course_id having count(id) < 2)", "");
		//g.generateDatasetForQuery("8", "true", "select DISTINCT course_id from teaches where semester ='Spring' and year = '2010' and id NOT IN (select id from teaches where semester = 'Fall' and year = '2009')", "");
		//g.generateDatasetForQuery("7", "true", "SELECT department.dept_name, I.name FROM department LEFT OUTER JOIN (SELECT * from instructor WHERE instructor.salary > 70000) I ON I.dept_name = department.dept_name", "");		
		
	//g.generateDatasetForQuery("2", "true", "create view instructLoad as (select instructor.name, count(takes.id) from instructor, takes, teaches where instructor.id = teaches.id and teaches.course_id = takes.course_id and teaches.sec_id = takes.sec_id and takes.semester = 'Spring' and takes.year = '2010' and teaches.semester = 'Spring' and teaches.year = '2010' group by instructor.id)", "");
	  //g.generateDatasetForQuery("1", "true", "select distinct student.id,student.name from student, takes, course where student.id = takes.id and takes.course_id = course.course_id and student.dept_name = 'CSE' and takes.semester='Spring'", "");

	}

}
