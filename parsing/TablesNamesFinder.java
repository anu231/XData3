package parsing;

import java.util.*;

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

public class TablesNamesFinder implements SelectVisitor, FromItemVisitor, ExpressionVisitor, ItemsListVisitor {
	private List tables;
	private List tableAlias;
	private List<FromItem> fromList;
	private Expression where;
	private List<SelectItem> projectedCols;
	public Node root=null;
	private Node currentRoot=null;

	public TablesNamesFinder(){
		//currentRoot = new Node("root");
		//root = currentRoot;
	}
	public Node getRootNode(){
		return currentRoot;
	}
	public List getTableList(Select select) {
		tables = new ArrayList();
		tableAlias = new ArrayList();
		select.getSelectBody().accept(this);
		return tables;
	}
	
	public void makeLists(Select select){
		fromList = new ArrayList();
		projectedCols = new ArrayList<SelectItem>();
		select.getSelectBody().accept(this);
	}
	
	public void makeLists(){
		fromList = new ArrayList();
		projectedCols = new ArrayList<SelectItem>();
	}
	public List<SelectItem> getProjectedList(){
		return projectedCols;
	}
	
	public List<FromItem> getFromList(){
		return fromList;
	}
	
	public List getJoinList(Select select){
		fromList = new ArrayList();
		select.getSelectBody().accept(this);
		return fromList;
	}
	public Expression getWhereClause(){
		return where;
	}
	public List getTableAlias(Select select) {
		return tableAlias;
	}
	public void visit(PlainSelect plainSelect) {
		plainSelect.getFromItem().accept(this);
		//////////////Creating Tree////////////////
		
		/*Node temp = new Node(plainSelect.toString());
		if (currentRoot ==null){
			root = temp;
			currentRoot = temp;
			temp.parent = null;
		} else {
			temp.parent = currentRoot;
		}*/
		//////////////////////////////////////////
		where = plainSelect.getWhere();
		projectedCols = plainSelect.getSelectItems();
		fromList.add(plainSelect.getFromItem());
		
		//getting the 1st element in the join list
		FromItem firstElem = plainSelect.getFromItem();
		Node leftfirstNode = new Node(firstElem.toString()); 
		System.out.println("PlainSelect:"+plainSelect.getFromItem()+",,,"+plainSelect.getFromItem().getClass());
		Node startRoot = currentRoot;
		boolean start=true;
		Node prevJoin = null;
		if (plainSelect.getJoins() != null) {
			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				Join join = (Join) joinsIt.next();
				//create a new node for this join
				Node joinNode = new Node(join.toString());
				if (start){
					//the iteration has started and the 1st element of this join is already been extracted
					start=false;
					joinNode.setLeft(leftfirstNode);
					prevJoin = joinNode;
					leftfirstNode.parent = currentRoot;
					currentRoot = leftfirstNode;
					firstElem.accept(this);
				} else {
					joinNode.setLeft(prevJoin);
					prevJoin = joinNode;
				}
				FromItem rightItem = join.getRightItem();
				Node rightNode = new Node(rightItem.toString());
				joinNode.setRight(rightNode);
				//System.out.println("JOIN:"+join.toString());
				//System.out.println("Join-right-item:"+join.getRightItem()+",,,"+join.getRightItem().getClass());
				fromList.add(join.getRightItem());
				//System.out.println("String:"+join.getRightItem().toString()+"::Class:"+join.getRightItem().getClass());
				join.getRightItem().accept(this);
			}
		} else {
			//there is no join in this select statement
			Node temp = new Node(plainSelect.toString());
			temp.parent = currentRoot;
			currentRoot.setLeft(temp);
		}
		if (plainSelect.getWhere() != null)
			plainSelect.getWhere().accept(this);
		if (currentRoot.parent!=null){
			currentRoot = currentRoot.parent;
		}
	}

	public void visit(Union union) {
		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			PlainSelect plainSelect = (PlainSelect) iter.next();
			visit(plainSelect);
		}
	}

	public void visit(Table tableName) {
		String tableWholeName = tableName.getWholeTableName();
		//tables.add(tableWholeName);
		//tableAlias.add(tableName.getAlias());
	}

	public void visit(SubSelect subSelect) {
		Node temp = new Node(subSelect.toString());
		temp.parent = currentRoot;
		currentRoot.setLeft(temp);
		currentRoot = temp;
		System.out.println("SubSelect:"+subSelect.toString());		
		subSelect.getSelectBody().accept(this);
		if (currentRoot.parent!=null){
			currentRoot= currentRoot.parent;
		}
	}

	public void visit(Addition addition) {
		visitBinaryExpression(addition);
	}

	public void visit(AndExpression andExpression) {
		visitBinaryExpression(andExpression);
	}

	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

	public void visit(Column tableColumn) {
	}

	public void visit(Division division) {
		visitBinaryExpression(division);
	}

	public void visit(DoubleValue doubleValue) {
	}

	public void visit(EqualsTo equalsTo) {
		visitBinaryExpression(equalsTo);
	}

	public void visit(Function function) {
	}

	public void visit(GreaterThan greaterThan) {
		visitBinaryExpression(greaterThan);
	}

	public void visit(GreaterThanEquals greaterThanEquals) {
		visitBinaryExpression(greaterThanEquals);
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
	}

	public void visit(MinorThan minorThan) {
		visitBinaryExpression(minorThan);
	}

	public void visit(MinorThanEquals minorThanEquals) {
		visitBinaryExpression(minorThanEquals);
	}

	public void visit(Multiplication multiplication) {
		visitBinaryExpression(multiplication);
	}

	public void visit(NotEqualsTo notEqualsTo) {
		visitBinaryExpression(notEqualsTo);
	}

	public void visit(NullValue nullValue) {
	}

	public void visit(OrExpression orExpression) {
		visitBinaryExpression(orExpression);
	}

	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
	}

	public void visit(StringValue stringValue) {
	}

	public void visit(Subtraction subtraction) {
		visitBinaryExpression(subtraction);
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		binaryExpression.getLeftExpression().accept(this);
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
		System.out.println("Subjoin:"+subjoin);
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

