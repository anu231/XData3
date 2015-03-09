package generateConstraints;

import java.sql.Types;
import java.util.*;

import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * Common methods for dealing with nodes
 * @author mahesh
 *
 */
public class UtilsRelatedToNode {

	/**
	 * Check whether this Node has conditions which invovle Strings
	 * @param n--condition
	 * @param flag
	 * @return
	 * @throws Exception
	 */

	public static boolean isStringSelection(Node n,int flag) throws Exception{
		if(n.getLeft().getType().equals(Node.getColRefType())/* */){
			if(flag==1  && !n.getRight().getType().equals(Node.getValType()))
				return false;
			int i=n.getLeft().getColumn().getDataType();
			if(i== Types.VARCHAR || i==Types.CHAR || i==Types.LONGVARCHAR){
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Used to get number of groups for the query block in which this node is present
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static int getNoOfGroupsForThisNode(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n) {
		
		/** If it is from clause sub query*/
		if(n.getQueryType() == 1)
			if(queryBlock.getFromClauseSubQueries() != null && queryBlock.getFromClauseSubQueries().size() != 0)
				return queryBlock.getFromClauseSubQueries().get(n.getQueryIndex()).getNoOfGroups();
			else
				return queryBlock.getNoOfGroups();
		
		/** If this is where clause subquery node */
		else if (n.getQueryType() == 2)
			if(queryBlock.getWhereClauseSubQueries() != null && queryBlock.getWhereClauseSubQueries().size() != 0)
				return queryBlock.getWhereClauseSubQueries().get(n.getQueryIndex()).getNoOfGroups();
			else
				return queryBlock.getNoOfGroups();
		return queryBlock.getNoOfGroups();
	}
	
	/**
	 * This method is used to get index of this where clause subquery block 
	 * @param n
	 * @return
	 */
	public static int getQueryIndexOfSubQNode(Node n) {
		if(n == null)
			return -1;
		int index;
		/** get the index of this where clause subquery */
		if(n.getType().equals(Node.getNotExistsNodeType()) || n.getType().equals(Node.getExistsNodeType())){
			
			if( n.getLhsRhs() != null)
				index = n.getLhsRhs().getQueryIndex();
			else
				index = n.getSubQueryConds().firstElement().getRight().getQueryIndex();/**for NOT IN Type*/
		}
		else if(n.getLhsRhs().getType().equals(Node.getAggrNodeType()))
			index = n.getLhsRhs().getQueryIndex();
		else
			index = n.getLhsRhs().getRight().getQueryIndex();
		return index;
	}
	
	/**
	 * Used to get number of tuples for the relation to which the given node corresponds
	 * This does not include the number of groups, if the input node corresponds to subquery blocks
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static int getNoOfTuplesForThisNode(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node node) throws Exception{
		
		/**If this node corresponds to from clause nested subquery block */
		if(node.getQueryType() == 1)
			/**If there are from clause nested subqueries, 
			 * it means this node is in outer query block but the relation used is inside the from clause nested subquery block*/
			if(queryBlock.getFromClauseSubQueries() != null && queryBlock.getFromClauseSubQueries().size() != 0)
				/** We should get the number of groups of this from clause subquery block */
				return queryBlock.getFromClauseSubQueries().get(node.getQueryIndex()).getNoOfGroups();
			else/** Means this node is inside from clause subquery block */
				return cvc.getNoOfTuples().get(node.getTableNameNo());
		else /**If this node is inside where clause subqueries or in outer query block then just get the number of tuples of that relation only*/
			return cvc.getNoOfTuples().get(node.getTableNameNo());
		
	}
	
	/**
	 * Find the different relations involved in pred. Pred might be an arbitrary predicate 
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static HashMap<String,Table> getListOfRelationsFromNode(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n){
		HashMap<String, Table> rels = new HashMap<String, Table>();

		if(n.getType() != null && n.getType().equalsIgnoreCase(Node.getColRefType())){
			if(!rels.containsKey(n.getTableAlias())){
				rels.put(n.getTableAlias(), n.getTable());
			}
		}
		else{
			if(n.getLeft() != null)
				rels.putAll(getListOfRelationsFromNode(cvc, queryBlock, n.getLeft()));
			if(n.getRight() != null)
				rels.putAll(getListOfRelationsFromNode(cvc, queryBlock, n.getRight()));
		}	
		return rels;
	}
	
	public static int getMaxCountForPredAgg(GenerateCVC1 cvc, Node n){
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return cvc.getNoOfOutputTuples().get(n.getColumn().getTableName());/**FIXME: Handle repeated relations */
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			return 0;
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || 
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			int a = getMaxCountForPredAgg(cvc, n.getLeft());
			int b = getMaxCountForPredAgg(cvc, n.getRight());
			if(a > b) return a; 
			return b;
		}
		return 0;		
	}
	
	
	/**
	 * Get all the having clause mutations
	 * @param havingClause
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Node> getHavingMutations(Node havingClause) throws Exception {
	
		ArrayList<Node> mutantList = new ArrayList<Node>();
		
		mutantList = getMutants(havingClause,havingClause);
	
		return mutantList;
	}
	
	/** 
	 * Actual method that gets all the having clause mutations
	 * @param havingClause
	 * @param pointer
	 * @return
	 * @throws Exception
	 */

	public static ArrayList<Node> getMutants(Node havingClause,Object pointer) throws Exception{
		
		ArrayList<Node> list= new ArrayList<Node>();
		if(!(((Node)pointer).getNodeType().equalsIgnoreCase(Node.getBroNodeType() )))
			if( ((Node)pointer).getLeft()!=null){
				list.addAll(getMutants(havingClause,((Node)pointer).getLeft()));
			}
		if( ((Node)pointer).getNodeType().equalsIgnoreCase(Node.getBroNodeType() )){

			Node copy=(Node)havingClause.clone();
			Node clone=(Node)((Node)pointer).clone();
			String opr=((Node)pointer).getOperator();
			Node mut=new Node();

			((Node)pointer).setOperator(">");
			mut=(Node)havingClause.clone();
			list.add((Node)mut.clone());

			((Node)pointer).setOperator("<");
			mut=(Node)havingClause.clone();
			list.add(mut);

			((Node)pointer).setOperator("=");
			mut=(Node)havingClause.clone();
			list.add(mut);

			((Node)pointer).setOperator("/=");
			mut=(Node)havingClause.clone();
			list.add(mut);

			//Make original operator
			((Node)pointer).setOperator(opr);
		}

		if(!(((Node)pointer).getNodeType().equalsIgnoreCase(Node.getBroNodeType() )))
			if( ((Node)pointer).getRight()!=null){
				list.addAll(getMutants(havingClause,((Node)pointer).getRight()));
			}

		return list;
	}
	
	
	/**
	 * Finding the Maximum And Minum Values for a given column
	 * @param c-column
	 * @param conds
	 * @return
	 */
	
	
	public static int[] getMaxMinForIntCol(Column c,Vector<Node> conds){
		int[] maxMin=new int[2];
		maxMin[0]=(int)c.getMaxVal();
		maxMin[1]=(int)c.getMinVal();
		for(Node temp:conds){
			Node l=temp.getLeft();
			Node r=temp.getRight();
			if(l.getTable().getTableName().equalsIgnoreCase(c.getTableName())
					&& l.getColumn().getColumnName().equalsIgnoreCase(c.getColumnName())
					&& r.getStrConst()!=null){
				int num=Integer.parseInt(r.getStrConst());
				if(temp.getOperator().equalsIgnoreCase("<")){				
					if(maxMin[0]>num-1)
						maxMin[0]=num-1;
				}
				else if(temp.getOperator().equalsIgnoreCase("<=")){	
					if(maxMin[0]>num)
						maxMin[0]=num;
				}
				else if(temp.getOperator().equalsIgnoreCase(">")){
					if(maxMin[1]<num+1)
						maxMin[1]=num+1;
				}
				else if(temp.getOperator().equalsIgnoreCase(">=")){
					if(maxMin[1]<num)
						maxMin[1]=num;
				}
				else if(temp.getOperator().equalsIgnoreCase("=")){
					maxMin[1]=num;
					maxMin[0]=num;
					return maxMin;
				}
			}
		}
		return maxMin;
	}

	/**
	 * Checks whether the given node involves any column from the given relation name
	 * @param cond
	 * @param tableName
	 * @return
	 */
	public static boolean checkIfCorrespondToThisTable(Node cond,	String tableName) {
		
		if(cond == null)
			return false;
		
		boolean present = false;
		if( cond.getTable() != null && cond.getTable().getTableName().equalsIgnoreCase(tableName))
			present = true;
		
		/**if left side is present check if left side correspond to this table*/
		if(cond.getLeft() != null)
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableName);
		
		/** check for right side*/
		if( present == false && cond.getRight() != null) //getLeft??
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableName);
		
		if( present == false && cond.getLhsRhs() != null)
			present = checkIfCorrespondToThisTable(cond.getLhsRhs(), tableName);
	
		return present;
	}
	
	/**
	 * Checks whether the given node involves any column from the given relation occurrence
	 * @param cond
	 * @param tableNameNo
	 * @return
	 */
	public static boolean checkIfCorrespondToThisTableOccurrence(Node cond,	String tableNameNo) {
		
		if(cond == null)
			return false;
		
		boolean present = false;
		
		/**if left side is present check if left side correspond to this table*/
		if(cond.getLeft() != null)
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableNameNo);
		
		/** check for right side*/
		if( present == false && cond.getRight() != null)
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableNameNo);
		
		if( present == false && cond.getLhsRhs() != null)
			present = checkIfCorrespondToThisTable(cond.getLhsRhs(), tableNameNo);
		
		if( present == false && cond.getTableNameNo() != null && cond.getTableNameNo().equalsIgnoreCase(tableNameNo))
			present = true;
		
		return present;
	}

	public static Vector<Node> getJoinConditions(Vector<Vector<Node>> eqClass) {
	
		Vector<Node> eqJoin = new Vector<Node>();
	
	
		for(Vector<Node> ec: eqClass)
			eqJoin.addAll(UtilsRelatedToNode.getJoinCondition(ec));
	
		return eqJoin;
	}

	public static Vector<Node> getJoinCondition(Vector<Node> ec) {
	
		Vector<Node> eqJoin = new Vector<Node>();
	
		for(int i=0; i<ec.size(); i++)
			for(int j = i+1; j<ec.size(); j++)
				eqJoin.add(UtilsRelatedToNode.createJoinNode(ec.get(i), ec.get(j)));
		return eqJoin;
	
	}

	public static Node createJoinNode(Node node1, Node node2) {
	
		Node join = new Node();
		join.setLeft(node1);
		join.setRight(node2);
		join.setOperator( "=");
		return join;
	}

	/**
	 * Gets the like mutations for the given like condition
	 * @param likeCond
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Node> getLikeMutations(Node likeCond) throws Exception{
		
		ArrayList<Node> likeMutants = new ArrayList<Node>();
		
		Node lcm = null;
		lcm = (Node)likeCond.clone();
		lcm.setOperator("~");
		likeMutants.add(lcm);
	
		lcm = (Node)likeCond.clone();
		lcm.setOperator("i~");
		likeMutants.add(lcm);
	
		lcm = (Node)likeCond.clone();
		lcm.setOperator("!i~");
		likeMutants.add(lcm);
		
		return likeMutants;
	}

	/**
	 * Used to get mutations for the given selection condition
	 * @param selectionCond
	 * @return
	 */
	public static Vector<Node> getSelectionCondMutations(Node selectionCond) throws Exception{
		
		Vector<Node> scMutants = new Vector<Node>();
		
		Node scm = null;
		scm = (Node)selectionCond.clone();
		scm.setOperator("=");					
		scMutants.add(scm);
	
		scm = (Node)selectionCond.clone();
		scm.setOperator("<");
		scMutants.add(scm);
	
		scm = (Node)selectionCond.clone();
		scm.setOperator(">");
		scMutants.add(scm);
	
		scm = (Node)selectionCond.clone();
		scm.setOperator("/=");
		scMutants.add(scm);
		
		return scMutants;
	}

	/**
	 * Used to get mutations for the given selection condition
	 * @param stringCond
	 * @return
	 */
	public static Vector<Node> getStringSelectionCondMutations(Node stringCond) throws Exception{
		
		Vector<Node> scMutants = new Vector<Node>();
		
		Node scm = null;
		scm = (Node)stringCond.clone();
		scm.setOperator("=");					
		scMutants.add(scm);
	
		scm = (Node)stringCond.clone();
		scm.setOperator("<");
		scMutants.add(scm);
	
		scm = (Node)stringCond.clone();
		scm.setOperator(">");
		scMutants.add(scm);
	
		scm = (Node)stringCond.clone();
		scm.setOperator("/=");
		scMutants.add(scm);
		
		return scMutants;
	}

	/**
	 * check if this node corresponds to the given relation occurrence
	 * @param n
	 * @param col
	 * @param relationNo
	 * @return
	 */
	public static boolean presentNode(Node n, Column col, String relationNo) {
		
		boolean present = false;
		
		if( n.getLeft() != null && n.getLeft().getTableNameNo() != null)
			present = present || presentNode(n.getLeft(), col, relationNo);
		
		if( present == false && n.getRight() != null && n.getRight().getTableNameNo() != null)
			present = present || presentNode(n.getRight(), col, relationNo);
		
		if( present == false && n.getTableNameNo() != null){
			
			if(n.getTableNameNo().equalsIgnoreCase(relationNo) && 
					n.getColumn().getColumnName().equalsIgnoreCase(col.getColumnName()) )
				present = true;
		}
		return present;
	}

	/**
	 * Checks if the given column of the relation occurrence is present in the given conditions
	 * @param conds
	 * @param col
	 * @param relationNo
	 * @return
	 */
	public static boolean presentInConds(Vector<Node> conds, Column col, String relationNo) {
		
		/**for each node*/
		for(Node n: conds)
			if( presentNode(n, col, relationNo))
				return true;
		return false;
	}
	
}
