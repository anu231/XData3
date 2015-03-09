package killMutations.whereClauseNestedBlock;

import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.GenerateJoinPredicateConstraints;
import generateConstraints.RelatedToEquivalenceClassMutations;
import generateConstraints.UtilsRelatedToNode;

import java.util.*;

import parsing.Column;
import parsing.Conjunct;
import parsing.Node;
import parsing.RelationHierarchyNode;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.TagDatasets;

public class MutationsInNotExistsSubquery {

	/**
	 * Generates data to kill equivalence class mutations inside Where clause nested subquery block
	 * @param cvc
	 * @throws Exception
	 */
	
	GenerateCVC1 cvc;
	
	RelationHierarchyNode topLevelRelation;
	
	public MutationsInNotExistsSubquery(GenerateCVC1 cvc, RelationHierarchyNode relation){
		this.cvc = cvc;
		this.topLevelRelation = relation;
	}
	
	public static void genDataToKillMutantsInNotExistsSubquery(GenerateCVC1 cvc) throws Exception{
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		int notExistsCount = 0;
		
		QueryBlockDetails topQbt = cvc.getOuterBlock();
		
		/** we have to check if there are where clause sub queries in each conjunct of outer block of query */
		for(Conjunct con: cvc.getOuterBlock().getConjuncts()){

			/**For each where clause sub query blocks of this conjunct*/
			/** Kill equivalence class mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){
				
				System.out.println(subQCond.getType());
				
				if (subQCond.getType().equalsIgnoreCase(Node.getNotExistsNodeType())) {
					int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);
					QueryBlockDetails qbt = topQbt.getWhereClauseSubQueries().get(index);
					MutationsInNotExistsSubquery mutationKiller = new MutationsInNotExistsSubquery(cvc, topQbt.getTopLevelRelation());
					mutationKiller.genConstraintsForNotExists(qbt, topQbt.getTopLevelRelation().getNotExistsNode(notExistsCount));
					notExistsCount++;
				}			
			}
		}
		
		GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
	}
	
	public void genConstraintsForNotExists(QueryBlockDetails qbt, RelationHierarchyNode node) throws Exception{
		ArrayList<String> baseRelations = qbt.getBaseRelations();
		
		if(node.getNodeType().equals("_RELATION_")){
			ArrayList<Conjunct> conjuncts = qbt.getConjuncts();
			for(Conjunct c:conjuncts){
				
				/** Keep a copy of the original equivalence classes*/
				Vector<Vector<Node>> equivalenceClassesOrig = (Vector<Vector<Node>>)c.getEquivalenceClasses().clone();
				
				String constraint = GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, c);
				cvc.getConstraints().add(constraint);
			}
		}
		else if(node.getNodeType().equals("_LEFT_JOIN_")){
			ArrayList<Conjunct> conjuncts = qbt.getConjuncts();
			String leftTable = node.getLeft().getTableName();
			List<Node> selectionConds = new ArrayList<Node>(); 
			for(Conjunct c:conjuncts){
				for(Node n : c.getSelectionConds()){
					if(n.getTable() == null || n.getTable().getTableName().equals(leftTable)){
						selectionConds.add(n);
					}
				}
			}
			
			String res = GenerateCVCConstraintForNode.generateNegativeConditionsForNodesList(cvc, qbt, selectionConds);
			cvc.getConstraints().add(res);
		}
		else if(node.getNodeType().equals("_RIGHT_JOIN_")){
			ArrayList<Conjunct> conjuncts = qbt.getConjuncts();
			String rightTable = node.getRight().getTableName();
			List<Node> selectionConds = new ArrayList<Node>(); 
			for(Conjunct c:conjuncts){
				for(Node n : c.getSelectionConds()){
					if(n.getTable() == null || n.getTable().getTableName().equals(rightTable)){
						selectionConds.add(n);
					}
				}
			}
			String res = GenerateCVCConstraintForNode.generateNegativeConditionsForNodesList(cvc, qbt, selectionConds);
			cvc.getConstraints().add(res);
		}
		else if(node.getNodeType().equals("_JOIN_")){
			ArrayList<Conjunct> conjuncts = qbt.getConjuncts();
			String leftTable = node.getLeft().getTableName();
			String rightTable = node.getRight().getTableName();
			List<Node> selectionConds = new ArrayList<Node>(); 
			for(Conjunct c:conjuncts){
				for(Node n : c.getAllConds()){
					if(n.getLeft() != null && n.getRight() != null){
						if(n.getLeft().getTable().getTableName().equals(leftTable) && n.getRight().getTable().getTableName().equals(rightTable) || 
								n.getLeft().getTable().getTableName().equals(rightTable) && n.getRight().getTable().getTableName().equals(leftTable)){
							selectionConds.add(n);
						}
					}
				}
				
				for(Node n : c.getJoinConds()){
					if(n.getLeft() != null && n.getRight() != null){
						if(n.getLeft().getTable().getTableName().equals(leftTable) && n.getRight().getTable().getTableName().equals(rightTable) || 
								n.getLeft().getTable().getTableName().equals(rightTable) && n.getRight().getTable().getTableName().equals(leftTable)){
							selectionConds.add(n);
						}
					}
				}
				
				for(Node n : c.getSelectionConds()){
					if(n.getTable() == null || n.getTable().getTableName().equals(leftTable) || n.getTable().getTableName().equals(rightTable)){
						selectionConds.add(n);
					}
				}
			}
			
			String res = GenerateCVCConstraintForNode.generateNegativeConditionsForNodesList(cvc, qbt, selectionConds);
			cvc.getConstraints().add(res);
		}
	}
}
