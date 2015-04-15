package parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

public class ConstructXDataTree implements SelectVisitor, FromItemVisitor, JoinVisitor, ExpressionVisitor, ItemsListVisitor {
	private List tables;
	private QueryParser qp;
	private JoinTreeNode currentRoot;
	private Node whereClause;
	private boolean whereClauseParseStart = false;
	private boolean output = true;
	private Node currentNode;
	private boolean nextNodeLeft = true;
	private boolean joinWhere = false;
	
	private void debug(String s){
		if (output){
			System.out.println(s);
		}
	}
	
	public ConstructXDataTree(){
		//FIXME : qp = new QueryParser(TableMap.getInstances()); it takes a lot of time so initiating with null value
		qp = new QueryParser(null);
	}
	
	public void parseTree(Select stmt){
		stmt.getSelectBody().accept(this);
	}
	/*
	 * Description : displays the QueryParser Tree in form of a pretty print tree with proper indentation
	 */
	public void displayTree(){
		recursiveDispJTN(qp.root,1);
		//now where clause
		//debug("where clause :");
	}
	/*
	 * Description recursively displays the jtn
	 */
	private void recursiveDispJTN(JoinTreeNode j, int depth){
		if (j.getLeft()==null && j.getRight()==null){
			debug(insertSpace(depth)+"Table :"+j.getRelName());
		}
		if (j.getLeft()!=null){
			recursiveDispJTN(j.getLeft(), depth+1);
		} 
		debug(insertSpace(depth)+"Join Node On Exp :"+j.getJoinPred().toString());
		if (j.getRight()!=null){
			recursiveDispJTN(j.getRight(), depth+1);
		}
	}
	
	private String insertSpace(int d){
		String s="";
		for (int i=0; i<d; i++){
			s +=" ";
		}
		return s;
	}
	
	public List getTableList(Select select) {
		tables = new ArrayList();
		select.getSelectBody().accept(this);
		return tables;
	}
	/*
	 * Description Initiates the where clause tree
	 */
	private void initWhere(){
		
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
			}
		}
		
		plainSelect.getFromItem().accept(this);
		JoinTreeNode firstElem = null;
		if (currentRoot.getLeft()!=null){
			firstElem = currentRoot.getLeft();
		} else if (currentRoot.getRight()!=null){
			firstElem = currentRoot.getRight();
		}
		Boolean firstJoin = true;
		JoinTreeNode prevElem = null;
		if (plainSelect.getJoins() != null) {
			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				Join join = (Join) joinsIt.next();
				JoinTreeNode joinNode = new JoinTreeNode();
				if (firstJoin){
					//this is the first join
					joinNode.setLeft(firstElem);
					firstJoin = false;
				} else {
					joinNode.setLeft(prevElem);
				}
				
				JoinTreeNode oldRoot2 = currentRoot;
				currentRoot = joinNode;
				
				try {
					//getting the ON Expression
					//creating a where node out of the OnExpression
					debug("using columns :"+join.getOnExpression());
					whereClauseParseStart = true;
					joinWhere = true;
					join.getOnExpression().accept(this);
					addWhereClause();
				} catch(Exception e){
					debug("Exception :"+e.toString());
				}
				try {
					debug("using columns :"+join.getUsingColumns().toString());
				} catch(Exception e){
					debug("Exception :"+e.toString());
				}
				
				
				join.getRightItem().accept(this);
				currentRoot = oldRoot2;
			}
		}
		if (plainSelect.getWhere() != null){
			//where clause processing
			whereClauseParseStart = true;
			debug("where clause :"+plainSelect.getWhere().toString());
			debug("where class:"+plainSelect.getWhere().getClass());
			plainSelect.getWhere().accept(this);
			addWhereClause();
		} else {
			debug("no where");
		}
		
		if (parseStart){
			//do nothing
		} else {
			currentRoot = oldRoot;
		}
	}

	public void visit(Union union) {
		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			PlainSelect plainSelect = (PlainSelect) iter.next();
			visit(plainSelect);
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
	}

	public void visit(SubSelect subSelect) {
		//subquery
		JoinTreeNode jtn = new JoinTreeNode();
		JoinTreeNode oldRoot = currentRoot;
		if (currentRoot.getLeft()==null){
			currentRoot.setLeft(jtn);
		} else if (currentRoot.getRight()==null){
			currentRoot.setRight(jtn);
		} else {
			//FIXME
			//throw error
		}
		currentRoot = jtn;
		jtn.setNodeAlias(subSelect.getAlias());
		subSelect.getSelectBody().accept(this);
		currentRoot = oldRoot;
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
		addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(equalsTo);
		currentNode = oldNode;
	}

	public void visit(Function function) {
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
		addOnCurrentNode(tempNode);
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
		addOnCurrentNode(tempNode);
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
	}

	public void visit(JdbcParameter jdbcParameter) {
	}

	public void visit(LikeExpression likeExpression) {
		visitBinaryExpression(likeExpression);
	}

	public void visit(ExistsExpression existsExpression) {
		existsExpression.getRightExpression().accept(this);
	}

	public void visit(LongValue longValue) {
		debug("long val:"+longValue.toString());
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
		addOnCurrentNode(tempNode);
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
		addOnCurrentNode(tempNode);
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
		addOnCurrentNode(tempNode);
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
		addOnCurrentNode(tempNode);
		Node oldNode = currentNode;
		currentNode = tempNode;
		visitBinaryExpression(orExpression);
		currentNode = oldNode;
	}

	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
	}

	public void visit(StringValue stringValue) {
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

}