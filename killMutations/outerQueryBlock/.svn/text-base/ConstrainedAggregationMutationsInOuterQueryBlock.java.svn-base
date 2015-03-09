package killMutations.outerQueryBlock;

import generateConstraints.UtilsRelatedToNode;

import java.util.*;

import killMutations.GenerateDataForOriginalQuery;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill having clause mutations in each outer query block
 * @author mahesh
 *
 */
public class ConstrainedAggregationMutationsInOuterQueryBlock {

	/**
	 * Generates data to kill having clause mutations inside outer query block
	 * @param cvc
	 */
	public static void generateDataForkillingConstrainedAggregationInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		/**Get outer query block */
		QueryBlockDetails outer = cvc.getOuterBlock();
		
		System.out.println("\n----------------------------------");
		System.out.println("GENERATE DATA FOR KILLING HAVING MUTATIONS IN OUTER BLOCK OF QUERY: " + outer);
		System.out.println("----------------------------------\n");
		
		/**Get the having clause node of this query block*/
		Node havingClause = outer.getHavingClause();
		
		if(havingClause == null)
			return ;
		
		Node cloneHavingClause = (Node)havingClause.clone();
		ArrayList<Node> havingMutants = new ArrayList<Node>();
		
		if(havingClause !=null && havingClause.getType()!=null){/**If there is having clause*/
			
			System.out.println("\n----------------------------------");
			System.out.println("KILLING HAVING MUTATIONS IN OUTER BLOCK OF QUERY: " + havingClause);
			System.out.println("----------------------------------\n");
			
			/**Get different mutations for this having clause*/
			havingMutants = UtilsRelatedToNode.getHavingMutations(cloneHavingClause);
			
			/**Kill each mutation*/
			for(int k=0;k<havingMutants.size();k++){
				
				System.out.println("\n----------------------------------");
				System.out.println("KILLING: "+havingMutants.get(k));
				System.out.println("\n----------------------------------");
				
				/** generate data sets for each mutation */
				if( !(havingMutants.get(k).equals(cloneHavingClause)) ){
					
					
					
					/** Replace original having clause mutation with this mutation*/
					outer.setHavingClause( havingMutants.get(k) );
					
					String mutationType = TagDatasets.MutationType.HAVING.getMutationType() + TagDatasets.QueryBlock.OUTER_BLOCK.getQueryBlock();
					
					/**Use the original method */
					GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc, mutationType);
				}
			}
			
			/**Revert back to the old assignment*/
			outer.setHavingClause( cloneHavingClause );
		}
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		
	}

}
