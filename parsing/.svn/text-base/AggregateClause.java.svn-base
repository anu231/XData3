package parsing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.iapi.types.SQLDouble;
import org.apache.derby.iapi.types.SQLInteger;
import org.apache.derby.impl.sql.compile.AggregateNode;
import org.apache.derby.impl.sql.compile.AllResultColumn;
import org.apache.derby.impl.sql.compile.AndNode;
import org.apache.derby.impl.sql.compile.BinaryArithmeticOperatorNode;
import org.apache.derby.impl.sql.compile.BinaryOperatorNode;
import org.apache.derby.impl.sql.compile.CharConstantNode;
import org.apache.derby.impl.sql.compile.ColumnReference;
import org.apache.derby.impl.sql.compile.CursorNode;
import org.apache.derby.impl.sql.compile.FromBaseTable;
import org.apache.derby.impl.sql.compile.FromList;
import org.apache.derby.impl.sql.compile.GroupByColumn;
import org.apache.derby.impl.sql.compile.GroupByList;
import org.apache.derby.impl.sql.compile.HalfOuterJoinNode;
import org.apache.derby.impl.sql.compile.InListOperatorNode;
import org.apache.derby.impl.sql.compile.IsNullNode;
import org.apache.derby.impl.sql.compile.JoinNode;
import org.apache.derby.impl.sql.compile.LikeEscapeOperatorNode;
import org.apache.derby.impl.sql.compile.NotNode;
import org.apache.derby.impl.sql.compile.NumericConstantNode;
import org.apache.derby.impl.sql.compile.OrNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;
import org.apache.derby.impl.sql.compile.ResultColumn;
import org.apache.derby.impl.sql.compile.ResultColumnList;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SQLParser;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.SubqueryNode;
import org.apache.derby.impl.sql.compile.UnaryOperatorNode;
import org.apache.derby.impl.sql.compile.ValueNode;
import org.apache.derby.impl.sql.compile.ValueNodeList;

public class AggregateClause {

	private Vector<Aggregate> selectClause;
	private Vector<Aggregate> havingClause;
	
	public AggregateClause(){
		selectClause = new Vector<Aggregate>();
		havingClause = new Vector<Aggregate>();
	}
	
	public void addSelectClause(Aggregate aggr){
		this.selectClause.add(aggr);
	}
	
	public Vector<Aggregate> getSelectClause(){
		return this.selectClause;
	}
	
	public void addHavingClause(Aggregate aggr){
		this.havingClause.add(aggr);
	}
	
	public Vector<Aggregate> getHavingClause(){
		return this.havingClause;
	}
	
	public static void main(String args[]){
		
	}
}
