package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.*;

import parsing.Conjunct;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill like conditions  mutations in the outer query block
 * @author mahesh
 *
 */
public class LikeMutationsInOuterQueryBlock {

	/**
	 * Generates data to kill like conditions mutations inside outer query block
	 * @param cvc
	 */
	public static void generateDataForkillingLikeMutationsInOuterQueryBlock( GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		System.out.println("\n----------------------------------");
		System.out.println("GENERATE DATA FOR KILLING LIKE CLAUSE MUTATIONS IN OUTER QUERY BLOCK");
		System.out.println("----------------------------------\n");

		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();

		/**Kill the like clause mutations in each conjunct of this outer block of  query */
		for(Conjunct conjunct: qbt.getConjuncts()){

			System.out.println("\n----------------------------------");
			System.out.println("NEW CONJUNCT IN LIKE CLAUSE MUTATIONS KILLING: " + conjunct);
			System.out.println("\n----------------------------------");

			/**Get the like conditions of this conjunct*/
			Vector<Node > likeConds = conjunct.getLikeConds();

			/** Kill each like condition of this conjunct*/
			for(int i=0; i < likeConds.size(); i++){

				System.out.println("\n----------------------------------");
				System.out.println("\n\nGETTING LIKE MUTANTS\n");
				System.out.println("\n----------------------------------");

				Node lc = likeConds.get(i);
				
				ArrayList<Node> likeMutants = UtilsRelatedToNode.getLikeMutations(lc);
				
				/** Generate data set to kill each mutation*/
				for(int j=0; j<likeMutants.size(); j++){

					/**If this mutation is not same as that of original condition*/
					if(!( likeMutants.get(j).getOperator().equalsIgnoreCase(lc.getOperator())) ){

						System.out.println("\n----------------------------------");
						System.out.println("KILLING : " + likeMutants.get(j));
						System.out.println("----------------------------------\n");

						/** This is required so that the tuple assignment for the subquery is fine*/
						likeConds.set(i,likeMutants.get(j) );

						/** Initialize the data structures for generating the data to kill this mutation */
						cvc.inititalizeForDataset();

						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.LIKE, TagDatasets.QueryBlock.OUTER_BLOCK );

						/** get the tuple assignment for this query
						 * If no possible assignment then not possible to kill this mutation*/
						if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
							continue;

						/** Add constraints for all the From clause nested subquery blocks */
						for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
							cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");

							cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

							cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
						}


						/** Generate postive constraints for all the conditions of this  conjunct */
						cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );


						/** Add negative conditions for all other conjuncts of this query block*/
						for(Conjunct inner: qbt.getConjuncts())
							if(inner != conjunct)
								cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	

						/** get group by constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
						cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );


						/** Generate havingClause constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
						for(int l=0; l< qbt.getNoOfGroups(); l++)
							for(int k=0; k < qbt.getAggConstraints().size();k++){
								cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
							}
						cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");

						/** add other constraints of outer query block */
						cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );

						/** Call the method for the data generation*/
						GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
					}
				}
				/**Revert the change in like conditions list of this  block */
				likeConds.set(i,lc);
			}
		}

		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}
}
