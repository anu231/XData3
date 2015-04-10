package parsing;

import java.io.StringReader;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.*;

import util.TableMap;

public class ParseSQL {

	private static Query query;
	private static CCJSqlParserManager pm;
	private static Statement stmt;
	private static TablesNamesFinder tnf;
	private static QueryParser root;
	/**
	 * Description Constructs xdata based tree from JSQL
	 * @param stmt
	 */
	private static void constructXDataTree(Select stmt){
		/*root = new QueryParser(TableMap.getInstances());
		//FIXME add the where clause
		tnf = new TablesNamesFinder();
		tnf.createTree(root,stmt);*/
		
		ConstructXDataTree cxt = new ConstructXDataTree();
		cxt.parseTree(stmt);
	}

	private static void parseQuery(String q, String qid) throws JSQLParserException{
		//int qid = 1;
		query = new Query(qid,q);
		pm = new CCJSqlParserManager();
		stmt = pm.parse(new StringReader(query.getQueryString()));
		System.out.println(q);
		if (stmt instanceof Select){
			//get the from list without going into subquery
			Select selectStatement = (Select) stmt;
			
			constructXDataTree(selectStatement);
			
			/*PlainSelect ps = (PlainSelect)selectStatement.getSelectBody();
			tnf.makeLists(selectStatement);
			List<Join> jL = ps.getJoins();
			for (int i=0; i<jL.size(); i++){
				System.out.println("Join List:"+jL.get(i).toString());
			}
			List<FromItem> fromList = tnf.getFromList();
			List<SelectItem> projCols = tnf.getProjectedList();
			for (Iterator iter = fromList.iterator(); iter.hasNext();) {
				FromItem fromItem = (FromItem)iter.next();
				//net.sf.jsqlparser.schema.Table fromItem = (net.sf.jsqlparser.schema.Table)iter.next();
				System.out.println("From Item:"+fromItem.getClass());
			}
			System.out.println("Projected Cols:");
			for (Iterator<SelectItem> iter = projCols.iterator(); iter.hasNext();) {
				SelectItem col = iter.next(); 
				if ( col instanceof AllColumns) {
					System.out.println("aal columns");
				} else if (col instanceof SelectExpressionItem){
					Expression exp = ((SelectExpressionItem) col).getExpression();
					if (exp instanceof Function){
						System.out.println("Function :"+((Function) exp).getName());
						System.out.println(((Function) exp).getParameters().toString());
					}
					if (exp instanceof LongValue){
						System.out.println("Long val:"+((LongValue) exp).getValue());
					} else if (exp instanceof StringValue){
						System.out.println("String :"+((StringValue) exp).getValue());
					}

				} 
				if (col instanceof BinaryExpression){
					BinaryExpression bne = (BinaryExpression) col;
					System.out.println("Binary:"+bne.getStringExpression());
				}

				//String fromItem = iter.next().toString();
				//System.out.println(col.toString());
			}
			Node root = null;//tnf.getRootNode();
			while (root.left!=null){
				System.out.println(root.getName());
				root = root.getLeft();
			}
			/*Expression where = tnf.getWhereClause();
			System.out.println("where class:"+where.getClass());
			if (where instanceof OrExpression){
				BinaryExpression bne = (BinaryExpression) where;
				System.out.println("Binary:"+bne.getStringExpression());
				Expression lt = bne.getLeftExpression();
				System.out.println("left:"+lt.getClass());
				EqualsTo leftExp = (EqualsTo) lt;
				Expression lhs = leftExp.getLeftExpression();
				System.out.println("LHS Class:"+lhs.getClass());
				
			} else if (where instanceof Between){
				//System.out.println("Left class:"+((Between) where).getRightExpression().getClass());
				//BinaryExpression bne = (BinaryExpression)((BinaryExpression) where).getRightExpression();
				//System.out.println(bne.getStringExpression());
			}*/
		}
	}

	public static void main(String[] args)throws JSQLParserException {
		// TODO Auto-generated method stub
		//String query = "Select id1 as id,4,count(distinct(a)),'asd' from moodle as m left outer join highland as h,jurassic as h where moodle.id between 2 and 3";
		//String query = "Select * from (select * from a left outer join b using (id)) x left outer join (select * from c left outer join d using (id)) y using (id)";
		String query = "select * from a join b using (id) ";
		tnf = new TablesNamesFinder();
		parseQuery(query,"1");
		//CCJSqlParserManager pm = new CCJSqlParserManager(); 
		//Statement stmt = pm.parse(new StringReader(query));
		/*if (stmt instanceof Select) {
			Select selectStatement = (Select) stmt;
			PlainSelect ps = (PlainSelect)selectStatement.getSelectBody();
			List<SelectItem> l = ps.getSelectItems();
			//for ()
			System.out.println("from item :"+ps.getFromItem().toString());
			List<Join> j = ps.getJoins();
			System.out.println("right itms:");
			for (int i=0; i<j.size(); i++){
				Join temp = (Join)j.get(i);
				System.out.println(temp.getRightItem());
			}

			TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
			List tableList = tablesNamesFinder.getTableList(selectStatement);
			for (Iterator iter = tableList.iterator(); iter.hasNext();) {
				String tableName = iter.next().toString();
				System.out.println(tableName);
			}
			List tableAliasList = tablesNamesFinder.getTableAlias(selectStatement);
			for (Iterator iter = tableAliasList.iterator(); iter.hasNext();) {
				String tableName = iter.next().toString();
				System.out.println(tableName);
			}
		}*/

	}

}
