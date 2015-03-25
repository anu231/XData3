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
	
	private void debug(String s){
		if (output){
			System.out.println(s);
		}
	}
	
	public ConstructXDataTree(){
		qp = new QueryParser(TableMap.getInstances());
	}
	
	public void parseTree(Select stmt){
		stmt.getSelectBody().accept(this);
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
				join.getRightItem().accept(this);
				currentRoot = oldRoot2;
			}
		}
		if (plainSelect.getWhere() != null){
			//where clause processing
			whereClauseParseStart = true;
			debug(plainSelect.getWhere().toString());
			plainSelect.getWhere().accept(this);
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
		String tableWholeName = tableName.getWholeTableName();
		
		JoinTreeNode jtn = new JoinTreeNode();
		
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName.getName());
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(tableName.getAlias());
		
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
	
	public void visit(Expression exp){
		System.out.print("Expression:"+exp.toString());
		if (whereClauseParseStart){
			whereClauseParseStart = false;
			//where clause parsing begins
			//TODO parse where
		}
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