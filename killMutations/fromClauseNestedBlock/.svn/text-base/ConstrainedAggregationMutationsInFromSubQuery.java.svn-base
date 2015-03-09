package killMutations.fromClauseNestedBlock;

import generateConstraints.UtilsRelatedToNode;

import java.util.*;

import killMutations.GenerateDataForOriginalQuery;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill having clause mutations in each from clause nested subquery block
 * @author mahesh
 *
 */
public class ConstrainedAggregationMutationsInFromSubQuery {

	/**
	 * Generates data to kill having clause mutations inside from clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingConstrainedAggregationInFromSubquery(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		/** Kill having clause mutations in each from clause nested block of this query*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){
			
			System.out.println("\n----------------------------------");
			System.out.println("GENERATE DATA FOR Killing HAVING clause Mutations Inside From clause subquery block: "+ qbt);
			System.out.println("----------------------------------\n");
			
			/**Get the having clause node of this subquery block*/
			Node havingClause = qbt.getHavingClause();
			
			if(havingClause == null)
				continue ;
			
			Node cloneHavingClause = (Node)havingClause.clone();
			ArrayList<Node> havingMutants = new ArrayList<Node>();
			
			if(havingClause !=null){/**If there is having clause*/
				
				System.out.println("\n----------------------------------");
				System.out.println("KILLING HAVING MUTATIONS IN FROM CLAUSE SUBQUERY BLOCK" + havingClause);
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
						qbt.setHavingClause( havingMutants.get(k) );
						
						String mutationType = TagDatasets.MutationType.HAVING.getMutationType() + TagDatasets.QueryBlock.FROM_SUBQUERY.getQueryBlock();
						
						/**Use the original method */
						GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc, mutationType);
					}
				}
				/**Revert back to the old assignment*/
				qbt.setHavingClause( cloneHavingClause );
			}
		}
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

}
