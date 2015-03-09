package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * This class generates constraints to kill the extra group by mutations
 * @author mahesh
 *
 */
public class GenerateConstraintsToKillExtraGroupByMutations {

	/** Generates constraints to kill extra group by mutations in this query block */
	public static String getExtraGroupByConstraints(GenerateCVC1 cvc,	QueryBlockDetails queryBlock, ArrayList<Column> extraColumn, Map<String, String> tableOccurrence) throws Exception{

		/** Used to store the constraint*/
		String extraGroupBy = "";

		/**If there are no group  by attributes, then nothing need to be done*/
		if(queryBlock.getGroupByNodes() == null || queryBlock.getGroupByNodes().size() == 0)
			return extraGroupBy;



		/**FIXME: Will these extra columns affect tuple assignment method 
		 * If yes, then this method should be called before tuple assignment method in mutation killing methods OR
		 * Call tuple assignment method from here and then get constraints for all other conditions*/

		/**Generate constraint for each extra column */
		for(Column col: extraColumn){

			/**Get column and table details */
			String t1 = col.getTableName();
			String tableNameNo = tableOccurrence.get(t1);
			int Index = cvc.getTableMap().getTable(t1).getColumnIndex(col.getColumnName());
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];

			/**Generate constraints in each group of this query block*/
			for(int m=1; m <= queryBlock.getNoOfGroups() ; m++){

				extraGroupBy += "ASSERT ";

				/**get no of tuples */
				int count = cvc.getNoOfTuples().get(tableNameNo);

				/** get group number*/
				int group = (m-1)*count;

				/**If there is a single tuple */
				if(count == 1){
					extraGroupBy +=" TRUE ;\n";
					continue;
				}

				/**To kill this mutation
				 * This column has to be distinct in at least two tuples*/
				for(int k=1; k<=count;k++){
					for(int l=k+1; l<=count;l++)
						extraGroupBy += " DISTINCT (O_"+t1+"["+(group+k-1+offset)+"]."+Index+ " , O_"+t1+"["+(group+l-1+offset)+"]."+Index+") OR ";
				}
				int lastIndex = extraGroupBy.lastIndexOf("OR");
				extraGroupBy = extraGroupBy.substring(0, lastIndex-1) + " ;\n ";
			}
		}

		return extraGroupBy;
	}


	
	
	public static ArrayList<Column> getExtraColumns(QueryBlockDetails queryBlock, Map<String, String> tableOccurrence) throws Exception{
		/** get the list of tables which contain group by nodes of this query block */
		/** Along with the base table names, get their occurrences */
		/** Store base table names */
		ArrayList<Table> tempFromTables = new ArrayList<Table>();

		/**Stores relations occurrences*/
		tempFromTables = getListOfRelations( queryBlock.getGroupByNodes(), tableOccurrence );


		/** get all the extra columns of these tables*/
		ArrayList<Column> extraColumn = new ArrayList<Column>();
		extraColumn = getListOfExtraColumns(tempFromTables, queryBlock.getGroupByNodes());

		return extraColumn;
	}

	/**
	 * This function is used to get the list of extra columns apart from the group by attributes
	 * @param tempFromTables
	 * @param groupbyNodes
	 * @return
	 */
	public static ArrayList<Column> getListOfExtraColumns(	ArrayList<Table> tempFromTables, ArrayList<Node> groupbyNodes) throws Exception {

		/** Store the list of columns*/
		ArrayList<Column> extraColumn = new ArrayList<Column>();

		/** For each table */
		for(Table table: tempFromTables){
			for(int j=0;j<table.getNoOfColumn();j++){/**For each column of this table*/

				/**Get this column */
				Column col = table.getColumn(j);

				/**Indicates if this is a group by node */
				boolean flag=true;

				/** check if this column is a group by node */
				for(Node each: groupbyNodes)
					if(each.getColumn().getColumnName().equalsIgnoreCase(col.getColumnName())){
						flag=false;
						break;
					}

				if(flag)/** If this not a group by node column */
					extraColumn.add(col);
			}
		}
		return extraColumn;
	}

	/**
	 * This function returns the list of relations of group by nodes
	 * @param groupbyNodes
	 * @param tableOccurrence 
	 * @return
	 */
	public static ArrayList<Table> getListOfRelations(ArrayList<Node> groupbyNodes, Map<String, String> tableOccurrence) throws Exception{

		ArrayList<Table> tempFromTables = new ArrayList<Table>();

		/**Get for each group by node */
		for(Node tempgroupByNodeNew : groupbyNodes){
			/**If this table is not already in the list */
			if(!tempFromTables.contains(tempgroupByNodeNew.getColumn().getTable()))				
				/** Add this table */
				tempFromTables.add(tempgroupByNodeNew.getColumn().getTable());

			/**If this table occurrence is not already in the list */
			if( !tableOccurrence.containsValue(tempgroupByNodeNew.getType()))
				/**Add the occurrence of this relation*/
				tableOccurrence.put(tempgroupByNodeNew.getColumn().getTable().getTableName(), tempgroupByNodeNew.getTableNameNo());
		}

		return tempFromTables;
	}


}
