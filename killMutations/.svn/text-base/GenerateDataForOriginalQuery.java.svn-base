package killMutations;

import generateConstraints.GenerateCommonConstraintsForQuery;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets for the original query. This data set is intended tom give non empty result for the original query
 * @author mahesh
 *
 */
public class GenerateDataForOriginalQuery {

	/**
	 * Generates data set for the original query
	 * @param cvc
	 */
	public static void generateDataForOriginalQuery(GenerateCVC1 cvc, String mutationType) throws Exception{

		System.out.println("\n----------------------------------");
		System.out.println("GENERATE DATA FOR ORIGINAL QUERY: ");
		System.out.println("----------------------------------\n");

		/** Initialize the data structures for generating the data to kill this mutation */
		cvc.inititalizeForDataset();
		
	
		/**set the type of mutation we are trying to kill*/
		cvc.setTypeOfMutation(mutationType);

		/** get the tuple assignment for this query
		 * If no possible assignment then not possible to kill this mutation*/
		if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
			return ;

		/**Get the constraints for all the blocks of the query */
		cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );

		/** Call the method for the data generation*/
		GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
	}
}
