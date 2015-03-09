package killMutations.whereClauseNestedBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import parsing.Conjunct;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill like conditions  mutations in the Where clause nested subquery block
 * @author mahesh
 *
 */
public class LikeMutationsInWhereSubquery {

	/**
	 * Generates data to kill like conditions mutations inside Where clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingLikeMutationsInWhereSubquery( GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/** we have to check if there are where clause subqueries in each conjunct of outer block of query */
		for(Conjunct con: cvc.getOuterBlock().getConjuncts()){

			/**For each where clause subquery blocks of this conjunct*/
			/** Kill like conditions  mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this subquery node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause subquery block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				/** Kill like conditions mutations in each conjunct of this where clause nested block of this query*/
				for(Conjunct conjunct: qbt.getConjuncts()){

					System.out.println("\n----------------------------------");
					System.out.println("NEW CONJUNCT IN LIKE MUTATIONS KILLING: " + conjunct);
					System.out.println("\n----------------------------------");

					/**Get the like conditions of this conjunct*/
					Vector<Node > likeConds = conjunct.getLikeConds();

					/** Kill each like conditions of this conjunct*/
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
								cvc.setTypeOfMutation( TagDatasets.MutationType.LIKE, TagDatasets.QueryBlock.WHERE_SUBQUERY );
								
								/** get the tuple assignment for this query
								 * If no possible assignment then not possible to kill this mutation*/
								if(cvc.tupleAssignmentForQuery(cvc) == false)
									continue;


								/** Add constraints for all the From clause nested sub query blocks except this sub query block */
								for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){								
									cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");

									cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

									cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");								}

								

								/** Add negative conditions for all other conjuncts of this sub query block*/
								for(Conjunct inner: qbt.getConjuncts()){
									if(inner != conjunct){
										cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
									}
								}

								/** get constraints for this conjunct of outer query block, except for where clause sub query block */
								cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptWhereClauseSubQueryBlock( cvc, cvc.getOuterBlock(), con) );
								
								/** Also Generates positive constraints for all the conditions of this sub query block conjunct*/
								/** And we need to add the positive conditions for all other where clause sub query blocks in this conjunct*/
								GenerateConstraintsForWhereClauseSubQueryBlock.generateConstraintsForKillingMutationsInWhereSubqueryBlock(cvc, qbt, con, conjunct, subQCond, 0);

								/**add the negative constraints for all the other conjuncts of outer query block */
								for(Conjunct outer: cvc.getOuterBlock().getConjuncts())
									if( !outer.equals(con))
										cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, cvc.getOuterBlock(), outer) );

								/** add group by and having clause constraints for outer query block */
								cvc.getConstraints().add( QueryBlockDetails.getGroupByAndHavingClauseConstraints(cvc, cvc.getOuterBlock())) ;

								/**Add other related constraints for the outer query block */
								cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock())) ;

								/** Call the method for the data generation*/
								GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
							}
						}
						/**Revert the change in selection conditions list of this subquery block */
						likeConds.set(i,lc);
					}
				}
			}
		}
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

}