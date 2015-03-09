package killMutations;

import generateConstraints.CountMutations;
import generateConstraints.GenerateCommonConstraintsForQuery;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import util.TagDatasets.MutationType;
import util.TagDatasets.QueryBlock;

public class KillCountMutations {

	/**
	 * Generates data sets to kill mutation from COUNT(A.x) to COUNT(*) in the given query block
	 * @param cvc
	 * @param qbt
	 * @param af
	 */
	/**FIXME: When COUNT() in outer query block uses attributes inside FROM CLause block and/or
	 * There may not be relations in outer block of query i.e. only from clause blocks are present
	 */
	public static void killCountMutations(GenerateCVC1 cvc,	QueryBlockDetails qbt) throws Exception {
		
		/** Initialize the data structures for generating the data to kill this mutation */
		cvc.inititalizeForDataset();
	
		/**set the type of mutation we are trying to kill*/
		cvc.setTypeOfMutation( TagDatasets.MutationType.COUNT, TagDatasets.QueryBlock.OUTER_BLOCK );
		
		
		/** get the tuple assignment for this query
		 * If no possible assignment then not possible to kill this mutation*/
		if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
			return ;
		
		System.out.println("\nKILLING COUNT MUTANTS IN OUTER BLOCK OF QUERY");
		
		/**get the constraints for count mutation*/
		String constString = CountMutations.constraintsForKillingCountMutants(cvc, qbt);
		
		/**if not possible to kill count() mutations*/
		if(constString == "")
			return ;
		
		/**Get the constraints for all the blocks of the query */
		cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );		
		
		/**add constraints related to count mutation*/
		cvc.getConstraints().add("\n%-----------------------------------------------------\n%CONSTRAINTS FOR KILLING COUNT MUTATIONS \n%--------------------------------------------------------------\n");
		cvc.getConstraints().add(constString);
		cvc.getConstraints().add("\n%-----------------------------------------------------\n%END OF CONSTRAINTS FOR KILLING COUNT MUTATIONS \n%--------------------------------------------------------------\n");
		/** Call the method for the data generation*/
		GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
	}

}
