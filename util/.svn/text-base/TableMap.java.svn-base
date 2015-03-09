/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.derby.impl.sql.compile.TableName;

import parsing.*;


/**
 *
 * @author Bhanu Pratap Gupta
 */


public class TableMap {
    
    private static TableMap instance = null;
    private Map<String,Table> tables = null;
    public Map<String, Table> getTables() {
		return tables;
	}

	public void setTables(Map<String, Table> tables) {
		this.tables = tables;
	}

	private Vector<Table> topSortedTables = null;    
    private String database;
    private String schema;
   
    private Connection conn = null;
    public Graph<Table,ForeignKey> foreignKeyGraph = null;
    
    public static TableMap getInstances(){
        
        if(instance==null){
        	instance = new TableMap();            
        	instance.createTableMap();
        }
        return instance;
    }
    
    private TableMap(){
        tables = new LinkedHashMap<String,Table>();
        database = Configuration.databaseName;
        schema = Configuration.existingDatabaseUser;
        try{
            conn = MyConnection.getExistingDatabaseConnection();
            
        }catch(Exception e){}
    }
    
    public void createTableMap() {        
        try {
            DatabaseMetaData meta = conn.getMetaData();
            
            ResultSet rs = null, rs1=null;
            String tableFilter[] = {"TABLE"};                        
            rs = meta.getTables(database, schema, "%", tableFilter);
            while (rs.next()) {                          
               String tableName = rs.getString("TABLE_NAME").toUpperCase(); 
               if(tables.get(tableName)==null){
            	   Table table = new Table(tableName);
                   tables.put(tableName, table );
                   //tableVector.add(table);
               }               
            }
            rs = null;    
            rs = meta.getColumns(database, schema, "%","%");           
            while (rs.next()) {
               String tableName = rs.getString("TABLE_NAME").toUpperCase();
               Table table = getTable(tableName);
               if(table==null)
                   continue;
               String columnName = rs.getString("COLUMN_NAME").toUpperCase();

               Column col = new Column(columnName,table);
               
               col.setDataType(rs.getInt("DATA_TYPE"));
               if(col.getDataType()==Types.NUMERIC){
            	   if(rs.getInt("DECIMAL_DIGITS")==0)
            		   col.setDataType(Types.INTEGER);
               }
               if(col.getDataType()==Types.CHAR)
                   col.setDataType(Types.VARCHAR);
               col.setColumnSize(rs.getInt("COLUMN_SIZE"));                              
               col.setIsNullable(rs.getString("IS_NULLABLE").equals("YES"));
               if(rs.getString("COLUMN_DEF")!=null && rs.getString("COLUMN_DEF").startsWith("nextval")){
                   col.setIsAutoIncement(true);                   
               }
               table.addColumn(col);
            }
            
            // Primary key of a table
//            rs1 = meta.getPrimaryKeys(database, schema, null);
//            while(rs1.next()){
//            	String tableName = rs1.getString("TABLE_NAME").toUpperCase();
//            	//System.out.println("TableName = "+tableName);
//            	Table table = getTable(tableName);
//            	String columnName = rs1.getString("COLUMN_NAME").toUpperCase();
//            	//System.out.println("ColumnName = "+columnName);
//            	Column col = table.getColumn(columnName);
//            	//System.out.println(col==null?"is null":"not null");
//            	table.addColumnInPrimaryKey(col);
//            	col.setIsUnique(true);
//            	if(tableName.toUpperCase().equals("TAKES") && col.getColumnName().toUpperCase().equals("COURSE_ID"))
//            		System.out.println("Is takes unique " + col.isUnique());
//         	   System.out.println(rs1.getString("TABLE_cat")+" "+rs1.getString("TABLE_SCHEM")+" "+rs1.getString("table_name")+" "+rs1.getString("column_name")+" "+rs1.getString("key_seq")+" "+rs1.getString("PK_NAME"));
//            }
            //Changed by Biplab. Original code commented out above
            rs = null;
            rs = meta.getTables(database, schema, "%", tableFilter);
            while (rs.next()) {                           
              String tableName = rs.getString("TABLE_NAME"); 
              rs1 = null;
              rs1 = meta.getPrimaryKeys(database, schema, tableName);
           	  int size = rs1.getFetchSize();
           	  while(rs1.next()){
           		Table table = getTable(tableName.toUpperCase());
           		String columnName = rs1.getString("COLUMN_NAME").toUpperCase();
           		//System.out.println("ColumnName = "+columnName);
           		Column col = table.getColumn(columnName);
           		//System.out.println(col==null?"is null":"not null");
           		table.addColumnInPrimaryKey(col);
           		if(size == 1)
           			col.setIsUnique(true);
//           		System.out.println(rs1.getString("TABLE_cat")+" "+rs1.getString("TABLE_SCHEM")+" "+rs1.getString("table_name")+" "+rs1.getString("column_name")+" "+rs1.getString("key_seq")+" "+rs1.getString("PK_NAME"));
           	  }           
            }
            
            	
            
//            Table tablem = getTable("STUDREGN");
//            System.out.println(tablem==null?"is null":"not null");
//            System.out.println(tablem.getPrimaryKey());

            
            foreignKeyGraph = new Graph<Table,ForeignKey>(true);
            
            rs = meta.getExportedKeys(/*database*/conn.getCatalog(), schema, null);
            while(rs.next()){
            	
            	String fkName = rs.getString("FK_NAME");
            	String fkTableName = rs.getString("FKTABLE_NAME").toUpperCase();
                String fkColumnName = rs.getString("FKCOLUMN_NAME").toUpperCase();
//                System.out.println("fk_name = " + fkName + " fkTableName = " + fkTableName + " fkColumnName = " + fkColumnName);
                Table fkTable = getTable(fkTableName);
                Column fkColumn = fkTable.getColumn(fkColumnName);
                if(fkColumnName.equals(""))
                	continue;
                int seq_no = rs.getInt("KEY_SEQ");
                
                String pkTableName = rs.getString("PKTABLE_NAME").toUpperCase();
                String pkColumnName = rs.getString("PKCOLUMN_NAME").toUpperCase();
                
                Table pkTable = getTable(pkTableName);
                pkTable.setIsExportedTable(true);
                Column pkColumn = pkTable.getColumn(pkColumnName);
                pkColumn.setIsUnique(true);
                fkColumn.setReferenceTableName(pkTableName);
                fkColumn.setReferenceColumn(pkColumn);
                                
                ForeignKey fk = fkTable.getForeignKey(fkName);
                fk.addFKeyColumn(fkColumn, seq_no);
                fk.addReferenceKeyColumn(pkColumn, seq_no);
                fk.setReferenceTable(pkTable);
                fkTable.addForeignKey(fk);
                
            }
            
           for(String tableName : tables.keySet()){
            	Table table = tables.get(tableName);
            	if(table.hasForeignKey()){
            		for(String fKeyName : table.getForeignKeys().keySet()){
            			ForeignKey fKey = table.getForeignKeys().get(fKeyName);
            			foreignKeyGraph.add(fKey.getReferenceTable(), table, fKey);
            		}	
            	}
            }
            
            topSortedTables = foreignKeyGraph.topSort();   
//            System.out.println("Imported or Exported tables : "+topSortedTables);
//            System.out.println("Directed Foreign Key Graph : "+foreignKeyGraph); 
//            System.out.println("\nThe graph " + (foreignKeyGraph.isDag()?"is":"is not") + " a dag");
            
            for(String tableName : tables.keySet()){
            	Table table = tables.get(tableName);
            	if(!topSortedTables.contains(table))
            		topSortedTables.add(table);
            }
            
            
           //Commented out by Biplab. What is the fn of this code?
//           for(String tableName : tables.keySet()){
//                rs = meta.getIndexInfo(database, schema, tableName,true,true);
//                while(rs.next()){                                                             
//                   String columnName = rs.getString("COLUMN_NAME").toUpperCase();
//                   Table table = tables.get(tableName);
//                   if(table==null)
//                       continue;            
//                   Column col = table.getColumn(columnName);
//                   col.setIsUnique(true);
//                }
//            }           
           
        conn.close(); 
        }catch(Exception e){
        	e.printStackTrace();
        }
    }
    
    public Vector<Table> getAllTablesInTopSorted(){                
        return topSortedTables;        
    }
    
    public Table getTable(String tableName){                
        return (tables.get(tableName));
    }
    
    public Graph<Table,ForeignKey> getForeignKeyGraph(){
    	return foreignKeyGraph;
    }
    
    public static void main(String args[]){
        try{
        	TableMap tm = TableMap.getInstances();
            System.out.println(tm.getAllTablesInTopSorted());
            
            /*Table table = tm.getTable("ROLLHIST");
            for(String columnName : table.getColumns().keySet()){
            	Column col = table.getColumns().get(columnName);
            	System.out.println(col.getColumnName()+" is Nullable : "+col.isNullable());
            }*/
        }catch(Exception e){
            e.printStackTrace();;
        }
     
    }

}