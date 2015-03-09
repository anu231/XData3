package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillExtraGroupByMutations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import java.util.Arrays;

/**
 * This class generates data set to kill extra group by attributes mutation
 * @author mahesh
 *
 */
public class ExtraGroupByMutationsInOuterQueryBlock {

	/**
	 * Generate data set to kill mutations with extra  group by attributes in outer block of the query
	 * 
	 * Approach:
	 * Get all the attributes of the tables related to group by nodes
	 * Get all the constraints for the query
	 * For the extra attributes which are not present in the group by list, make them differ in value in at least two tuples
	 * Assumption: There are at least two tuples to satisfy constrained aggregation in the outer block of query
	 * 
	 * @param cvc
	 */
	public static void generateDataForkillingExtraGroupByMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception {

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/**Get outer query block */
		QueryBlockDetails outer = cvc.getOuterBlock();

		System.out.println("\n----------------------------------");
		System.out.println("GENERATE DATA FOR KILLING EXTRA GROUP BY MUTATION IN OUTER BLOCK OF QUERY: " + outer);
		System.out.println("----------------------------------\n");



		/** If there are no group by attributes, then no need to kill this mutation */
		if(outer.getGroupByNodes() == null || outer.getGroupByNodes().size() == 0)
			return ;


		/** Initialize the data structures for generating the data to kill this mutation */
		cvc.inititalizeForDataset();

		/**set the type of mutation we are trying to kill*/
		cvc.setTypeOfMutation( TagDatasets.MutationType.EXTRAGROUPBY, TagDatasets.QueryBlock.OUTER_BLOCK );

		
		/**get extra columns in all the relations*/
		Map<String, String> tableOccurrence = new HashMap<String, String>();
		ArrayList<Column> extraColumn = GenerateConstraintsToKillExtraGroupByMutations.getExtraColumns(outer, tableOccurrence);
		
		/**Extra attributes must be distinct in at least two values. So we are making them unique*/
		for(Column col: extraColumn){
			
			Node n = Node.createNode( col, col.getTable() );
			n.setTableNameNo(tableOccurrence.get(col.getTableName()));
			
			outer.getUniqueElementsAdd().add( new HashSet<Node>( Arrays.asList(n) ));
		}
			
			
		if( CountEstimationRelated.getCountAndTupleAssignmentToKillExtraGroupByMutations(cvc, outer) == false)
			return;

		/**Get the constraints for all the blocks of the query */
		cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );

		/**Get the constraints to kill this mutation*/
		cvc.getConstraints().add("\n%---------------------------------\n%CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES\n%---------------------------------\n");
		cvc.getConstraints().add(GenerateConstraintsToKillExtraGroupByMutations.getExtraGroupByConstraints(cvc, outer, extraColumn,tableOccurrence));
		cvc.getConstraints().add("\n%---------------------------------\n%END OF CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES\n%---------------------------------\n");

		/** Call the method for the data generation*/
		GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);


		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );

	}



}
