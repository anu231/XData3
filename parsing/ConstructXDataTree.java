package parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import util.TableMap;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

public class ConstructXDataTree implements SelectVisitor, FromItemVisitor, JoinVisitor, ExpressionVisitor, ItemsListVisitor, SelectItemVisitor {
	private List tables;
	private QueryParser qp;
	public QueryParser origQP;
	private JoinTreeNode currentRoot;
	private Node whereClause;
	private boolean whereClauseParseStart = false;
	private boolean fromClauseSubQuery = false;
	private boolean whereClauseSubQuery = false;
	private boolean output = true;
	private Node currentNode;
	private boolean nextNodeLeft = true;
	private boolean joinWhere = false;
	private boolean visitRightJoinElem = false;
	private boolean subQparse = false;
	Vector<FromListElement> t = new Vector<FromListElement>();
	private boolean visitingProj = false;
	private Vector<Node> projectedCols;
	private Node currProjNode;
	
	private void debug(String s){
		if (output){
			System.out.println(s);
		}
	}
	
	public ConstructXDataTree(QueryParser q){
		//FIXME : qp = new QueryParser(TableMap.getInstances()); it takes a lot of time so initiating with null value
		qp = q;
		origQP = qp;
	}
	
	public void parseTree(Select stmt,String q,String qid){
		Query qObj = new Query(qid,q);
		qp.setQuery(qObj);
		stmt.getSelectBody().accept(this);
	}
	/*
	 * Description : displays the QueryParser Tree in form of a pretty print tree with proper indentation
	 */
	public void displayTree(){
		recursiveDispJTN(origQP.root,1);
	}
	/*
	 * description displays the node recursively
	 */
	private void recursiveDispNode(Node n,int depth){
		if (n.getLeft()!=null){
			recursiveDispNode(n.getLeft(), depth+1);
		}
		debug(insertSpace(depth)+"Node :"+n.getNodeType()+" ");
		if (n.getNodeType().contains(Node.getColRefType())) {
			debug(insertSpace(depth)+"Node :"+n.getTable().getTableName()+"."+n.getColumn().getColumnName());
		}
		if (n.getRight()!=null){
			recursiveDispNode(n.getRight(), depth+1);
		}
	}
	/*
	 * Description recursively displays the jtn
	 */
	private void recursiveDispJTN(JoinTreeNode j, int depth){
		if (j.getLeft()==null && j.getRight()==null){
			debug(insertSpace(depth)+"Table :"+j.getRelName()+"\t alias :"+j.getNodeAlias());
			debug(insertSpace(depth)+"JTN TYpe :"+j.getNodeType());
			return;
		}
		if (j.getLeft()!=null){
			recursiveDispJTN(j.getLeft(), depth+1);
		} 
		debug(insertSpace(depth)+"JTN Type :"+j.getNodeType());
		debug(insertSpace(depth)+"Table :"+j.getRelName()+"\t alias :"+j.getNodeAlias());
		if (j.getJoinPred()!=null){
			//debug(insertSpace(depth)+"Join Node On Exp :"+j.getJoinPred().toString());
			for (int i=0; i<j.getJoinPred().size(); i++){
				recursiveDispNode(j.getJoinPred().elementAt(i), depth+1);
			}
		}
		if (j.getRight()!=null){
			recursiveDispJTN(j.getRight(), depth+1);
		}
	}
	
	private String insertSpace(int d){
		String s="";
		String sep = "-";
		for (int i=0; i<d; i++){
			s +=sep;
		}
		return s;
	}
	
	public List getTableList(Select select) {
		tables = new ArrayList();
		select.getSelectBody().accept(this);
		return tables;
	}
	
	public void visit(PlainSelect plainSelect) {
		Boolean parseStart = false;//checks whether this is the base statement of the query
		JoinTreeNode oldRoot = currentRoot;
		if (qp.root==null){
			parseStart = true;
			qp.root = new JoinTreeNode();
			currentRoot = qp.root;
		} else {
			currentRoot = new JoinTreeNode();
			if (oldRoot.getLeft()==null){
				oldRoot.setLeft(currentRoot);
			} else if (oldRoot.getRight()==null){
				oldRoot.setRight(currentRoot);
			} else {
				debug("ERROR in adding PlainSelect to tree. No spcae.");
			}
		}
		/*
		 * Parsing the projected cols
		 */
		List<SelectItem> projList = plainSelect.getSelectItems();
		visitingProj = true;
		projectedCols = new Vector<Node>();
		for (int i=0; i<projList.size(); i++){
			SelectItem temp = (SelectItem)projList.get(i);
			temp.accept(this);
		}
		qp.projectedCols.addAll(projectedCols);
		visitingProj = false;
		
		//getting the groupby list
		List<Column> groupBy = plainSelect.getGroupByColumnReferences();
		Vector<Node> groupByNodes = new Vector<Node>();
		if (groupBy!=null){
			for (int i=0; i<groupBy.size(); i++){
				Column temp = groupBy.get(i);
				Node gnNode = new Node();
				gnNode.setColumn(new parsing.Column(temp.getColumnName(),temp.getTable().getName()));
				gnNode.setTable(new parsing.Table(temp.getTable().getName()));
				//debug(temp.getClass().getName());
				groupByNodes.add(gnNode);
			}
			qp.setGroupByNodes(groupByNodes);
		}
		
		Boolean firstJoin = true;
		JoinTreeNode prevElem = null;
		JoinTreeNode parentJTN = currentRoot;
		Vector<FromListElement> oldT = t;
		t = new Vector<FromListElement>();
		if (plainSelect.getJoins() != null) {
			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				Join join = (Join) joinsIt.next();
				JoinTreeNode joinNode=null;	
				if (firstJoin){
					//this is the first join
					//make this the base root
					//this will be the base JTN for this query
					joinNode = currentRoot;					
					plainSelect.getFromItem().accept(this);
					firstJoin = false;
				} else {
					joinNode = new JoinTreeNode();
					addOnCurrentJTN(joinNode);
				}
				//now this joinNode will become the current root
				currentRoot = joinNode;
				joinNode.setNodeType(getJoinType(join));
				try {
					//getting the ON Expression
					//creating a where node out of the OnExpression
					//debug("using columns :"+join.getOnExpression());
					if (join.getOnExpression()!=null){
						whereClauseParseStart = true;
						joinWhere = true;
						join.getOnExpression().accept(this);
						addWhereClause();
						//add the where caluse to all conds in querpaprser
						qp.allConds.add(whereClause);
					}
					
				} catch(Exception e){
					debug("Exception1 :"+e.toString());
				}
				try {
					if (join.getUsingColumns()!=null){
						debug("using columns :"+join.getUsingColumns().toString());
					}
					
				} catch(Exception e){
					debug("Exception2 :"+e.toString());
				}
				join.getRightItem().accept(this);
				currentRoot = joinNode.getRight();
			}
			if (t!=null){
				qp.queryAliases = new FromListElement();
				qp.queryAliases.setAliasName("Q");
				qp.queryAliases.setTableName(null);
				qp.queryAliases.setTabs(t);
			}
			
			t = oldT;
			currentRoot = parentJTN;
		}
		if (plainSelect.getWhere() != null){
			//where clause processing
			whereClauseParseStart = true;
			debug("where clause :"+plainSelect.getWhere().toString());
			debug("where class:"+plainSelect.getWhere().getClass());
			plainSelect.getWhere().accept(this);
			whereClauseParseStart = false;
			addWhereClause();
			qp.allConds.add(whereClause);
		} else {
			debug("no where");
		}
		
		if (parseStart){
			//do nothing
		} else {
			currentRoot = oldRoot;
		}
	}

	private String getJoinType(Join j){
		if (j.isFull()){
			return JoinClauseInfo.fullOuterJoin;
		} else if (j.isInner()){
			return JoinClauseInfo.innerJoin;
		} else if (j.isLeft()){
			return JoinClauseInfo.leftOuterJoin;
		} else if (j.isRight()){
			return JoinClauseInfo.rightOuterJoin;
		} else {
			return "Unknown Join";
		}
	}
	
	public void visit(Union union) {
		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			qp.isUnion = true;
			
			PlainSelect plainSelect = (PlainSelect) iter.next();
			QueryParser temp = qp;
			qp = new QueryParser(TableMap.getInstances());
			qp.setQuery(new Query("1",plainSelect.toString()));
			if (temp.getLeftQuery()==null){
				temp.setLeftQuery(qp);
			} else if (temp.getRightQuery()==null){
				temp.setLeftQuery(qp);
			} else {
				debug("No Space to add the child to union");
			}
			visit(plainSelect);
			qp = temp;
		}
	}

	public void visit(Table tableName) {		
		JoinTreeNode jtn = new JoinTreeNode();	
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName.getName());
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(tableName.getAlias());
		
		//FIXME where to add this jtn to
		addOnCurrentJTN(jtn);
		
		/*
		 * Create FromListElement
		 */
		String tName = tableName.getName();
		String aliasName = tableName.getAlias();
		
		
		//create parsing.Table. From OperateOnBaseTable
		/*************************************************************/
		parsing.Table table = qp.getTableMap().getTable(tName);
		//Util.addFromTable(table,qp);
		
		if (aliasName != null) {
			qp.getQuery().putBaseRelation(aliasName, tName);
		} else {
			qp.getQuery().putBaseRelation(tName, tName);
		}
		if (qp.getQuery().getRepeatedRelationCount().get(tName) != null) {
			qp.getQuery().putRepeatedRelationCount(tName, qp.getQuery()
					.getRepeatedRelationCount().get(tName) + 1);
			//	query.putTableNameToQueryIndex(tableName +  (query.getRepeatedRelationCount().get(tableName)), queryType, queryIndex);
		} else {
			qp.getQuery().putRepeatedRelationCount(tName, 1);
			//query.putTableNameToQueryIndex(tableName +  "1", queryType, queryIndex);
		}
		qp.getQuery().putCurrentIndexCount(tName, qp.getQuery().getRepeatedRelationCount()
				.get(tName) - 1);
		/*************************************************************/
		String tableNameNo = tableName.getName()+ qp.getQuery().getRepeatedRelationCount().get(tName);
		
		FromListElement temp = new FromListElement();
		temp.setAliasName(aliasName);
		temp.setTableName(tName);
		
		temp.setTableNameNo(tableNameNo);
		temp.setTabs(null);
		
		Util.updateTableOccurrences(fromClauseSubQuery, whereClauseSubQuery, tableNameNo, qp);
		
		if (qp.getQuery().getCurrentIndex().get(tName) == null)
			qp.getQuery().putCurrentIndex(tName, 0);
		t.addElement(temp);
	}

	public void visit(SubSelect subSelect) {
		//subquery
		
		/*JoinTreeNode jtn = new JoinTreeNode();
		JoinTreeNode oldRoot = currentRoot;
		if (currentRoot.getLeft()==null){
			currentRoot.setLeft(jtn);
		} else if (currentRoot.getRight()==null){
			currentRoot.setRight(jtn);
		} else {
			debug("No place to add subselect");
		}
		currentRoot = jtn;
		jtn.setNodeAlias(subSelect.getAlias());
		subSelect.getSelectBody().accept(this);
		currentRoot = oldRoot;
		*/
		/*
		 * Adding it as another query parser instance
		 */
		if (whereClauseParseStart){
			whereClauseSubQuery = true;
		}
		parsing.QueryParser subQClause=new parsing.QueryParser(TableMap.getInstances());
		subQClause.setQuery(new Query("1",subSelect.toString()));
		String aliasName = subSelect.getAlias();
		subQClause.queryAliases=new FromListElement();
		subQClause.queryAliases.setAliasName(subSelect.getAlias());
		subQClause.queryAliases.setTableName(null);
		
		if(fromClauseSubQuery){
			qp.getFromClauseSubqueries().add(subQClause);
			qp.getSubQueryNames().put(aliasName, qp.getFromClauseSubqueries().size()-1);
		}
		if(whereClauseSubQuery){
			qp.getWhereClauseSubqueries().add(subQClause);
		}
		//if (fromClauseSubQuery || whereClauseSubQuery){
			subQparse = true;
			origQP = qp;
			qp = subQClause;
		//} else {
			//do nothing. its part of joins
		//}
		subSelect.getSelectBody().accept(this);
		/*if (!fromClauseSubQuery && !whereClauseSubQuery){
			//do nothing
		} else {*/
			
			subQparse = false;
			qp = origQP;
		//}
		
	}

	public void visit(Addition addition) {
		//create an addition node
		Node tempNode = new Node();
		tempNode.setType(Node.getBaoNodeType());
		tempNode.setOperator("+");
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(addition);
		currentNode = oldNode;
	}
	private void addOnCurrentJTN(JoinTreeNode j){
		if (currentRoot.getLeft()==null){
			currentRoot.setLeft(j);
		} else if (currentRoot.getRight()==null){
			currentRoot.setRight(j);
		} else {
			debug("ERROR in jtn insertion. No space on left/right");
		}
	}
	/*
	 * Description adds the provided node to the current node at right place i.e. left or right. 
	 * If currenntNode is not created. It creates it
	 */
	private void addOnCurrentNode(Node n){
		if (currentNode==null){
			currentNode = n;
		} else {
			if (nextNodeLeft && currentNode.getLeft()==null){
				currentNode.setLeft(n);
			} else if (!nextNodeLeft && currentNode.getRight()==null){
				currentNode.setRight(n);
			}  
			else {
				debug("error on child insertion");
			}
		}
	}
	/*
	 * Description - adds the where clause node to the query parser
	 */
	private void addWhereClause(){
		//adding only the where clause
		if (joinWhere) {
			//this where clause belongs to the join node
			//add it to current jtn
			currentRoot.addJoinPred(whereClause);
			joinWhere = false;
		} else {
			qp.allConds.add(whereClause);
		}
	}
	
	/*public void visit(SelectItem selectItem){
		
	}*/
	
	public void visit(AndExpression andExpression) {
		//create an and node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);	
		}
		tempNode.setType(Node.getAndNodeType());
		tempNode.setOperator("AND");
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(andExpression);
		currentNode = oldNode;
	}

	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

	public void visit(Column tableColumn) {
		Node tempNode = new Node();
		tempNode.setType(Node.getColRefType());
		tempNode.setColumn(new parsing.Column(tableColumn.getColumnName(),tableColumn.getTable().getName()));
		tempNode.setRight(null);
		tempNode.setLeft(null);
		tempNode.setTable(new parsing.Table(tableColumn.getTable().getWholeTableName()));
		addOnCurrentNode(tempNode);
	}

	public void visit(Division division) {
		//create a division node
		Node tempNode = new Node();
		tempNode.setType(Node.getBaoNodeType());
		tempNode.setOperator("/");
		addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(division);
		currentNode = oldNode;
	}

	public void visit(DoubleValue doubleValue) {
		Node tempNode = new Node();
		tempNode.setType(Node.getValType());
		tempNode.setStrConst(doubleValue.toString());
		addOnCurrentNode(tempNode);
	}

	public void visit(EqualsTo equalsTo) {
		//create a equalsTo node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
			
		}
		tempNode.setType(Node.getBroNodeType());
		tempNode.setOperator("=");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(equalsTo);
		currentNode = oldNode;
	}

	public void visit(Function function) {
		debug("Function:"+function.toString());
		AggregateFunction af = new AggregateFunction();
		af.setFunc(function.getName());
		af.setDistinct(function.isDistinct());
		af.setAggAliasName(function.getName());//FIXME how to get the alias
		ExpressionList expL = function.getParameters();
		List<Expression> exps = expL.getExpressions();
		for (int i=0; i<exps.size(); i++){
			exps.get(i).accept(this);
		}
	}

	public void visit(GreaterThan greaterThan) {
		//create a greaterThan node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
		}
		tempNode.setType(Node.getBroNodeType());
		tempNode.setOperator(">");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(greaterThan);
		currentNode = oldNode;
	}

	public void visit(GreaterThanEquals greaterThanEquals) {
		//create a greaterThanEquals node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
		}
		tempNode.setType(Node.getBroNodeType());
		tempNode.setOperator(">=");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(greaterThanEquals);
		currentNode = oldNode;
	}

	public void visit(InExpression inExpression) {
		inExpression.getLeftExpression().accept(this);
		inExpression.getItemsList().accept(this);
	}

	public void visit(InverseExpression inverseExpression) {
		inverseExpression.getExpression().accept(this);
	}

	public void visit(IsNullExpression isNullExpression) {
		Node tempNode = new Node();
		tempNode.setType(Node.getIsNullNodeType());
		tempNode.setOperator("=");
		tempNode.setRight(null);
		nextNodeLeft = true;
		Node oldNode = currentNode;
		currentNode = tempNode;
		isNullExpression.getLeftExpression().accept(this);
		currentNode = oldNode;
	}

	public void visit(JdbcParameter jdbcParameter) {
		Node tempNode = new Node();
		tempNode.setType(Node.getValType());
		tempNode.setStrConst("$"+qp.paramCount);
		qp.paramCount++;
		tempNode.setLeft(null);
		tempNode.setRight(null);
		addOnCurrentNode(tempNode);
	}

	public void visit(LikeExpression likeExpression) {
		Node tempNode = new Node();
		tempNode.setType(Node.getLikeNodeType());
		tempNode.setOperator("~");
		addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(likeExpression);
		currentNode = oldNode;
	}

	public void visit(ExistsExpression existsExpression) {
		existsExpression.getRightExpression().accept(this);
	}

	public void visit(LongValue longValue) {
		Node tempNode = new Node();
		tempNode.setType(Node.getValType());
		tempNode.setStrConst(longValue.toString());
		addOnCurrentNode(tempNode);
	}

	public void visit(MinorThan minorThan) {
		//create a minorThan node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
			
		}
		tempNode.setType(Node.getBroNodeType());
		tempNode.setOperator("<");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(minorThan);
		currentNode = oldNode;
	}

	public void visit(MinorThanEquals minorThanEquals) {
		//create a minorThanEquals node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
			
		}
		tempNode.setType(Node.getBroNodeType());
		tempNode.setOperator("<=");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(minorThanEquals);
		currentNode = oldNode;
	}

	public void visit(Multiplication multiplication) {
		//create an multiplication node
		Node tempNode = new Node();
		tempNode.setType(Node.getBaoNodeType());
		tempNode.setOperator("*");
		addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(multiplication);
		currentNode = oldNode;
	}

	public void visit(NotEqualsTo notEqualsTo) {
		//create a notEqualsTo node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
			
		}
		tempNode.setType(Node.getBroNodeType());
		tempNode.setOperator("/=");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(notEqualsTo);
		currentNode = oldNode;
	}

	public void visit(NullValue nullValue) {
	}

	public void visit(OrExpression orExpression) {
		//create a OR node
		Node tempNode = new Node();
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			whereClause = tempNode;
			currentNode = whereClause;
			//need to add the where clause to the tree who will be the parent of where clause
			//the where clause will be added to the queryparser
		} else {
			//where clause has already started so the current node will be populated
			addOnCurrentNode(tempNode);
			
		}
		tempNode.setType(Node.getOrNodeType());
		tempNode.setOperator("OR");
		//addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(orExpression);
		currentNode = oldNode;
	}

	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
	}

	public void visit(StringValue stringValue) {
		Node tempNode = new Node();
		tempNode.setType(Node.getValType());
		tempNode.setStrConst(stringValue.toString());
		addOnCurrentNode(tempNode);
	}

	public void visit(Subtraction subtraction) {
		//create an subtraction node
		Node tempNode = new Node();
		tempNode.setType(Node.getBaoNodeType());
		tempNode.setOperator("-");
		addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(subtraction);
		currentNode = oldNode;
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		nextNodeLeft = true;
		binaryExpression.getLeftExpression().accept(this);
		nextNodeLeft = false;
		binaryExpression.getRightExpression().accept(this);
	}
	
	public void visit(ExpressionList expressionList) {
		for (Iterator iter = expressionList.getExpressions().iterator(); iter.hasNext();) {
			Expression expression = (Expression) iter.next();
			expression.accept(this);
		}
	}

	public void visit(DateValue dateValue) {
	}
	
	public void visit(TimestampValue timestampValue) {
	}
	
	public void visit(TimeValue timeValue) {
	}

	public void visit(CaseExpression caseExpression) {
	}

	public void visit(WhenClause whenClause) {
	}

	public void visit(AllComparisonExpression allComparisonExpression) {
		allComparisonExpression.GetSubSelect().getSelectBody().accept(this);
	}

	public void visit(AnyComparisonExpression anyComparisonExpression) {
		anyComparisonExpression.GetSubSelect().getSelectBody().accept(this);
	}

	public void visit(SubJoin subjoin) {
		subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

	@Override
	public void visit(Concat arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Matches arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseAnd arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseOr arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BitwiseXor arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AllColumns arg0) {
		// TODO Auto-generated method stub
		/*
		 * all colimns as in *
		 */
		//projectedCols.addAll(Util.addAllProjectedColumns(qp.queryAliases,0,qp));
		debug("allcolumns :"+arg0.toString());
	}

	@Override
	public void visit(AllTableColumns arg0) {
		// TODO Auto-generated method stub
		//FIXME when does this get called
		debug("alltablecolumns :"+arg0.toString());
	}

	@Override
	public void visit(SelectExpressionItem selectExpItem) {
		// FIXME add alias of projected cols as well
		Node oldNode = currentNode;
		currentNode = null;
		//tempNode.setTableAlias(selectExpItem.getAlias());
		selectExpItem.getExpression().accept(this);
		projectedCols.add(currentNode);
		currentNode = oldNode;
		debug("selectexpitem :"+selectExpItem.toString());
	}

}