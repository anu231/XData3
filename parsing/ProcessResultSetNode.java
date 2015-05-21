package parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.schema.Table;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.iapi.types.TypeId;
import org.apache.derby.impl.sql.compile.AllResultColumn;
import org.apache.derby.impl.sql.compile.AndNode;
import org.apache.derby.impl.sql.compile.BinaryOperatorNode;
import org.apache.derby.impl.sql.compile.FromBaseTable;
import org.apache.derby.impl.sql.compile.FromList;
import org.apache.derby.impl.sql.compile.FromSubquery;
import org.apache.derby.impl.sql.compile.HalfOuterJoinNode;
import org.apache.derby.impl.sql.compile.JoinNode;
import org.apache.derby.impl.sql.compile.NotNode;
import org.apache.derby.impl.sql.compile.OrNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;
import org.apache.derby.impl.sql.compile.ResultColumn;
import org.apache.derby.impl.sql.compile.ResultColumnList;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.SubqueryList;
import org.apache.derby.impl.sql.compile.SubqueryNode;
import org.apache.derby.impl.sql.compile.ValueNode;

import parsing.QueryParser;
import parsing.Conjunct;
import parsing.FromListElement;
import parsing.JoinTreeNode;
import parsing.Node;

public class ProcessResultSetNode {

	public static void processResultSetNodeJSQL(Statement stmt, boolean debug,parsing.QueryParser qParser){

		Select selectStatement = (Select) stmt;
		PlainSelect ps = (PlainSelect)selectStatement.getSelectBody();
		TablesNamesFinder tnf = new TablesNamesFinder();
		tnf.makeLists(selectStatement);

		//going through the joins in the select statement
		FromItem firstElem = ps.getFromItem();
		
		qParser.queryAliases = new FromListElement();
		qParser.queryAliases.setAliasName("Q");
		qParser.queryAliases.setTableName(null);

		qParser.topLevelRelation = generateRelationHierarchy(rsNode);//FIXME adapt to jsql
		
		//JoinTree
		qParser.root = new JoinTreeNode();
		Vector<Node> JoinConditions = new Vector<Node>(); 
		Vector<FromListElement> t = new Vector<FromListElement>();
		//assuming that 1st Element can't be join node
		if (firstElem instanceof net.sf.jsqlparser.schema.Table){
			//its a table. call operate on base table
			FromListElement temp = OperateOnBaseTable.OperateOnBaseTableJSQL((Table)firstElem,false, "", qParser.root,qParser, false, false);
			t.add(temp);
		} else if (firstElem instanceof SubSelect){
			//its a subquery
			FromListElement temp =	OperateOnSubQuery.OperateOnSubquery((FromSubquery) fromTableList.get(j),qParser.allConds, qParser.root,true,false,qParser);
			t.add(temp);
		}
		List<net.sf.jsqlparser.statement.select.Join> joinList = ps.getJoins();
		//as the jsql join doesn't provide the left element so storing the left item in a variable
		FromItem leftElem = firstElem;
		for (int i=0; i<joinList.size(); i++){
			FromListElement temp = new FromListElement();
			temp = OperateOnJoin.OperateOnJoinNodeJSQL(joinNode, "", JoinConditions, qParser.root, false, false, qParser);
			t.add(temp);
		}
		qParser.queryAliases.setTabs(t);
		
		
		
	}
	private void processProjectedCols(List<SelectItem> projCols, QueryParser qParser)throws Exception{
		//Add projected columns
		//ResultColumnList rcList = rsNode.getResultColumns();
		Vector<Node> tempProjectedCols = new Vector<Node>();
		for (int k=0;k<projCols.size();k++) {

			/*if (rcList.getNodeVector().get(k) instanceof AllResultColumn) {
				tempProjectedCols.addAll(Util.addAllProjectedColumns(qParser.queryAliases,0,qParser));
			}*/
			SelectItem col = projCols.get(k);

			if (col instanceof AllColumns){
				tempProjectedCols.addAll(Util.addAllProjectedColumns(qParser.queryAliases,0,qParser));
			} else if (col instanceof SelectExpressionItem) {
				tempProjectedCols.add(WhereClauseVector.getWhereClauseVectorJSQL(((SelectExpressionItem) col).getExpression(),((SelectExpressionItem) col).getAlias(), qParser.queryAliases,false,0,qParser));
			}
			/*else if (rcList.getNodeVector().get(k) instanceof ResultColumn){
				ResultColumn rc = (ResultColumn)rcList.getNodeVector().get(k);
				ValueNode exp = rc.getExpression();

				//projectedCols.add(getWhereClauseVector(exp, queryAliases,false));
				tempProjectedCols.add(WhereClauseVector.getWhereClauseVector(exp,rc.getName(), qParser.queryAliases,false,0,qParser));


				if(qParser.isUpdateNode){
					qParser.updateColumn.add(WhereClauseVector.getWhereClauseVector(rc.getReference(),rc.getName(), qParser.queryAliases,false,0,qParser));
				}


			}*/
		}
	}
	public static void processResultSetNode (ResultSetNode rsNode,boolean debug,parsing.QueryParser qParser) throws Exception{


		if (debug)
			System.out.println("\nOriginal Query : "+ qParser.getQuery().getQueryString());
		System.out.println();
		FromList fromList = rsNode.getFromList();
		Vector<QueryTreeNode> fromTableList = fromList.getNodeVector(); // this
		// is
		// coming
		// from
		// derby
		// parser


		fromTableList.get(0).printSubNodes(2);

		/*
		 * The fromTableList can contain nodes which are join nodes (or other
		 * complex nodes - as written by apache derby parser) so need to go
		 * through the list to identify individual tables.
		 */

		qParser.queryAliases = new FromListElement();
		qParser.queryAliases.setAliasName("Q");
		qParser.queryAliases.setTableName(null);

		qParser.topLevelRelation = generateRelationHierarchy(rsNode);

		//JoinTree
		qParser.root = new JoinTreeNode();
		Vector<Node> JoinConditions = new Vector<Node>(); 
		Vector<FromListElement> t = new Vector<FromListElement>();
		for (int j = 0; j < fromTableList.size(); j++) {
			if (fromTableList.get(j) instanceof FromBaseTable) {
				FromListElement temp = OperateOnBaseTable.OperateOnBaseTable((FromBaseTable) fromTableList.get(j),false, "", qParser.root,qParser, false, false);
				t.add(temp);

			} else if (fromTableList.get(j) instanceof JoinNode) {
				FromListElement temp = new FromListElement();
				temp = OperateOnJoin.OperateOnJoinNode((JoinNode) fromTableList.get(j), "",JoinConditions, qParser.root, false, false,qParser);
				t.add(temp);
			} else if (fromTableList.get(j) instanceof FromSubquery) {
				FromListElement temp =	OperateOnSubQuery.OperateOnSubquery((FromSubquery) fromTableList.get(j),qParser.allConds, qParser.root,true,false,qParser);
				t.add(temp);
			}
		}

		qParser.queryAliases.setTabs(t);
					

		// Geting SelectionClause and JoinClause equalities
		ValueNode whereClause = ((SelectNode) rsNode).getWhereClause();





		if (whereClause != null) {
			Node whereClausePred = new Node();
			whereClausePred = WhereClauseVector.getWhereClauseVector(whereClause,null, qParser.queryAliases,true,0,qParser);

			//FIXME: Mahesh Add to sub query if subquery references
			//whereClausePred = modifyNode(whereClausePred);

			if( whereClausePred != null) qParser.allConds.add(whereClausePred);
			//Add join condition
			if(!JoinConditions.isEmpty()){
				Vector<Node> allCondsDups=(Vector<Node>) qParser.allConds.clone();
				for (Node cond : qParser.allConds){
					Node NewCond = new Node();
					NewCond.setType(Node.getAndNodeType());
					NewCond.setLeft(JoinConditions.get(0));
					NewCond.setRight(cond);
					allCondsDups.remove(cond);
					allCondsDups.add(NewCond);
				}
				qParser.allConds.removeAllElements();
				qParser.allConds.addAll(allCondsDups);
			}

		}
		else if(!JoinConditions.isEmpty()){
			if(!qParser.allConds.isEmpty()) {
				Node n1= JoinConditions.get(0);
				Node n2=qParser.allConds.get(0);
				Node n=new Node();
				n.setLeft(n1);
				n.setRight(n2);
				n.setType(Node.getAndNodeType());
				qParser.allConds.removeAllElements();
				qParser.allConds.add(n);
			} else {
			 qParser.allConds.addAll(JoinConditions);
			}
		}

		/*for(int i=0;i<allConds.size();i++){
			System.out.println("WHERE CLAUSE NODE ==== >     "+(Node)allConds.get(i));
		}*/

		modifyTreeForCompareSubQ(qParser);

		// Getting EquivalenyClass Elements
		//EquivalenceClass.makeEquivalenceClasses(qParser);//Method to get equivalence classes for outer query block and each sub query

		QueryParser.flattenAndSeparateAllConds(qParser);
		for(Conjunct conjunct:qParser.conjuncts){
			conjunct.createEqClass();
		}
		for(parsing.QueryParser qp: qParser.getFromClauseSubqueries()){//For From clause subqueries

			QueryParser.flattenAndSeparateAllConds(qp);
			for(Conjunct conjunct:qp.conjuncts){
				conjunct.createEqClass();
			}
		}
		for(parsing.QueryParser qp: qParser.getWhereClauseSubqueries()){//For Where clause subqueries

			QueryParser.flattenAndSeparateAllConds(qp);
			for(Conjunct conjunct:qp.conjuncts){
				conjunct.createEqClass();
			}
		}
		//qParser.EqClass.addAll(EquivalenceClass.createEqClass(qParser));

		// Getting Foreign Key into a vector of JoinClauseInfo object
		Util.foreignKeyClosure(qParser);

		if(qParser.isDeleteNode){
			return;
		}

		// Get Projected Columns in a list Vector<Node> projectionList: one
		// element for each expression in the projection list



		WhereClauseVector.getAggregationDataStructures(rsNode, qParser.queryAliases, qParser.getQuery().getFromTables(),false,false, qParser);



		if (debug) {
			System.out.println("\nJoin Tables : " + qParser.getQuery().getJoinTables());
			System.out.println("Undirected Join Graph : "
					+ qParser.getQuery().getJoinGraph());
		}

		//Add projected columns
		ResultColumnList rcList = rsNode.getResultColumns();
		Vector<Node> tempProjectedCols = new Vector<Node>();
		for (int k=0;k<rcList.size();k++) {

			if (rcList.getNodeVector().get(k) instanceof AllResultColumn) {
				tempProjectedCols.addAll(Util.addAllProjectedColumns(qParser.queryAliases,0,qParser));
			}
			else if (rcList.getNodeVector().get(k) instanceof ResultColumn){
				ResultColumn rc = (ResultColumn)rcList.getNodeVector().get(k);
				ValueNode exp = rc.getExpression();

				//projectedCols.add(getWhereClauseVector(exp, queryAliases,false));
				tempProjectedCols.add(WhereClauseVector.getWhereClauseVector(exp,rc.getName(), qParser.queryAliases,false,0,qParser));


				if(qParser.isUpdateNode){
					qParser.updateColumn.add(WhereClauseVector.getWhereClauseVector(rc.getReference(),rc.getName(), qParser.queryAliases,false,0,qParser));
				}


			}
		}
		//ProjectedColumns.add(tempProjectedCols);
		qParser.projectedCols.addAll(tempProjectedCols);
	}

	private static RelationHierarchyNode generateRelationHierarchy(QueryTreeNode rs) throws StandardException{

		RelationHierarchyNode node = null;

		if(rs instanceof FromBaseTable){
			node = new RelationHierarchyNode(((FromBaseTable) rs).getBaseTableName());
		}
		else if(rs instanceof HalfOuterJoinNode){
			HalfOuterJoinNode joinNode = (HalfOuterJoinNode)rs;
			boolean isRightOuter = joinNode.isRightOuterJoin();

			RelationHierarchyNode l = generateRelationHierarchy(joinNode.getLeftResultSet());
			RelationHierarchyNode r = generateRelationHierarchy(joinNode.getRightResultSet());
			if(isRightOuter){
				node = new RelationHierarchyNode("_RIGHT_JOIN_", l, r);
			}
			else{
				node = new RelationHierarchyNode("_LEFT_JOIN_", l, r);
			}
		}
		else if(rs instanceof JoinNode){
			JoinNode joinNode = (JoinNode)rs;

			RelationHierarchyNode l = generateRelationHierarchy(joinNode.getLeftResultSet());
			RelationHierarchyNode r = generateRelationHierarchy(joinNode.getRightResultSet());
			node = new RelationHierarchyNode("_JOIN_", l, r);
		}
		else if(rs instanceof SelectNode){

			SelectNode selectNode = (SelectNode) rs;
			Vector<QueryTreeNode> v = selectNode.getFromList().getNodeVector();

			for(QueryTreeNode i : v){
				node = generateRelationHierarchy(i);
			}

			List<RelationHierarchyNode> l = new ArrayList<RelationHierarchyNode>();
			ValueNode w = selectNode.getWhereClause();
			getAllNotExistsQueries(w, l);
			if(node!=null) {
				node.setNotExistsSubQueries(l);
			}
		}

		return node;
	}

	private static void getAllNotExistsQueries(ValueNode w, List<RelationHierarchyNode> l) throws StandardException{
		if(w instanceof BinaryOperatorNode){
			BinaryOperatorNode whereNode = (BinaryOperatorNode) w;
			getAllNotExistsQueries(whereNode.getRightOperand(), l);
			getAllNotExistsQueries(whereNode.getLeftOperand(), l);
		}
		else if(w instanceof NotNode){
			NotNode notOp = (NotNode) w;
			ValueNode temp = notOp.getOperand();
			if(temp instanceof SubqueryNode){
				SubqueryNode subQueryNode = (SubqueryNode)temp;
				int subQueryType = subQueryNode.getSubqueryType();
				if(subQueryType ==16 ||subQueryType ==15 ){ //EXISTS Type
					QueryTreeNode resultSet = subQueryNode.getResultSet();
					l.add(generateRelationHierarchy(resultSet));
				}
			}
		}
	}

	public static void modifyTreeForCompareSubQ(parsing.QueryParser qParser) {

		for (Node n: qParser.allConds)  // This is not only for outer block 
			Util.modifyTreeForComapreSubQ(n);

		for(parsing.QueryParser qp: qParser.getFromClauseSubqueries()){//For From clause subqueries
			for(Node n: qp.allConds)
				Util.modifyTreeForComapreSubQ(n);
		}

		for(parsing.QueryParser qp: qParser.getWhereClauseSubqueries()){//For where clause subqueries
			for(Node n: qp.allConds)
				Util.modifyTreeForComapreSubQ(n);
		}
	}
}
