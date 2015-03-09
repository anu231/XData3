package testDataGen;

import java.util.*;

import parsing.Conjunct;
import parsing.Node;
import testDataGen.GetTupleAssignment;
import util.JoinGraphNode;

/**
 * This is a wrapper class that does the preprocessing activity needed for actual tuple assignment method
 * If tuple assignment method is successful then this class also updates the required datastructures
 * @author mahesh
 *
 */
public class GetTupleAssignmentForQueryBlock {


	public static boolean getTupleAsgnmentForQueryBlock(GenerateCVC1 cvc,	QueryBlockDetails queryBlock, String rootTableName ) throws Exception {
		/** If Join conditions are present*/

		/** get join conditions of this query block*/
		ArrayList<Node > joinConds = new ArrayList<Node>();

		/**TODO: Which conjunct should be consider for the tuple assignment
		 * For now consider all the conjuncts, but incorrect
		 */
		for(Conjunct con: queryBlock.getConjuncts())
			for(Node n: con.getJoinConds())
				joinConds.add(new Node(n));

		if(joinConds != null && joinConds.size()!=0){
			GetTupleAssignment gta = new GetTupleAssignment(rootTableName);

			gta.setJoinConds(joinConds);

			/**Get group by nodes of this query block*/

			ArrayList<Node> groupByNodes = new ArrayList<Node>();	
			for(Node n: queryBlock.getGroupByNodes())
				groupByNodes.add(new Node(n));

			/**Update the group by nodes*/
			gta.setGroupByNodes(groupByNodes);

			/**Update the equivalence classes of this query block*/
			ArrayList<ArrayList<Node>> eqClas = new ArrayList<ArrayList<Node>>();
			for(Conjunct con: queryBlock.getConjuncts()){
				ArrayList<ArrayList<Node>> ec = new ArrayList<ArrayList<Node>>(); 
				for(Vector<Node> e:	con.getEquivalenceClasses())
					ec.add(new ArrayList(e));

				eqClas.addAll(ec);
			}
			gta.setEquivalenceClasses((ArrayList<ArrayList<Node>>) eqClas.clone());

			/**Update the final count*/
			gta.setFinalCount(queryBlock.getFinalCount());

			/** Update selection conditions*/
			ArrayList< Node > selectionConds = new ArrayList<Node>();
			ArrayList< Node > stringSelectionConds = new ArrayList<Node>();
			for(Conjunct conjunct: queryBlock.getConjuncts()){
				/** get selection conditions*/
				selectionConds.addAll(conjunct.getSelectionConds());

				/** get string selection conds*/
				stringSelectionConds.addAll(conjunct.getStringSelectionConds());
			}
			gta.setSelectionConds(selectionConds);
			gta.setStringSelectionConds(stringSelectionConds);

			/** Update the unique elements if any*/
			for(HashSet<Node> un: queryBlock.getUniqueElementsAdd())
				gta.addUniqueElements(un);

			/** Update the single valued attributes  if any*/
			for(Node n: queryBlock.getSingleValuedAttributesAdd())
				groupByNodes.add(new Node(n));


			/** The group by nodes of from clause subquery must be added to unique elements set, when we are getting the assignment of outer query block */

			int subq = 0;
			for(QueryBlockDetails qb: queryBlock.getFromClauseSubQueries()){

				/** A wrapper to modify the join conditions that involve attributes of nested sub query from different set of relations
				 * keep a mapping from sub query actual attribute to some name 
				 */
				gta.createRelationNodeForSubquery(qb, subq);
			}


			boolean assignmentPossible = gta.getTupleAssignmentForQuery();

			if(assignmentPossible == false){
				System.out.println(" NO POSSIBLE ASSIGNMENT ");
				return false;
			}

			assignmentFromJoinGraphVericesToRelationsInQuery(cvc, queryBlock, gta);
		}
		return true;
	}



	/**
	 * This method updates the data structures that are used to store the number of tuples for each relation occurrence and their positions
	 * @param cvc
	 * @param gta
	 */
	private static void assignmentFromJoinGraphVericesToRelationsInQuery(GenerateCVC1 cvc, QueryBlockDetails queryBlock, GetTupleAssignment gta) {

		/** get vertices of the join graph*/
		Vector<JoinGraphNode> relations = new Vector<JoinGraphNode>();
		relations = gta.getJoinGraph().getAllVertex();

		Iterator<JoinGraphNode>  it = relations.iterator();

		while(it.hasNext()){

			JoinGraphNode relation = it.next();
			/** If this relation node correspond to a from clause subquery block , then we should update number of groups of that subquery block */
			if(gta.getTableMapping().containsValue(relation.getTableNameNo())){

				int index = relation.getQueryIndex();
				cvc.getOuterBlock().getFromClauseSubQueries().get(relation.getQueryIndex()).setNoOfGroups(relation.getCardinality());

				System.out.println("\nAssigning number of groups: = "+relation.getCardinality());

				/** update noofoutputtuples */
				for( String key: gta.getTableMapping().keySet()){
					/** If this table is involved in this from clause sub query block */
					if(gta.getTableMapping().get(key).equalsIgnoreCase(relation.getTableNameNo())){

						/** get the nooftuples of this relation occurrence */
						int count = 0;
						if(cvc.getNoOfTuples().get(key) != null)
							count = cvc.getNoOfTuples().get(key);

						/**decrease this count from noofoutputtuples */
						int totalCount = 0;
						if(cvc.getNoOfOutputTuples().get(key.substring(0, key.length()-1)) != null)
							totalCount = cvc.getNoOfOutputTuples().get(key.substring(0, key.length()-1));

						String tableName = key.substring(0, key.length()-1);

						/** update noofoutput tuples */
						if(count != 0)
							cvc.getNoOfOutputTuples().put(key.substring(0, key.length()-1), totalCount - count + (count*relation.getCardinality()) );
						else
							cvc.getNoOfOutputTuples().put(key.substring(0, key.length()-1), totalCount - count + relation.getCardinality() );

						/** Update repeated next tuple position*/
						int thisTablePos=Integer.parseInt(key.substring(key.length()-1));
						for(int i=thisTablePos+1;i <= cvc.getRepeatedRelationCount().get(tableName);i++){
							Integer[] j = cvc.getRepeatedRelNextTuplePos().get(tableName+(thisTablePos+1));
							j[1] = j[1] + queryBlock.getFinalCount() - 1;
							cvc.getRepeatedRelNextTuplePos().put(tableName+(thisTablePos+1), j);
						}
					}
				}

			}
			/** update the number of tuples of this base relation occurrence*/
			else{

				/** get the table name*/
				String tableNameNo = relation.getTableNameNo();

				int count = relation.getCardinality();

				int prevCount ;/**To denote the number of tuples of this relation occurrence previously*/

				if(cvc.getNoOfTuples().get(tableNameNo) == null){/**If the count was not updated previously for this relation occurrence*/
					cvc.getNoOfTuples().put(tableNameNo, count);
					prevCount = 0;
				}
				else{
					prevCount = cvc.getNoOfTuples().get(tableNameNo);				
					cvc.getNoOfTuples().put(tableNameNo,  prevCount + count - 1);
				}

				String tableName = relation.getTable().getTableName();

				/** update the repeated relation position */
				int thisTablePos=Integer.parseInt(tableNameNo.substring(tableNameNo.length()-1));
				for(int i=thisTablePos+1;i <= cvc.getRepeatedRelationCount().get(tableName);i++){
					Integer[] j = cvc.getRepeatedRelNextTuplePos().get(tableName+(thisTablePos+1));
					j[1] = j[1] + count - 1;
					cvc.getRepeatedRelNextTuplePos().put(tableName+(thisTablePos+1), j);
				}

				/**  update number of tuples based on noOfGroups and noOftuples for each relation occurrence */
				int totalCount = count * queryBlock.getNoOfGroups();/**The total number of tuples contributed by this relation occurrence now*/

				int totPrevCount = prevCount * queryBlock.getNoOfGroups();/**get the total number of tuples contributed by this relation occurrence previously*/

				/**update the total number of tuples*/
				if( cvc.getNoOfOutputTuples().get(tableName) == null)
					cvc.getNoOfOutputTuples().put(tableName, totalCount );
				else
					cvc.getNoOfOutputTuples().put(tableName, cvc.getNoOfOutputTuples().get(tableName)+ totalCount - totPrevCount );
			}
		}

		System.out.println("\nFinal count = "+ queryBlock.getFinalCount() +" \n No of output Tuples for each relation occurrence assigned:\n"+ cvc.getNoOfTuples().toString() + "\n");

		System.out.println(" \n Total No of output Tuples for each relation  assigned:\n"+ cvc.getNoOfOutputTuples().toString());

		gta.getOriginalColumnNames(cvc.getqParser().getTableMap());

		/** Update the elements that are to be unique and single valued. These are used for generating the constraints
		 * We are removing the group by nodes of the sub queries from the list of unique elements, because these are to be unique across multiple groups and this is ensured while generating 
		 * the constraints for the sub query group by nodes 
		 */

		for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
			HashSet<Node> group = new HashSet<Node>(qb.getGroupByNodes());
			HashSet<HashSet<Node>> uniqueSet = new HashSet<HashSet<Node>>();
			uniqueSet.addAll(gta.getUniqueElements());

			for(HashSet<Node> unq: uniqueSet)
				if(group.containsAll(unq))
					gta.removeUniqueElements(unq);

		}

		/** Add the unique elements and single valued attributes inferred to the query block*/
		queryBlock.setUniqueElements(gta.getUniqueElements());
		queryBlock.setSingleValuedAttributes(gta.getSingleValuedAttributes());

		System.out.println("\nUnique Elements: " + gta.getUniqueElements());
		System.out.println("\nSV Elements: " + gta.getSingleValuedAttributes());
		System.out.println("-------------------------------------------------");
		System.out.println("-------------------------------------------------");
	}



	/**
	 * Assigns the number of tuples for this query block, if there are no join conditions in this query block
	 * @param cvc
	 * @param queryBlock
	 * @return
	 */
	public static boolean getTupleAssignmentWithoutJoins(GenerateCVC1 cvc,	QueryBlockDetails queryBlock) {

		/**check if there is From clause nested block in outer block
		 * If there is, we should assign the count to number of groups of From clause nested block*/
		if(queryBlock.getFromClauseSubQueries() != null && queryBlock.getFromClauseSubQueries().size() > 0 && (queryBlock.getBaseRelations() == null || queryBlock.getBaseRelations().size() == 0)){

			queryBlock.getFromClauseSubQueries().get(0).setNoOfGroups( queryBlock.getFinalCount());

			for(String key: queryBlock.getFromClauseSubQueries().get(0).getBaseRelations()){
				/** get the nooftuples of this relation occurrence */
				int count = 0;
				if(cvc.getNoOfTuples().get(key) != null)
					count = cvc.getNoOfTuples().get(key);

				/**decrease this count from noofoutputtuples */
				int totalCount = 0;
				if(cvc.getNoOfOutputTuples().get(key.substring(0, key.length()-1)) != null)
					totalCount = cvc.getNoOfOutputTuples().get(key.substring(0, key.length()-1));

				String tableName = key.substring(0, key.length()-1);

				/** update noofoutput tuples */
				if(count != 0)
					cvc.getNoOfOutputTuples().put(key.substring(0, key.length()-1), totalCount - count + (count*queryBlock.getFinalCount()) );
				else
					cvc.getNoOfOutputTuples().put(key.substring(0, key.length()-1), totalCount - count + queryBlock.getFinalCount() );

				/** Update repeated next tuple position*/
				int thisTablePos=Integer.parseInt(key.substring(key.length()-1));
				for(int i=thisTablePos+1;i <= cvc.getRepeatedRelationCount().get(tableName);i++){
					Integer[] j = cvc.getRepeatedRelNextTuplePos().get(tableName+(thisTablePos+1));
					j[1] = j[1] + queryBlock.getFinalCount() - 1;
					cvc.getRepeatedRelNextTuplePos().put(tableName+(thisTablePos+1), j);
				}
			}
			return true;
		}
		/** Get the table name that is present in this query block*/
		/**FIXME: What if it is an aliased column of some other table*/

		/**Stores the table occurrence*/
		String tableNameNo = "";
		if(queryBlock.getAggConstraints() != null && queryBlock.getAggConstraints().size()!=0)/**If there are aggregation constraints*/
			tableNameNo = queryBlock.getAggConstraints().get(0).getLeft().getAgg().getAggExp().getTableNameNo();
		else {
			// queries may not have any projected attributes as in delete
			if(queryBlock.getProjectedCols() != null || queryBlock.getProjectedCols().size() == 0){
				return false;
			}
			if(queryBlock.getProjectedCols().get(0).getType().equalsIgnoreCase(Node.getAggrNodeType())){/**FIXME: Bug here if COUNT(*)*/
				tableNameNo = queryBlock.getProjectedCols().get(0).getAgg().getAggExp().getTableNameNo();/** If it is an aggregate function*/
			}
			else/**If the projected type is of column*/
				tableNameNo = queryBlock.getProjectedCols().get(0).getTableNameNo();
			
		}


		System.out.println("Table Name No: " + tableNameNo);

		/**Get the base table name*/
		String tableName = tableNameNo.substring(0, tableNameNo.length()-1);

		/**stores the number of tuples for this relation occurrence before this assignment*/
		int prevCount;

		/**store the present count*/
		int count = queryBlock.getFinalCount();

		/**Update the count of this relation in the respected data structures*/ 
		if(cvc.getNoOfTuples().get(tableNameNo) == null){/**If the count was not updated previously for this relation occurrence*/
			cvc.getNoOfTuples().put(tableNameNo, queryBlock.getFinalCount());
			prevCount = 0;
		}
		else{
			prevCount = cvc.getNoOfTuples().get(tableNameNo);
			cvc.getNoOfTuples().put(tableNameNo, prevCount + count - 1);
		}

		/** update the repeated relation position*/
		int thisTablePos=Integer.parseInt(tableNameNo.substring(tableNameNo.length()-1));
		for(int i=thisTablePos+1;i<=cvc.getRepeatedRelationCount().get(tableName);i++){
			Integer[] j = cvc.getRepeatedRelNextTuplePos().get(tableName+(thisTablePos+1));
			j[1] = j[1] + queryBlock.getFinalCount() - 1;
			cvc.getRepeatedRelNextTuplePos().put(tableName+(thisTablePos+1), j);
		}

		int totalCount = count * queryBlock.getNoOfGroups();/**The total number of tuples contributed by this relation occurrence now*/

		int totPrevCount = prevCount * queryBlock.getNoOfGroups();/**get the total number of tuples contributed by this relation occurrence previously*/

		/**update the total number of tuples*/
		if( cvc.getNoOfOutputTuples().get(tableName) == null)
			cvc.getNoOfOutputTuples().put(tableName, totalCount );
		else
			cvc.getNoOfOutputTuples().put(tableName, cvc.getNoOfOutputTuples().get(tableName)+ totalCount - totPrevCount );

		System.out.println("\nFinal count = "+ queryBlock.getFinalCount() +" \n No of output Tuples for each relation occurrence assigned:\n"+ cvc.getNoOfTuples().toString() + "\n");

		System.out.println(" \n Total No of output Tuples for each relation  assigned:\n"+ cvc.getNoOfOutputTuples().toString());

		return true;
	}

}
