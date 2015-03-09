package parsing;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.derby.impl.sql.compile.CursorNode;
import org.apache.derby.impl.sql.compile.DeleteNode;
import org.apache.derby.impl.sql.compile.InsertNode;
import org.apache.derby.impl.sql.compile.IntersectOrExceptNode;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SQLParser;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.StatementNode;
import org.apache.derby.impl.sql.compile.UnionNode;
import org.apache.derby.impl.sql.compile.UpdateNode;

import parsing.Column;
import parsing.AggregateFunction;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.FromListElement;
import parsing.JoinClauseInfo;
import parsing.JoinTreeNode;
import parsing.Node;
import parsing.Query;
import parsing.TreeNode;
import parsing.Utility;
import util.Graph;
import util.TableMap;
import parsing.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

class QueryAliasMap {
	/** Data Structure to avoid aliasing in queries.*/

	String queryId;
	String queryIdOrTableName;
	String aliasOfSubqueryOrTable;

	QueryAliasMap() {
		queryId = null;
		queryIdOrTableName = null;
		aliasOfSubqueryOrTable = null;
	}
}

public class QueryParser {

	public ORNode orNode;
	private Query query;
	private TableMap tableMap;

	private Vector<TreeNode> inOrderList;
	private Vector<JoinClauseInfo> joinClauseInfoVector;

	private Vector<JoinClauseInfo> selectionClauseVector;
	private Vector<JoinClauseInfo> foreignKeyVector;


	private Vector<ForeignKey> foreignKeyVectorModified;

	private Vector<JoinClauseInfo> foreignKeyVectorOriginal;
	private Vector<Vector> equivalenceClassVector;
	// currentAliasTables holds the Aliasname and the tablename for the current
	// level of the query.

	public Vector<Node> subQJC;

	// If table is a subquery, it holds alias and "SUBQUERY"
	private HashMap<String, String> currentAliasTables;
	// Data Structure to avoid aliasing in queries.
	private Vector<QueryAliasMap> qam;


	private String currentQueryId;
	private int tableNo; // required for maintaining the repeated occurences of
	// tables.
	// Data Structure to kill IN Clause Mutants and perhaps other subquery
	// mutants also
	private HashMap<Integer, Vector<JoinClauseInfo>> inConds;
	public Vector<Node> allConds;
	private Vector<Node> selectionConds;
	private Vector<Node> likeConds;
	Vector<Node> isNullConds;
	private Vector<Node> inClauseConds;
	Vector<Vector<Node>> equivalenceClasses;
	Vector<Node> joinConds;
	private Vector<Node> foreignKeys;
	Vector<Node> projectedCols;

	//union
	public boolean isUnion;
	public boolean isDeleteNode;
	public boolean isUpdateNode;
	public boolean isIntersectOrExcept;
	public Vector<Node> updateColumn;

	public QueryParser leftQuery;
	public QueryParser rightQuery; 

	// aggregation
	Vector<AggregateFunction> aggFunc;
	Vector<Node> groupByNodes;
	Node havingClause;
	JoinTreeNode root;// not using currently
	HashMap<String, Node> constraintsWithParameters;
	// Subquery
	private Vector<Node> allCondsExceptSubQuery;
	Vector<Node> allSubQueryConds;
	private Vector<Node> allAnyConds;
	Vector<Node> lhsRhsConds;
	int paramCount;
	// private Node whereClausePred;

	Vector<Conjunct> conjuncts;
	Vector<Vector<Node>> allDnfSelCond;
	Vector<Vector<Node>> dnfLikeConds;
	Vector<Vector<Node>> dnfIsNullConds;
	Vector<Vector<Node>> allDnfSubQuery;
	Vector<Vector<Node>> dnfJoinCond;
	Vector<Vector<Vector<Node>>> EqClass;

	private Vector<Node> joinConditionList;

	//To store from clause subqueries
	private Vector<QueryParser> FromClauseSubqueries;//To store from clause subqueries
	private Vector<QueryParser> WhereClauseSubqueries;//To store where clause sub queries
	private HashMap<String, Vector<Node>> aliasedToOriginal;//To store the mapping from aliased columns names to original names

	private HashMap<String, Integer> subQueryNames;//TO store names for the sub queries

	private HashMap<String, Integer[]> tableNames;//It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]

	//Representation in DNF
	Vector<Vector<Node>> dnfCond;
	/*static String getCVCRelOP(){
		
	}*/
	static String[] cvcRelationalOperators = { "DUMMY", "=", "/=", ">",
		">=", "<", "<=", }; // IsNull and IsNotNull not supported currently.

	public FromListElement queryAliases;
	
	public RelationHierarchyNode topLevelRelation;

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}


	public TableMap getTableMap() {
		return tableMap;
	}

	public void setTableMap(TableMap tableMap) {
		this.tableMap = tableMap;
	}


	public Vector<JoinClauseInfo> getJoinClauseInfoVector() {
		return joinClauseInfoVector;
	}

	public void setJoinClauseInfoVector(Vector<JoinClauseInfo> joinClauseInfoVector) {
		this.joinClauseInfoVector = joinClauseInfoVector;
	}


	public Vector<QueryAliasMap> getQam() {
		return qam;
	}

	public void setQam(Vector<QueryAliasMap> qam) {
		this.qam = qam;
	}

	public void setQueryAliases(FromListElement queryAliases) {
		this.queryAliases = queryAliases;
	}



	public void setFromClauseSubqueries(Vector<QueryParser> fromClauseSubqueries) {
		FromClauseSubqueries = fromClauseSubqueries;
	}


	public HashMap<String, Vector<Node>> getAliasedToOriginal() {
		return aliasedToOriginal;
	}

	public void setAliasedToOriginal(HashMap<String, Vector<Node>> aliasedToOriginal) {
		this.aliasedToOriginal = aliasedToOriginal;
	}


	public HashMap<String, Integer> getSubQueryNames() {
		return subQueryNames;
	}

	public void setSubQueryNames(HashMap<String, Integer> subQueryNames) {
		this.subQueryNames = subQueryNames;
	}


	public HashMap<String, Integer[]> getTableNames() {
		return tableNames;
	}

	public void setTableNames(HashMap<String, Integer[]> tableNames) {
		this.tableNames = tableNames;
	}



	public Vector<Conjunct> getConjuncts() {
		return conjuncts;
	}

	public void setConjuncts(Vector<Conjunct> conjuncts) {
		this.conjuncts = conjuncts;
	}

	public Vector<Vector<Node>> getDnfCond() {
		return dnfCond;
	}

	public void setDnfCond(Vector<Vector<Node>> dnfCond) {
		this.dnfCond = dnfCond;
	}

	public Vector<Vector<Node>> getAllDnfSelCond() {
		return allDnfSelCond;
	}

	public void setAllDnfSelCond(Vector<Vector<Node>> allDnfSelCond) {
		this.allDnfSelCond = allDnfSelCond;
	}

	public Vector<Vector<Node>> getDnfLikeConds() {
		return dnfLikeConds;
	}

	public void setDnfLikeConds(Vector<Vector<Node>> dnfLikeConds) {
		this.dnfLikeConds = dnfLikeConds;
	}

	public Vector<Vector<Node>> getDnfIsNullConds() {
		return dnfIsNullConds;
	}

	public void setDnfIsNullConds(Vector<Vector<Node>> dnfIsNullConds) {
		this.dnfIsNullConds = dnfIsNullConds;
	}

	public Vector<Vector<Node>> getAllDnfSubQuery() {
		return allDnfSubQuery;
	}

	public void setAllDnfSubQuery(Vector<Vector<Node>> allDnfSubQuery) {
		this.allDnfSubQuery = allDnfSubQuery;
	}

	public Vector<Vector<Node>> getDnfJoinCond() {
		return dnfJoinCond;
	}

	public void setDnfJoinCond(Vector<Vector<Node>> dnfJoinCond) {
		this.dnfJoinCond = dnfJoinCond;
	}

	public Vector<Vector<Vector<Node>>> getEqClass() {
		return EqClass;
	}

	public void setEqClass(Vector<Vector<Vector<Node>>> eqClass) {
		EqClass = eqClass;
	}


	// @junaid modified
	public FromListElement getQueryAliases() {
		return queryAliases;
	}



	public Vector<Node> getLhsRhsConds() {
		return lhsRhsConds;
	}

	public JoinTreeNode getRoot() {
		return root;
	}


	public Vector<Node> getAllCondsExceptSubQuery() {
		return allCondsExceptSubQuery;
	}

	public void setAllCondsExceptSubQuery(Vector<Node> allCondsExceptSubQuery) {
		this.allCondsExceptSubQuery = allCondsExceptSubQuery;
	}

	public Vector<Node> getAllSubQueryConds() {
		return allSubQueryConds;
	}

	public void setSubQueryConds(Vector<Node> subQueryConds) {
		this.allSubQueryConds = subQueryConds;
	}

	public Vector<AggregateFunction> getAggFunc() {
		return aggFunc;
	}

	public void setAggFunc(Vector<AggregateFunction> aggFunc) {
		this.aggFunc = aggFunc;
	}

	public Vector<Node> getGroupByNodes() {
		return groupByNodes;
	}

	public void setGroupByNodes(Vector<Node> groupByNodes) {
		this.groupByNodes = groupByNodes;
	}

	public Node getHavingClause() {
		return havingClause;
	}

	public void setHavingClause(Node havingClause) {
		this.havingClause = havingClause;
	}

	public Vector<Node> getSelectionConds() {
		return selectionConds;
	}

	public Vector<Node> getIsNullConds() {
		return isNullConds;
	}

	public Vector<Node> getLikeConds() {
		return likeConds;
	}

	public Vector<Node> getAllConds() {
		return allConds;
	}

	public Vector<Vector<Node>> getEquivalenceClasses() {
		return equivalenceClasses;
	}

	public Vector<Node> getForeignKeys() {
		return foreignKeys;
	}

	public HashMap<Integer, Vector<JoinClauseInfo>> getInConds() {
		return inConds;
	}

	public Vector<Node> getJoinConds() {
		return joinConds;
	}

	public Vector<Node> getProjectedCols() {
		return projectedCols;
	}


	public Vector<QueryParser> getFromClauseSubqueries(){
		return this.FromClauseSubqueries;
	}

	public Vector<QueryParser> getWhereClauseSubqueries(){
		return this.WhereClauseSubqueries;
	}

	public QueryParser(TableMap tableMap) {
		this.tableMap = tableMap;
		this.inOrderList = new Vector<TreeNode>();
		this.joinClauseInfoVector = new Vector<JoinClauseInfo>();
		this.selectionClauseVector = new Vector<JoinClauseInfo>();
		this.foreignKeyVector = new Vector<JoinClauseInfo>();
		orNode = new ORNode();
		this.conjuncts = new Vector<Conjunct>();
		dnfCond = new Vector<Vector<Node>>();
		dnfJoinCond = new Vector<Vector<Node>>();
		dnfLikeConds = new Vector<Vector<Node>>();
		allDnfSelCond = new Vector<Vector<Node>>();
		EqClass =new Vector<Vector<Vector<Node>>>();
		dnfIsNullConds = new Vector<Vector<Node>>();
		allDnfSubQuery =new Vector<Vector<Node>>();
		//orNode = new ORNode();
		this.foreignKeyVectorModified = new Vector<ForeignKey>();

		this.equivalenceClassVector = new Vector<Vector>();
		this.currentAliasTables = new HashMap<String, String>();
		qam = new Vector<QueryAliasMap>();
		this.currentQueryId = "Q";
		inConds = new HashMap<Integer, Vector<JoinClauseInfo>>();
		allConds = new Vector<Node>();
		equivalenceClasses = new Vector<Vector<Node>>();

		aliasedToOriginal = new HashMap<String, Vector<Node>>();
		subQueryNames = new HashMap<String, Integer>();
		tableNames = new HashMap<String, Integer[]>();

		joinConds = new Vector<Node>();
		foreignKeys = new Vector<Node>();
		projectedCols = new Vector<Node>();
		selectionConds = new Vector<Node>();
		likeConds = new Vector<Node>();
		isNullConds = new Vector<Node>();
		inClauseConds = new Vector<Node>();
		aggFunc = new Vector<AggregateFunction>();
		groupByNodes = new Vector<Node>();
		havingClause = new Node();
		allSubQueryConds = new Vector<Node>();
		allCondsExceptSubQuery = new Vector<Node>();
		subQJC = new Vector<Node>();
		tableNo = 0;

		joinConditionList = new Vector<Node>();
		root = null;
		constraintsWithParameters = new HashMap<String, Node>();
		lhsRhsConds = new Vector<Node>();

		updateColumn=new Vector<Node>();
		this.FromClauseSubqueries = new Vector<QueryParser>();
		this.WhereClauseSubqueries = new Vector<QueryParser>();
	}

	public String getModifiedQuery(ResultSetNode rsNode, boolean debug,
			QueryParser qp) throws Exception {
		HashMap<String, String> currentAT = new HashMap<String, String>();
		// this.currentAliasTables = new HashMap<String, String>();
		String fromClauseString = Utility.getFromClauseString(rsNode, qp,
				currentAT);
		// currentAT.putAll(this.getCurrentAliasTables());
		String whereCluase = Utility.getWhereClauseString(rsNode, true);
		String groupByClause = Utility
				.getGroupByClauseAttributes(((SelectNode) rsNode)
						.getGroupByList());

		/*
		 * Currently not handling Group by cluase if (getGroupByColumns() !=
		 * null && getGroupByColumns().size()>0) { remainingQuery =
		 * remainingQuery.substring(0,remainingQuery.indexOf("GROUP BY")); }
		 */

		String selectClause = "SELECT ";
		// boolean b = false;

		for (String aliasName : currentAT.keySet()) {
			if (currentAT.get(aliasName).equalsIgnoreCase("SUBQUERY")) {
				selectClause = selectClause + aliasName + ".*, ";
			} else {
				String tableName = query.getTableOfAlias(aliasName).get(0);
				selectClause = selectClause + aliasName + ".CTID AS "
						+ tableName + "CTID, ";
				selectClause = selectClause + aliasName + ".*, ";
			}
		}
		/*
		 * for(String tableName : query.getFromTables().keySet()){ Table table =
		 * query.getFromTables().get(tableName); tableName =
		 * tableName.toUpperCase(); if(table.getAliasName()!=null) selectClause
		 * += table.getAliasName(); else selectClause += tableName;
		 * 
		 * selectClause += ".CTID AS "; selectClause += tableName+"CTID, ";
		 * 
		 * if(table.getAliasName()!=null) selectClause +=
		 * table.getAliasName()+".*, "; else selectClause += tableName+".*, "; }
		 */
		selectClause = selectClause.substring(0, selectClause.length() - 2)
				.toString();

		// This code checks if the projected columns have a foreign key
		// relationship with any other table that is not projected.
		// If so it adds the table to the projection list.
		// Info added for JoinClause
		Graph<Table, JoinClauseInfo> joinGraph = query.getJoinGraph();
		// Graph<Table,ForeignKey> foreignKeyGraph =
		// tableMap.getForeignKeyGraph();
		for (String tableName : query.getFromTables().keySet()) {
			Table table = query.getFromTables().get(tableName);
			if (table.hasForeignKey()) {
				for (String fKeyName : table.getForeignKeys().keySet()) {
					ForeignKey fKey = table.getForeignKey(fKeyName);
					Vector<Column> fKeyColumns = fKey.getFKeyColumns();
					// Vector<Column> refKeyColumns =
					// fKey.getReferenceKeyColumns();
					boolean joinFound = false;
					if (joinGraph.getNeighbours(table) != null) {
						for (Table joinTable : joinGraph.getNeighbours(table)
								.keySet()) {
							for (JoinClauseInfo joinClauseInfo : joinGraph
									.getEdges(table, joinTable)) {
								if (joinClauseInfo.contains(fKey
										.getFKTablename())
										&& joinClauseInfo.contains(fKey
												.getReferenceTable()
												.getTableName())) {
									// if(joinClause.contains(fKeyColumns) &&
									// joinClause.contains(refKeyColumns)){
									joinFound = true;
								}
							}
						}
					}
					if (!joinFound) {
						Table referenceTable = fKey.getReferenceTable();
						if (query.getBaseRelation().containsKey(
								referenceTable.getTableName())) {
							continue;
						}
						if (!selectClause.contains(referenceTable.getTableName())) {
							fromClauseString += JoinClauseInfo.leftOuterJoin+ " " + referenceTable.getTableName()+ " ON (";
							for (Column fKeyColumn : fKeyColumns) {
								if (table.getAliasName() != null)
									fromClauseString += table.getAliasName()+ "." + fKeyColumn.getColumnName()+ "=";
								else
									fromClauseString += table.getTableName()+ "." + fKeyColumn.getColumnName()+ "=";

								fromClauseString += fKeyColumn
										.getReferenceTableName()
										+ "."
										+ fKeyColumn.getReferenceColumn()
										.getColumnName();
								fromClauseString += " AND ";
							}
							fromClauseString = fromClauseString.substring(0,
									fromClauseString.length() - 5);
							fromClauseString += ")";

							if (!query.getFromTables().containsKey(referenceTable)) {
								selectClause += ", "+ referenceTable.getTableName()+ ".CTID AS "+ referenceTable.getTableName()	+ "CTID";
								selectClause += ", "+ referenceTable.getTableName() + ".* ";
							}
						}
					}
				}
			}// till here: add FK tables to projection list
		}

		// System.out.println("SELECT DISTINCT "+groupByClause+fromClauseString+whereCluase);
		query.setQueryForGroupBy("SELECT DISTINCT " + groupByClause
				+ fromClauseString + whereCluase);
		// TODO Why is group by clause not added?
		String modifiedQueryString = selectClause + fromClauseString
				+ whereCluase;// +" order by random()";
		this.currentAliasTables = new HashMap<String, String>();
		if (debug)
			System.out.println("\nModified Query : " + modifiedQueryString);
		return modifiedQueryString;
	}

	public void parseQuery(String queryId, String queryString) throws Exception {
		parseQuery(queryId, queryString, true);
	}
	
	public void parseQueryJSQL(String queryId, String queryString, boolean debug)throws Exception{
		System.out.println("queryString" + queryString);
		queryString=queryString.trim().replaceAll("\n+", " ");
		queryString=queryString.trim().replaceAll(" +", " ");		
		queryString=preParseQuery(queryId,queryString);		
		this.query = new Query(queryId, queryString);
		
		CCJSqlParserManager pm = new CCJSqlParserManager();
		Statement stmt = pm.parse(new StringReader(query.getQueryString()));
		
		if (stmt instanceof Select){
			//vary simple query
			ProcessResultSetNode.processResultSetNodeJSQL(stmt, debug, this);
		}
		
	}
	
	public void parseQuery(String queryId, String queryString, boolean debug)
			throws Exception {
		System.out.println("queryString" + queryString);
		queryString=queryString.trim().replaceAll("\n+", " ");
		queryString=queryString.trim().replaceAll(" +", " ");		
		if(queryString.toLowerCase().contains("year")){
			queryString=queryString.replaceAll("year","year1");
			queryString=queryString.replaceAll("Year","year1");
			queryString=queryString.replaceAll("YEAR","year1");
		}
		queryString=preParseQuery(queryId,queryString);		
		this.query = new Query(queryId, queryString);
		SQLParser sqlParser = new SQLParser();
		query.setQueryString(queryString);

		

		BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
		//System.out.println("Original query :"+queryString);
		//queryString = generateCleanQry(queryString, "", "");		
		System.out.println("Cleaned query:"+queryString);

		StatementNode s=sqlParser.Statement(queryString, null);
		ResultSetNode rsNode ;

		if(s instanceof InsertNode){
			rsNode = ((InsertNode)s).getResultSetNode();

		}else if(s instanceof DeleteNode){
			rsNode = ((DeleteNode)s).getResultSetNode();
			isDeleteNode=true;
			havingClause=null;
		}else if(s instanceof UpdateNode){
			rsNode = ((UpdateNode)s).getResultSetNode();
			isUpdateNode=true;
			havingClause=null;			
		}else{
			rsNode = ((CursorNode)s).getResultSetNode();
		}	



		//UpdateResultSet u=(UpdateResultSet)rsNode;


		if( rsNode instanceof UnionNode){

			UnionNode unionNode=(UnionNode)rsNode;
			isUnion=true;


			/*ResultSetNode left=unionNode.getLeftResultSet();
				ResultSetNode right=unionNode.getRightResultSet();

				processResultSetNode(left, debug);
				processResultSetNode(right, debug);*/

			leftQuery = new QueryParser(TableMap.getInstances());
			String left=queryString.substring(0,queryString.toLowerCase().indexOf("union"));
			leftQuery.parseQuery("q2", left);

			rightQuery = new QueryParser(TableMap.getInstances());
			String right=queryString.substring(queryString.toLowerCase().indexOf("union")+5);
			rightQuery.parseQuery("q3", right);



		}else if (rsNode instanceof IntersectOrExceptNode){

			IntersectOrExceptNode node =(IntersectOrExceptNode)rsNode;
			isIntersectOrExcept=true;

			leftQuery = new QueryParser(TableMap.getInstances());
			String left=queryString.substring(0,queryString.toLowerCase().indexOf("except"));
			leftQuery.parseQuery("q2", left);

			rightQuery = new QueryParser(TableMap.getInstances());
			String right=queryString.substring(queryString.toLowerCase().indexOf("except")+6);
			rightQuery.parseQuery("q3", right);

		}else{
			ProcessResultSetNode.processResultSetNode(rsNode, debug,this);
		}

	}

	//Added by Ankit
	/*Pre parse query to remove with clause*/	

	public String preParseQuery(String queryId,String queryString) throws Exception{

		StringTokenizer st=new StringTokenizer(queryString.trim());
		String token=st.nextToken();

		if(!token.equalsIgnoreCase("with")){
			return queryString;
		}
		int numberOfAlias=0;
		String aliasname[]=new String[10];
		String subquery[]=new String[10];

		while(true){

			String columnname="";
			aliasname[numberOfAlias]=st.nextToken();

			if(aliasname[numberOfAlias].contains("(")){

				columnname=aliasname[numberOfAlias].substring(aliasname[numberOfAlias].indexOf("("));
				columnname=columnname.substring(1,columnname.length()-1);	//remove ( & )

				aliasname[numberOfAlias]=aliasname[numberOfAlias].substring(0,aliasname[numberOfAlias].indexOf("("));           	

			}
			token=st.nextToken();   	// should be AS key word or should start with (

			if(token.startsWith("(")){
				while(!token.contains(")")){
					columnname+=token;
					token=st.nextToken();
				}
				columnname+=token;            	
				token=st.nextToken();	// should be AS key word
			}

			if(!token.equalsIgnoreCase("as")){            	
				Exception e= new Exception("Error while preparsing the with clause AS expected");
				throw e;
			}
			
			subquery[numberOfAlias]="(";
			queryString=queryString.substring(queryString.indexOf("(")+1);
			if(columnname.length()!=0){
				queryString=queryString.substring(queryString.indexOf("(")+1);
			}

			int count=1,i=0;
			while(count!=0){
				if(queryString.charAt(i)=='('){
					count++;
				}else if(queryString.charAt(i)==')'){
					count--;
				}
				subquery[numberOfAlias]+=queryString.charAt(i);
				i++;
			}
			queryString=queryString.substring(i).trim();

			if(columnname.length()!=0){
				columnname=columnname.substring(1,columnname.length()-1);
				String columnlist[]=columnname.split(",");
				int ctr=0;
				String temp=subquery[numberOfAlias];
				subquery[numberOfAlias]="";            	
				String tok=temp.substring(0,temp.indexOf("from"));
				for(int j=0;j<tok.length();j++){
					if(tok.charAt(j)==','){
						subquery[numberOfAlias]+=" as "+columnlist[ctr++]+" , ";
					}else{
						subquery[numberOfAlias]+=tok.charAt(j);
					}

				}            	            	
				subquery[numberOfAlias]+=" as "+columnlist[ctr]+" "+temp.substring(temp.indexOf("from"));
			}

			numberOfAlias++;
			if(queryString.charAt(0)!=','){            	
				break;
			}else{
				st=new StringTokenizer(queryString.substring(1).trim());
			}

		}

		String newquery="";
		/*Add the select part to new query */
		st=new StringTokenizer(queryString);                    
		//token=st.nextToken();
		
		while(st.hasMoreTokens()){
			
			token=st.nextToken();
			
			if(token.toLowerCase().equals("from")){
				newquery+=token+ " ";
				newquery = parseFromPart(st, newquery, numberOfAlias, subquery, aliasname);				
			}
			else{			
				newquery+=token+ " ";
			}
		}

		return newquery;
	}
	
	private String parseFromPart(StringTokenizer st, String newquery, int numberOfAlias, String subquery[], String aliasname[]){
		
		String token;
		
		while(st.hasMoreTokens()){
			token=st.nextToken();            
					
			if(token.equalsIgnoreCase("where")||token.equalsIgnoreCase("group")){
				newquery+=token+ " ";
				break;
			}			
			
			if(token.equals(",")){
				newquery+=token+ " ";
			}
			if(token.contains(",")){
				token+=" ";
				String tablenames[]=token.split(",");
				for(int j=0;j<tablenames.length;j++){
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(tablenames[j].equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=tablenames[j]+" ";
					}
					newquery+=",";
				}
				newquery=newquery.substring(0,newquery.length()-1);

			}else if(token.contains(")")){
				String relationName = token.substring(0, token.length() - 1);				
				
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(relationName.equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=relationName + " ";
					}
					newquery+=")";
				
			}else{
				boolean isPresent=false;
				for(int k=0;k<numberOfAlias;k++){
					if(token.equals(aliasname[k])){
						newquery+=subquery[k] + " " + aliasname[k]+" ";
						isPresent=true;
					}
				}
				if(!isPresent){
					newquery+=token+" ";
				}
			}

		}
		
		return newquery;		
	}

	public Vector<TreeNode> getInOrderList() {
		return inOrderList;
	}

	public void setInOrderList(Vector<TreeNode> inOrderList) {
		this.inOrderList = inOrderList;
	}

	public Vector<JoinClauseInfo> getSelectionClauseVector() {
		return selectionClauseVector;
	}

	public void setSelectionClauseVector(
			Vector<JoinClauseInfo> selectionClauseVector) {
		this.selectionClauseVector = selectionClauseVector;
	}

	public Vector<JoinClauseInfo> getForeignKeyVector() {
		return foreignKeyVector;
	}

	public void setForeignKeyVector(Vector<JoinClauseInfo> foreignKeyVector) {
		this.foreignKeyVector = foreignKeyVector;
	}

	public Vector<ForeignKey> getForeignKeyVectorModified() {
		return foreignKeyVectorModified;
	}

	public void setForeignKeyVectorModified(
			Vector<ForeignKey> foreignKeyVectorModified) {
		this.foreignKeyVectorModified = foreignKeyVectorModified;
	}

	public Vector<JoinClauseInfo> getForeignKeyVectorOriginal() {
		return foreignKeyVectorOriginal;
	}

	public void setForeignKeyVectorOriginal(
			Vector<JoinClauseInfo> foreignKeyVectorOriginal) {
		this.foreignKeyVectorOriginal = foreignKeyVectorOriginal;
	}

	public Vector<Vector> getEquivalenceClassVector() {
		return equivalenceClassVector;
	}

	public void setEquivalenceClassVector(Vector<Vector> equivalenceClassVector) {
		this.equivalenceClassVector = equivalenceClassVector;
	}

	public Vector<Node> getSubQJC() {
		return subQJC;
	}

	public void setSubQJC(Vector<Node> subQJC) {
		this.subQJC = subQJC;
	}

	public HashMap<String, String> getCurrentAliasTables() {
		return currentAliasTables;
	}

	public void setCurrentAliasTables(HashMap<String, String> currentAliasTables) {
		this.currentAliasTables = currentAliasTables;
	}

	public String getCurrentQueryId() {
		return currentQueryId;
	}

	public void setCurrentQueryId(String currentQueryId) {
		this.currentQueryId = currentQueryId;
	}

	public int getTableNo() {
		return tableNo;
	}

	public void setTableNo(int tableNo) {
		this.tableNo = tableNo;
	}

	public Vector<Node> getInClauseConds() {
		return inClauseConds;
	}

	public void setInClauseConds(Vector<Node> inClauseConds) {
		this.inClauseConds = inClauseConds;
	}

	public boolean isUnion() {
		return isUnion;
	}

	public void setUnion(boolean isUnion) {
		this.isUnion = isUnion;
	}

	public boolean isDeleteNode() {
		return isDeleteNode;
	}

	public void setDeleteNode(boolean isDeleteNode) {
		this.isDeleteNode = isDeleteNode;
	}

	public boolean isUpdateNode() {
		return isUpdateNode;
	}

	public void setUpdateNode(boolean isUpdateNode) {
		this.isUpdateNode = isUpdateNode;
	}

	public boolean isIntersectOrExcept() {
		return isIntersectOrExcept;
	}

	public void setIntersectOrExcept(boolean isIntersectOrExcept) {
		this.isIntersectOrExcept = isIntersectOrExcept;
	}

	public Vector<Node> getUpdateColumn() {
		return updateColumn;
	}

	public void setUpdateColumn(Vector<Node> updateColumn) {
		this.updateColumn = updateColumn;
	}

	public QueryParser getLeftQuery() {
		return leftQuery;
	}

	public void setLeftQuery(QueryParser leftQuery) {
		this.leftQuery = leftQuery;
	}

	public QueryParser getRightQuery() {
		return rightQuery;
	}

	public void setRightQuery(QueryParser rightQuery) {
		this.rightQuery = rightQuery;
	}

	public HashMap<String, Node> getConstraintsWithParameters() {
		return constraintsWithParameters;
	}

	public void setConstraintsWithParameters(
			HashMap<String, Node> constraintsWithParameters) {
		this.constraintsWithParameters = constraintsWithParameters;
	}

	public Vector<Node> getAllAnyConds() {
		return allAnyConds;
	}

	public void setAllAnyConds(Vector<Node> allAnyConds) {
		this.allAnyConds = allAnyConds;
	}

	public int getParamCount() {
		return paramCount;
	}

	public void setParamCount(int paramCount) {
		this.paramCount = paramCount;
	}

	public Vector<Node> getJoinConditionList() {
		return joinConditionList;
	}

	public void setJoinConditionList(Vector<Node> joinConditionList) {
		this.joinConditionList = joinConditionList;
	}

	public static String[] getCvcRelationalOperators() {
		return cvcRelationalOperators;
	}

	public static void setCvcRelationalOperators(String[] cvcRelationalOperators) {
		QueryParser.cvcRelationalOperators = cvcRelationalOperators;
	}

	public void setInConds(HashMap<Integer, Vector<JoinClauseInfo>> inConds) {
		this.inConds = inConds;
	}

	public void setAllConds(Vector<Node> allConds) {
		this.allConds = allConds;
	}

	public void setSelectionConds(Vector<Node> selectionConds) {
		this.selectionConds = selectionConds;
	}

	public void setLikeConds(Vector<Node> likeConds) {
		this.likeConds = likeConds;
	}

	public void setIsNullConds(Vector<Node> isNullConds) {
		this.isNullConds = isNullConds;
	}

	public void setEquivalenceClasses(Vector<Vector<Node>> equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}

	public void setJoinConds(Vector<Node> joinConds) {
		this.joinConds = joinConds;
	}

	public void setForeignKeys(Vector<Node> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}

	public void setProjectedCols(Vector<Node> projectedCols) {
		this.projectedCols = projectedCols;
	}

	public void setRoot(JoinTreeNode root) {
		this.root = root;
	}

	public void setAllSubQueryConds(Vector<Node> allSubQueryConds) {
		this.allSubQueryConds = allSubQueryConds;
	}

	public void setLhsRhsConds(Vector<Node> lhsRhsConds) {
		this.lhsRhsConds = lhsRhsConds;
	}

	public void setWhereClauseSubqueries(Vector<QueryParser> whereClauseSubqueries) {
		WhereClauseSubqueries = whereClauseSubqueries;
	}


	public boolean alreadyNotExistInEquivalenceClass(ArrayList<Node> S, Node ece) {
		for (int i = 0; i < S.size(); i++) {
			Node temp = S.get(i);
			if (temp.getTableNameNo().equalsIgnoreCase(ece.getTableNameNo())
					&& temp.getColumn().getColumnName().equalsIgnoreCase(ece.getColumn().getColumnName()) )
				/*if(temp.getTable() == ece.getTable() &&
					temp.getColumn() == ece.getColumn())*/
				return false;
		}

		return true;
	}

	/**
	 * Revamp allConds. It should now contain the distinct predicates not
	 * containing a AND (or OR but ORs not considered for the moment) TODO: Do
	 * something about the presence of ORs: Need to convert the predicate into
	 * CNF and then create datasets by nulling each pair Eg.: if R.a = S.b OR
	 * T.c = U.d is the predicate, then create datasets by killing each of the
	 * following: 1. R.a and Tc 2. R.a and U.d 3. S.b and T.c 4. S.b and U.d
	 */

	public static void flattenAndSeparateAllConds(QueryParser qParser) {
		if(qParser.allConds == null)
			return ;

		Vector<Node> allCondsDuplicate = new Vector<Node>();
		allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

		qParser.allConds.removeAllElements();
		Vector<Vector<Node>> allDnfDuplicate= new Vector<Vector<Node>>();
		allDnfDuplicate =(Vector<Vector<Node>>) qParser.dnfCond.clone();

		qParser.dnfCond.removeAllElements();
		Node temp;
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			if(allCondsDuplicate.get(i) != null)
				qParser.allConds.addAll(GetNode.flattenNode(qParser, allCondsDuplicate.get(i)));
		}

		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			if(allCondsDuplicate.get(i) != null)
				qParser.dnfCond.addAll(GetNode.flattenCNF(qParser, allCondsDuplicate.get(i)));
		}

		for (int i=0;i< allCondsDuplicate.size() ; i++) {
			if(allCondsDuplicate.get(i)!=null){
				qParser.orNode=GetNode.flattenOr(allCondsDuplicate.get(i));
			}

		}

		Conjunct.createConjunsts(qParser);

		allCondsDuplicate.removeAllElements();
		allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

		allDnfDuplicate.removeAllElements();
		allDnfDuplicate = (Vector<Vector<Node>>) qParser.dnfCond.clone();

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				String type=n.getType();
				if(type.equalsIgnoreCase(Node.getAllAnyNodeType()) || type.equalsIgnoreCase(Node.getInNodeType()) ||
						type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
						||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
					subCond.add(n);
					temp1.remove(n);
				}
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.allDnfSubQuery.add(subCond);
			}
		}

		for(Node n:allCondsDuplicate){
			String type=n.getType();
			if(type.equalsIgnoreCase(Node.getAllAnyNodeType()) || type.equalsIgnoreCase(Node.getInNodeType()) ||
					type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
					||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
				qParser.allSubQueryConds.add(n);
				qParser.allConds.remove(n);
			}
		}

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
						&& n.getOperator().equalsIgnoreCase("=")) {
					if (n.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
							&& n.getRight().getType().equalsIgnoreCase(
									Node.getColRefType())) {
						subCond.add(n);
						temp1.remove(n);
					}
				}

			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.dnfJoinCond.add(subCond);
			}
		}

		// Now separate Join Conds for EC And Selection Conds and Non Equi join
		// conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);

			Conjunct con = new Conjunct( new Vector<Node>());

			boolean isJoinNodeForEC = GetNode.getJoinNodesForEC(con, temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeForEC) {
				isJoinNodeForEC = false;
				qParser.allConds.remove(temp);
			}
		}

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if (n.containsConstant()) {
					subCond.add(n);
					temp1.remove(n);

				}
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.allDnfSelCond.add(subCond);
			}
		}

		// Now separate Selection conds into the vector Selection Conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);

			Conjunct con = new Conjunct( new Vector<Node>());

			boolean isSelection = GetNode.getSelectionNode(con,temp);
			if (isSelection) {
				isSelection = false;
				// remove it from allConds as it is added to selection
				// conditions
				qParser.allConds.remove(temp);
			}
		}

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			//Vector<Node> 
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if(n.getType().equalsIgnoreCase(Node.getLikeNodeType())){//CharConstantNode
					subCond.add(n);
					temp1.remove(n);

				}    
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.dnfLikeConds.add(subCond);
			}
		}

		//Added by Bikash----------------------------------------------------
		//For the like operator
		for(int i=0;i<allCondsDuplicate.size();i++){
			temp = allCondsDuplicate.get(i);

			Conjunct con = new Conjunct( new Vector<Node>());
			boolean isLikeType = GetNode.getLikeNode(con,temp);
			if(isLikeType){
				isLikeType = false;
				//remove it from allConds as it is added to like conditions
				qParser.allConds.remove(temp);
			}
		}

		//***************************************************************************/
		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			//Vector<Node> 
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if(n.getType().equals(Node.getIsNullNodeType())){
					subCond.add(n);
					temp1.remove(n);

				}    
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.dnfIsNullConds.add(subCond);
			}
		}

		for(Node n:allCondsDuplicate){
			if(n.getType().equals(Node.getIsNullNodeType())){
				qParser.isNullConds.add(n);
				qParser.allConds.remove(n);
			}
		}


		//Now get the lhsRhs conditions in a separate vector, lhsRhsConds
		//This has to be added in each and every killing procedure as positive cond
		for(int i=0;i<qParser.allSubQueryConds.size();i++){
			Node n = qParser.allSubQueryConds.get(i);
			if(n.getLhsRhs()==null || n.getType().equalsIgnoreCase(Node.getExistsNodeType()) || n.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))	
				continue;
			Vector<Node> lhsRhs = GetNode.flattenNode(qParser, n.getLhsRhs());
			qParser.lhsRhsConds.addAll(lhsRhs);				//Why is this variable required???
		}

		for(Node n: qParser.allSubQueryConds){
			Vector<Node> subQConds=(Vector<Node>)n.getSubQueryConds().clone();
			n.getSubQueryConds().removeAllElements();
			for(Node subQ:subQConds){
				n.getSubQueryConds().addAll(GetNode.flattenNode(qParser,subQ));
				//n.setSubQueryConds(flattenNode(subQ));
			}
		}


		// Now separate SubqueryConds into the vector subQueyConds
		// for(int i=0;i<allCondsDuplicate.size();i++){
		// temp = allCondsDuplicate.get(i);
		// boolean isSelection = getSubQueryNode(temp);
		// if(isSelection){
		// isSelection = false;
		// //remove it from allConds as it is added to subQuery conditions
		// allConds.remove(temp);
		// }
		// }
	}

}
