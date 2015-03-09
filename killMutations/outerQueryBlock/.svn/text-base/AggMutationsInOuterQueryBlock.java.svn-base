package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillAggregationMutations;

import java.util.ArrayList;
import java.util.HashMap;

import killMutations.KillCountMutations;
import killMutations.Utils;
import parsing.AggregateFunction;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill aggregation function mutations in the outer query block
 * @author mahesh
 *
 */
public class AggMutationsInOuterQueryBlock {


	/**
	 * Generates data to kill aggregation function mutations inside each outer query block
	 * @param cvc
	 */
	/**FIXME: COUNT() to COUNT(*) is not added yet*/

	public static void generateDataForkillingAggMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{


		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		System.out.println("\n----------------------------------");
		System.out.println("GENERATE DATA FOR KILLING AGGREGATION MUTATIONS IN OUTER QUERY BLOCK");
		System.out.println("----------------------------------\n");

		/** TO indicate if there is COUNT() in aggregate function list*/
		boolean killCountMutants = false;
		
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();

		int attempt = 0;

		/** Get the aggregate function list of this subquery block*/
		ArrayList<AggregateFunction> aggFunc = qbt.getAggFunc();

		/**Kill each aggregate function mutation*/
		for(int i=0; i< aggFunc.size(); i++){

			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDataset();

			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.AGG, TagDatasets.QueryBlock.OUTER_BLOCK );
			
			/**Get aggregate function to kill in this iteration*/
			AggregateFunction af = aggFunc.get(i);

			if(af.getAggExp()==null) { //projected attribute is not an aggregate
				continue;
			}
			
			System.out.println("\n----------------------------------");
			System.out.println("\nKILLING IN OUTER BLOCK OF QUERY: " + af.toString()+"   ATTEMPT NUMBER: "+(attempt+1));
			System.out.println("----------------------------------\n");

			qbt.setConstrainedAggregation(true);
			
			/**get the count needed*/
			if(CountEstimationRelated.getCountNeededToKillAggregationMutation(cvc, qbt, attempt) == false)
				continue;

			
			/**assign the number of tuples for the this outer query block*/
			if( QueryBlockDetails.getTupleAssignment( cvc, qbt, null) == false)
				continue ;

			/**get the tuple assignment for all other query blocks*/
			if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, qbt) == false)
				continue;

			
			/** Check if this aggregate function uses any column that is inside from clause sub query block*/
			/**TODO: If aliased column is itself aggregate function, we are generating aggregation constraints on the column used in aliased aggregate function
			 * But the actual constraints are to be generated on the aliased aggregate function
			 * Example: SUM( AVG(COLUMN) ) then we should generate constraints on AVG(COLUMN). But now we are doing for COLUMN*/
			int noofGroups = 1;
			AggregateFunction newAf = new AggregateFunction();
			if(af.getAggExp().getQueryType() == 1){
				noofGroups = cvc.getOuterBlock().getFromClauseSubQueries().get( af.getAggExp().getQueryIndex() ).getNoOfGroups();
				newAf = af.getAggExp().getAgg();
			}
			else
				newAf = af;

			/** Add constraints for all the blocks of the query */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );

			cvc.getConstraints().add("\n%---------------------------------\n% AGGREGATION CONSTRAINTS FOR FROM CLAUSE SUBQUERY\n%---------------------------------\n");
			cvc.getConstraints().add( (GenerateConstraintsToKillAggregationMutations.getAggConstraints(cvc, newAf, qbt.getNoOfGroups())) );
			cvc.getConstraints().add("\n%---------------------------------\n% END OF AGGREGATION CONSTRAINTS FOR FROM CLAUSE SUBQUERY\n%---------------------------------\n");

			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);

			/**Reset the variable*/
			qbt.setConstrainedAggregation(false);
			
			/**check whether the data generation is succeeded or not
			 * if not then give another attempt by changing number of tuples
			 */
			int [] list = Utils.checkIfSucces(cvc, attempt, i);			
			attempt = list[0];
			i = list[1];
			
			/**Will generate data sets later to kill COUNT <-> COUNT(*) */
			if(af.getFunc().toUpperCase().contains("COUNT"))			
				killCountMutants = true;				
			
		}

		
		if(killCountMutants)
			KillCountMutations.killCountMutations(cvc, qbt);
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );

	}

}
