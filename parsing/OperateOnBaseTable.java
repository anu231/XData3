package parsing;

import net.sf.jsqlparser.statement.select.FromItem;

import org.apache.derby.impl.sql.compile.FromBaseTable;
import org.apache.derby.impl.sql.compile.FromList;

import parsing.Table;
import parsing.JoinTreeNode;
import parsing.FromListElement;

public class OperateOnBaseTable {

	public static FromListElement OperateOnBaseTableJSQL(net.sf.jsqlparser.schema.Table node,
			boolean isJoinTable, String subqueryAlias, JoinTreeNode jtn, QueryParser qParser, boolean isFromSubQuery, boolean isWhereSubQuery) throws Exception {
		String tableName = node.getName();
		Table table = qParser.getTableMap().getTable(tableName);
		
		
		String aliasName = "";
		if (node.getAlias() == null) {
			aliasName = tableName;
		} else {
			aliasName = node.getAlias();
		}
		
	//	Jointree processing
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName);
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(aliasName);
		
		//	FIXME: Mahesh some bug while adding baseRelation
		Util.addFromTable(table,qParser);

		if (aliasName != null) {
			qParser.getQuery().putBaseRelation(aliasName, tableName);
		} else {
			qParser.getQuery().putBaseRelation(tableName, tableName);
		}
		
		if (qParser.getQuery().getRepeatedRelationCount().get(tableName) != null) {
			qParser.getQuery().putRepeatedRelationCount(tableName, qParser.getQuery()
					.getRepeatedRelationCount().get(tableName) + 1);
			//	query.putTableNameToQueryIndex(tableName +  (query.getRepeatedRelationCount().get(tableName)), queryType, queryIndex);
		} else {
			qParser.getQuery().putRepeatedRelationCount(tableName, 1);
			//query.putTableNameToQueryIndex(tableName +  "1", queryType, queryIndex);
		}
		
		qParser.getQuery().putCurrentIndexCount(tableName, qParser.getQuery().getRepeatedRelationCount()
				.get(tableName) - 1);
		
		FromListElement temp = new FromListElement();
		if (node.getAlias() != null) {
			temp.setAliasName(node.getAlias());
		} else {
			temp.setAliasName(node.getName());
		}
		temp.setTableName(node.getName());
		String tableNameNo = tableName
				+ qParser.getQuery().getRepeatedRelationCount().get(tableName);
		temp.setTableNameNo(tableNameNo);
		temp.setTabs(null);
		
		Util.updateTableOccurrences(isFromSubQuery, isWhereSubQuery, tableNameNo, qParser);
		
		if (qParser.getQuery().getCurrentIndex().get(tableName) == null)
			qParser.getQuery().putCurrentIndex(tableName, 0);
		
		return temp;
	}
	public static FromListElement OperateOnBaseTable(FromBaseTable node,
			boolean isJoinTable, String subqueryAlias, JoinTreeNode jtn, QueryParser qParser, boolean isFromSubQuery, boolean isWhereSubQuery) throws Exception {

		String tableName = node.getBaseTableName();
		Table table = qParser.getTableMap().getTable(tableName);
		
		
		String aliasName = "";
		if (node.getCorrelationName() == null) {
			aliasName = tableName;
		} else {
			aliasName = node.getCorrelationName();
		}
		
	//	Jointree processing
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName);
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(aliasName);
		
		//	FIXME: Mahesh some bug while adding baseRelation
		Util.addFromTable(table,qParser);

		if (aliasName != null) {
			qParser.getQuery().putBaseRelation(aliasName, tableName);
		} else {
			qParser.getQuery().putBaseRelation(tableName, tableName);
		}
		
		if (qParser.getQuery().getRepeatedRelationCount().get(tableName) != null) {
			qParser.getQuery().putRepeatedRelationCount(tableName, qParser.getQuery()
					.getRepeatedRelationCount().get(tableName) + 1);
			//	query.putTableNameToQueryIndex(tableName +  (query.getRepeatedRelationCount().get(tableName)), queryType, queryIndex);
		} else {
			qParser.getQuery().putRepeatedRelationCount(tableName, 1);
			//query.putTableNameToQueryIndex(tableName +  "1", queryType, queryIndex);
		}
		
		qParser.getQuery().putCurrentIndexCount(tableName, qParser.getQuery().getRepeatedRelationCount()
				.get(tableName) - 1);
		
		FromListElement temp = new FromListElement();
		if (node.getCorrelationName() != null) {
			temp.setAliasName(node.getCorrelationName());
		} else {
			temp.setAliasName(node.getBaseTableName());
		}
		temp.setTableName(node.getBaseTableName());
		String tableNameNo = tableName
				+ qParser.getQuery().getRepeatedRelationCount().get(tableName);
		temp.setTableNameNo(tableNameNo);
		temp.setTabs(null);
		
		Util.updateTableOccurrences(isFromSubQuery, isWhereSubQuery, tableNameNo, qParser);
		
		if (qParser.getQuery().getCurrentIndex().get(tableName) == null)
			qParser.getQuery().putCurrentIndex(tableName, 0);
		
		return temp;
	}
}
