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
	public static ConstructXDataTree cxt;
	/**
	 * Description Constructs xdata based tree from JSQL
	 * @param stmt
	 */
	private static void constructXDataTree(Select stmt,String q,String qid,QueryParser qp){	
		cxt = new ConstructXDataTree(qp);
		cxt.parseTree(stmt,q,qid);
		System.out.println("Displaying Tree");
		cxt.displayTree();
	}

	public static void parseQuery(String q, String qid,QueryParser qp) throws JSQLParserException{
		//int qid = 1;
		query = new Query(qid,q);
		pm = new CCJSqlParserManager();
		stmt = pm.parse(new StringReader(query.getQueryString()));
		System.out.println(q);
		if (stmt instanceof Select){
			//get the from list without going into subquery
			Select selectStatement = (Select) stmt;
			constructXDataTree(selectStatement,q,qid,qp);		
			/******taken from process result set node
			 */
			ProcessResultSetNode.modifyTreeForCompareSubQ(qp);

			// Getting EquivalenyClass Elements
			//EquivalenceClass.makeEquivalenceClasses(qParser);//Method to get equivalence classes for outer query block and each sub query

			QueryParser.flattenAndSeparateAllConds(qp);
			for(Conjunct conjunct:qp.conjuncts){
				conjunct.createEqClass();
			}
			for(parsing.QueryParser qrp: qp.getFromClauseSubqueries()){//For From clause subqueries

				QueryParser.flattenAndSeparateAllConds(qrp);
				for(Conjunct conjunct:qrp.conjuncts){
					conjunct.createEqClass();
				}
			}
			for(parsing.QueryParser qrp: qp.getWhereClauseSubqueries()){//For Where clause subqueries

				QueryParser.flattenAndSeparateAllConds(qrp);
				for(Conjunct conjunct:qrp.conjuncts){
					conjunct.createEqClass();
				}
			}
			//qParser.EqClass.addAll(EquivalenceClass.createEqClass(qParser));

			// Getting Foreign Key into a vector of JoinClauseInfo object
			Util.foreignKeyClosure(qp);
			/*
			 * 
			 */
		}
	}

	public static void main(String[] args)throws JSQLParserException {
		// TODO Auto-generated method stub
		//String query = "Select id1 as id,4,count(distinct(a)),'asd' from moodle as m left outer join highland as h,jurassic as h where moodle.id between 2 and 3";
		//String query = "Select * from (select * from a left outer join b using (id)) x left outer join (select * from c left outer join d using (id)) y using (id)";
		String query = "Select * from (select * from a left outer join b on a.id=b.id) x left outer join (select * from c left outer join d on c.id=d.id) y on x.id=y.id group by x.a";
		//String query ="SELECT distinct dept_name FROM course WHERE credits IN (SELECT SUM(credits) FROM course NATURAL JOIN department WHERE  title='CS' GROUP BY dept_name, building HAVING COUNT(course_id)> 2)";
		//String query="SELECT t.semester, SUM(c.credits) FROM (select dep,budget,dept_name from department where valid=1 ) as d INNER JOIN teaches t  ON (d.budget = t.year + 4)  INNER JOIN course c ON (c.dept_name = d.dept_name)  GROUP BY t.semester HAVING AVG(c.credits) > 2 AND COUNT(d.building) = 2";
		//String query = "select * from a join b on a.id=b.id";
		//String query="SELECT c.dept_name, SUM(c.credits) FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name)  GROUP BY c.dept_name  HAVING SUM(c.credits) > 10 AND COUNT(c.credits) > 1";
		//String query="SELECT count(c.der) as d FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name)  GROUP BY c.dept_name  HAVING SUM(c.credits) > 10 AND COUNT(c.credits) > 1";
		//parseQuery(query,"1");
		
			}

}
