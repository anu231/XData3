
package testDataGen;

/**
 * This class stores details about the whole query
 * 
 * @author mahesh
 */
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GetCVC3HeaderAndFooter;

import java.util.*;
import java.util.Map.Entry;

import killMutations.GenerateDataForOriginalQuery;
import killMutations.MutationsInFromSubQuery;
import killMutations.MutationsInOuterBlock;
import killMutations.MutationsInWhereSubQuery;
import parsing.Column;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Query;
import parsing.QueryParser;
import parsing.Table;
import stringSolver.StringConstraintSolver;
import util.TableMap;
import util.TagDatasets;
import util.TagDatasets.MutationType;
import util.TagDatasets.QueryBlock;

public class GenerateCVC1 {
	/**FIXME: appConstraints, NonEmptyConstraints...whether they are common for query or for each block of query*/

	/** Details about the the tables in the input database */
	private TableMap tableMap;
	private Query query;




	private Query topQuery;

	/** The parser stores the details of the query after the input query is parsed	 */
	private QueryParser qParser;

	/** Stores the base relation for each repeated occurrence of a relation  */

	private HashMap<String,String> baseRelation; 

	/** Maintains the increment for each repeated occurrence of a relation. For instance if R repeats twice as R1 and R2 
	 * then the currentIndexCount of R is 0, R1 is 1 and that of R2 is 2.  */
	private  HashMap<String,Integer> currentIndexCount; 

	/**  For each relation maintains a count of how many times it repeats. Incides of a relation should be incremented by this number */
	private HashMap<String,Integer> repeatedRelationCount;

	/** Stores the positions at which the indexes for each repeated relations start */
	private HashMap<String, Integer[]> repeatedRelNextTuplePos;

	/** Stores details about the number of tuples for each occurrence of the relation in the input query */
	private HashMap<String, Integer> noOfTuples;

	/** Stores the no of tuples to be generated for each relation */
	private HashMap<String,Integer> noOfOutputTuples;

	/** Stores details about the outer block of the query */
	public QueryBlockDetails outerBlock;

	/** Stores details about the foreign keys of the tables of the query*/
	private ArrayList<Node> foreignKeys;

	/** Stores details about the foreign keys of the tables of the query*/
	private ArrayList<ForeignKey> foreignKeysModified;

	/** Stores the list of constraints for this data generation step*/
	private ArrayList<String> constraints;

	/** Stores the list of string constraints for this data generation step*/
	private ArrayList<String> stringConstraints;

	private String CVCStr;

	private HashMap<Table,Vector<String> > resultsetTableColumns1;

	private Vector<Column> resultsetColumns;

	/** Keeps track of which columns have which null values and which of them have been used*/
	private HashMap<Column, HashMap<String, Integer>> colNullValuesMap;	

	/** Reference to string solver */
	private StringConstraintSolver stringSolver;

	/**FORALL/ NOT EXISTS:	fne = true/false*/
	private boolean fne;		

	/**FIXME: Why this??*/
	private ArrayList<String> datatypeColumns;

	private String output;

	/** Used to store the English description of the query */
	private String queryString;

	/** Sets the path to the location where the file containing queries is located and where the output files will be generated */
	private String filePath;

	/** Used to number the data sets */
	private int count;


	private ArrayList<Table> resultsetTables;

	/** I/P DATABASE: 		ipdb = true/false*/
	private boolean ipdb;   		

	/**Stores CVC3 Header*/
	private String CVC3_HEADER;

	/**stores details about the query, if query consists of set operations*/
	private GenerateUnionCVC unionCVC;


	/**details about branch queries of the input if any*/
	private BranchQueriesDetails branchQueries; 

	private Vector<Table> tablesOfOriginalQuery;

	/**Indicates the type of mutation we are trying to kill*/
	private String typeOfMutation;

	/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
	private HashMap<String, Integer[]> tableNames;

	/** Contains the list of equi join condition for each table in the given query. Used during foreign key constraints*/
	private HashMap<String, Vector<Vector<Node>> > equiJoins; 

	/** The constructor for this class */
	public GenerateCVC1 (){
		tableMap = TableMap.getInstances();
		baseRelation = new HashMap<String, String>();
		currentIndexCount = new HashMap<String, Integer>();
		repeatedRelationCount = new HashMap<String, Integer>();
		repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
		noOfTuples = new HashMap<String, Integer>();
		noOfOutputTuples = new HashMap<String, Integer>();
		outerBlock = new QueryBlockDetails();
		colNullValuesMap = new HashMap<Column, HashMap<String,Integer>>();
		datatypeColumns = new ArrayList<String>();
		resultsetColumns = new Vector<Column>();
		resultsetTableColumns1 = new HashMap<Table, Vector<String>>();
		resultsetTables = new ArrayList<Table>();
		stringSolver = new StringConstraintSolver();	
		branchQueries = new BranchQueriesDetails();
		tableNames = new HashMap<String, Integer[]>();
		equiJoins = new HashMap<String, Vector<Vector<Node>>>();
	}


	/** 
	 * This method initializes all the details about the given query whose details are stored in the query Parser
	 * @param qParser
	 */
	public void initializeQueryDetails (QueryParser queryParser) throws Exception{
		qParser = queryParser;
		query = qParser.getQuery();
		queryString = query.getQueryString();
		//currentIndex = query.getCurrentIndex();
		baseRelation = query.getBaseRelation();
		currentIndexCount = query.getCurrentIndexCount();
		repeatedRelationCount = query.getRepeatedRelationCount();

		/** Initialize the foreign key details*/
		foreignKeys = new ArrayList<Node>( qParser.getForeignKeys());
		foreignKeysModified = new ArrayList<ForeignKey>( qParser.getForeignKeyVectorModified());		

		/** Initiliaze the outer query block*/
		outerBlock = QueryBlockDetails.intializeQueryBlockDetails(queryParser);

		/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
		tableNames = qParser.getTableNames();

		/** Initialize each from clause nested sub query blocks */
		for(QueryParser qp: qParser.getFromClauseSubqueries())
			outerBlock.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );

		/** Initialize the where clause nested sub query blocks */
		for(QueryParser qp: qParser.getWhereClauseSubqueries())
			outerBlock.getWhereClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );

	}


	public void initializeOtherDetails() throws Exception{

		/**Update the  base relations in each block of the query*/
		RelatedToPreprocessing.getRelationOccurredInEachQueryBlok(this);

		/**Sort the foreign keys based on topological sorting of foreign keys*/
		RelatedToPreprocessing.sortForeignKeys(this);

		/**Generate CVC3 Header, This is need to initialize the CVC3 Data Type field of each column of each table */
		this.setCVC3_HEADER( GetCVC3HeaderAndFooter.generateCVC3_Header(this) );
	}


	/**
	 * Initializes the elements necessary for data generation
	 * Call this function after the previous data generation has been done and 
	 * constraints for the current data generation have not been added
	 */
	public void inititalizeForDataset() throws Exception{

		constraints = new ArrayList<String>();
		stringConstraints = new ArrayList<String>();
		CVCStr = "";
		typeOfMutation = "";

		/** initialize the no of output tuples*/
		noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();

		/**Merging noOfOutputTuples, if input query has set operations*/
		if(qParser.isUnion || qParser.isIntersectOrExcept){  

			/**Initialize the number of tuples in left side query of the set operation*/
			noOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCleft().query.getRepeatedRelationCount().clone();

			/**Now get the no of tuples for each relation on right side query of the set operation and add to the data structure*/
			HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCright().query.getRepeatedRelationCount().clone();

			/**get iterator*/
			Iterator rt=RightnoOfOutputTuples.entrySet().iterator();

			/**while there are values in the hash map*/
			while(rt.hasNext()){
				Map.Entry pairs=(Entry) rt.next();

				/**get table name*/
				String table=(String) pairs.getKey();

				/**get the number of tuples*/
				int noOfTuples = (Integer) pairs.getValue();

				/**Update the data structure*/
				if(noOfOutputTuples.containsKey(table)&&noOfOutputTuples.get(table)<noOfTuples){
					noOfOutputTuples.put(table, noOfTuples);
				}
				if(!noOfOutputTuples.containsKey(table)){
					noOfOutputTuples.put(table, noOfTuples);
				}
			}
		}

		/**If there are no set operations in the input query*/
		if(!qParser.isUnion &&  !qParser.isIntersectOrExcept )
			this.noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();

		for(String tempTable : noOfOutputTuples.keySet())
			if(noOfOutputTuples.get(tempTable) != null && noOfOutputTuples.get(tempTable) >= 1)
				System.out.println("START COUNT for " + tempTable + " = " + noOfOutputTuples.get(tempTable));

		repeatedRelNextTuplePos = new HashMap<String, Integer[]>();

		/** Update repeated relation next position etc..*/
		Iterator<String> itr = repeatedRelationCount.keySet().iterator();
		while(itr.hasNext()){
			String tableName = itr.next();
			int c = repeatedRelationCount.get(tableName);
			for(int i=1;i<=c;i++){
				Integer[] tuplePos = new Integer[32];
				tuplePos[1] = i;//Meaning first tuple is at pos i
				repeatedRelNextTuplePos.put(tableName+i, tuplePos);
				noOfTuples.put(tableName+i, 1);
				currentIndexCount.put(tableName+i, i);
			}
		}

		/** Initializes the data structures that are used/updated by the tuple assignment method*/
		initilizeDataStructuresForTupleAssignment(outerBlock);
		for(QueryBlockDetails qbt: getOuterBlock().getFromClauseSubQueries())
			initilizeDataStructuresForTupleAssignment(qbt);
		for(QueryBlockDetails qbt: getOuterBlock().getWhereClauseSubQueries())
			initilizeDataStructuresForTupleAssignment(qbt);
		
		/**get the list of equi join conditions for each table in the query*/
		GenerateCVC1.getListOfEquiJoinConditions( this );
	}

	/**
	 * This method initializes the data structures that are used by the tuple assignment method
	 * @param queryBlock
	 */
	public void initilizeDataStructuresForTupleAssignment(QueryBlockDetails queryBlock){

		/** Add constraints related to parameters*/
		this.getConstraints().add(RelatedToParameters.addDatatypeForParameters( this, queryBlock));

		/**initialize other elements*/
		queryBlock.setUniqueElements(new HashSet<HashSet<Node>>());
		queryBlock.setUniqueElementsAdd(new HashSet<HashSet<Node>>());

		queryBlock.setSingleValuedAttributes(new HashSet<Node>());
		queryBlock.setSingleValuedAttributesAdd(new HashSet<Node>());

		queryBlock.setNoOfGroups(1);
		queryBlock.setFinalCount(0);

		queryBlock.setEquivalenceClassesKilled( new ArrayList<Node>());

	}
	/**
	 * Generates datasets to kill each type of mutation for the original query
	 * @throws Exception
	 */
	public static void generateDatasetsToKillMutations(GenerateCVC1 cvc) throws Exception {

		/**Generate data for the original query*/
		
		String mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock();
		
		GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc, mutationType);

		/**Generate data sets to kill mutations in outer query block */
		MutationsInOuterBlock.generateDataForKillingMutantsInOuterQueryBlock(cvc);

		/**Generate data sets  to kill mutations in from clause nested sub query blocks */
		MutationsInFromSubQuery.generateDataForKillingMutantsInFromSubQuery(cvc);

		/**Generate data sets  to kill mutations in where clause nested sub query blocks */
		MutationsInWhereSubQuery.generateDataForKillingMutantsInWhereSubQuery(cvc);
	}






	/**
	 * A wrapper method that is used to get the number of tuples for each base relation occurrence 
	 * in each block of the query
	 */
	public static boolean tupleAssignmentForQuery(GenerateCVC1 cvc) throws Exception{

		if( CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, cvc.getOuterBlock()) == false)
			return false;

		return getTupleAssignmentForSubQueries(cvc);

	}


	/**
	 * estimate the number of tuples for each relation in each sub query block
	 * @throws Exception
	 */
	public static boolean getTupleAssignmentForSubQueries(GenerateCVC1 cvc) throws Exception{

		/** flag to indicate whether tuple assignment is possible or not*/
		boolean possible ;

		/** get tuple assignment for each from clause sub query block*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries() ){

			possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qbt);

			/** If tuple assignment is not possible*/
			if(possible == false)
				return false;
		}

		/** get tuple assignment for each where clause sub query block*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getWhereClauseSubQueries()){

			possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qbt);

			/** If tuple assignment is not possible*/
			if(possible == false)
				return false;
		}
		/** For all blocks the tuple assignment is successful*/
		return true;

	}



	/**
	 * Gets the list of all equi join conditions on each column of each table 
	 */
	public static void getListOfEquiJoinConditions(GenerateCVC1 cvc) throws Exception{

		cvc.setEquiJoins( new HashMap<String, Vector<Vector<Node>>>());
		/**get list of equi joins in outer query block*/
		getListOfEquiJoinConditionsInQueryBlock(cvc, cvc.getOuterBlock());

		/**get list of join conditions in each from clause nested sub query block*/
		for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries())
			getListOfEquiJoinConditionsInQueryBlock(cvc, qb);

		/**get list of join conditions in each where clause nested sub query block*/
		for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries())
			getListOfEquiJoinConditionsInQueryBlock(cvc, qb);
	}

	/**
	 * Gets the list of equi join conditions in this query block
	 * @param cvc
	 * @param outerBlock2
	 */
	public static void getListOfEquiJoinConditionsInQueryBlock(	GenerateCVC1 cvc, QueryBlockDetails queryBlock) {

		/**for each conjunct*/
		for(Conjunct con: queryBlock.getConjuncts()){
			
			/**get the list of equi join conditions*/
			Vector<Vector<Node>> eqClass = con.getEquivalenceClasses();
			
			/**for every equivalence class*/
			for(Vector<Node> ec: eqClass){
				
				/**for every node in this equivalence class*/
				for(Node n: ec){
					
					String key =  n.getTable().getTableName() ;
					/**if this relation is present in the hash map*/
					if( cvc.getEquiJoins().containsKey(key) ){
						
						/**add this equivalence class to the list, if already not added*/
						if( !cvc.getEquiJoins().get(key).contains(ec) ){
							
							cvc.getEquiJoins().get(key).add(ec);
						}
					}
					else{
						
						Vector< Vector< Node >> eq = new Vector<Vector<Node>>();
						eq.add(ec);
						cvc.getEquiJoins().put(key, eq);
					}
				}
			}
		}

	}


	/**Below are the setters and getters for the variables of this class */
	public TableMap getTableMap() {
		return tableMap;
	}

	public void setTableMap(TableMap tableMap) {
		this.tableMap = tableMap;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public Query getTopQuery() {
		return topQuery;
	}

	public void setTopQuery(Query topQuery) {
		this.topQuery = topQuery;
	}

	public QueryParser getqParser() {
		return qParser;
	}

	public void setqParser(QueryParser qParser) {
		this.qParser = qParser;
	}

	public HashMap<String, String> getBaseRelation() {
		return baseRelation;
	}

	public void setBaseRelation(HashMap<String, String> baseRelation) {
		this.baseRelation = baseRelation;
	}

	public HashMap<String, Integer> getCurrentIndexCount() {
		return currentIndexCount;
	}

	public void setCurrentIndexCount(HashMap<String, Integer> currentIndexCount) {
		this.currentIndexCount = currentIndexCount;
	}

	public HashMap<String, Integer> getRepeatedRelationCount() {
		return repeatedRelationCount;
	}

	public void setRepeatedRelationCount(
			HashMap<String, Integer> repeatedRelationCount) {
		this.repeatedRelationCount = repeatedRelationCount;
	}

	public HashMap<String, Integer[]> getRepeatedRelNextTuplePos() {
		return repeatedRelNextTuplePos;
	}

	public void setRepeatedRelNextTuplePos(
			HashMap<String, Integer[]> repeatedRelNextTuplePos) {
		this.repeatedRelNextTuplePos = repeatedRelNextTuplePos;
	}

	public HashMap<String, Integer> getNoOfTuples() {
		return noOfTuples;
	}

	public void setNoOfTuples(HashMap<String, Integer> noOfTuples) {
		this.noOfTuples = noOfTuples;
	}

	public HashMap<String, Integer> getNoOfOutputTuples() {
		return noOfOutputTuples;
	}

	public void setNoOfOutputTuples(HashMap<String, Integer> noOfOutputTuples) {
		this.noOfOutputTuples = noOfOutputTuples;
	}

	public QueryBlockDetails getOuterBlock() {
		return outerBlock;
	}

	public void setOuterBlock(QueryBlockDetails outerBlock) {
		this.outerBlock = outerBlock;
	}


	public ArrayList<Node> getForeignKeys() {
		return foreignKeys;
	}


	public void setForeignKeys(ArrayList<Node> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}


	public ArrayList<ForeignKey> getForeignKeysModified() {
		return foreignKeysModified;
	}


	public void setForeignKeysModified(ArrayList<ForeignKey> foreignKeysModified) {
		this.foreignKeysModified = foreignKeysModified;
	}


	public ArrayList<String> getConstraints() {
		return constraints;
	}


	public void setConstraints(ArrayList<String> constraints) {
		this.constraints = constraints;
	}


	public ArrayList<String> getStringConstraints() {
		return stringConstraints;
	}


	public void setStringConstraints(ArrayList<String> stringConstraints) {
		this.stringConstraints = stringConstraints;
	}


	public String getCVCStr() {
		return CVCStr;
	}


	public void setCVCStr(String cVCStr) {
		CVCStr = cVCStr;
	}


	public HashMap<Table, Vector<String>> getResultsetTableColumns1() {
		return resultsetTableColumns1;
	}


	public void setResultsetTableColumns1(HashMap<Table, Vector<String>> resultsetTableColumns1) {
		this.resultsetTableColumns1 = resultsetTableColumns1;
	}


	public HashMap<Column, HashMap<String, Integer>> getColNullValuesMap() {
		return colNullValuesMap;
	}


	public void setColNullValuesMap(HashMap<Column, HashMap<String, Integer>> colNullValuesMap) {
		this.colNullValuesMap = colNullValuesMap;
	}

	public StringConstraintSolver getStringSolver() {
		return stringSolver;
	}


	public void setStringSolver(StringConstraintSolver stringSolver) {
		this.stringSolver = stringSolver;
	}


	public Vector<Column> getResultsetColumns() {
		return resultsetColumns;
	}


	public void setResultsetColumns(Vector<Column> resultsetColumns) {
		this.resultsetColumns = resultsetColumns;
	}


	public boolean isFne() {
		return fne;
	}


	public void setFne(boolean fne) {
		this.fne = fne;
	}


	public ArrayList<String> getDatatypeColumns() {
		return datatypeColumns;
	}


	public void setDatatypeColumns(ArrayList<String> datatypeColumns) {
		this.datatypeColumns = datatypeColumns;
	}


	public String getOutput() {
		return output;
	}


	public void setOutput(String output) {
		this.output = output;
	}


	public String getQueryString() {
		return queryString;
	}


	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}


	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public int getCount() {
		return count;
	}


	public void setCount(int count) {
		this.count = count;
	}


	public ArrayList<Table> getResultsetTables() {
		return resultsetTables;
	}


	public void setResultsetTables(ArrayList<Table> resultsetTables) {
		this.resultsetTables = resultsetTables;
	}


	public boolean isIpdb() {
		return ipdb;
	}


	public void setIpdb(boolean ipdb) {
		this.ipdb = ipdb;
	}


	public String getCVC3_HEADER() {
		return CVC3_HEADER;
	}


	public void setCVC3_HEADER(String cVC3_HEADER) {
		CVC3_HEADER = cVC3_HEADER;
	}


	public GenerateUnionCVC getUnionCVC() {
		return unionCVC;
	}


	public void setUnionCVC(GenerateUnionCVC unionCVC) {
		this.unionCVC = unionCVC;
	}

	/**
	 * This function is used to update the total number of output tuples data structure,
	 * @param queryBlock
	 * @param noOfGroups: Specifies the number of groups to be generated by this query block
	 */
	public  void updateTotalNoOfOutputTuples(QueryBlockDetails queryBlock, int noOfGroups) {

		/**for each base relation in the query block*/
		for(String tableNameNo: queryBlock.getBaseRelations()){

			/**Indicates the count of relation*/
			int prevCount, prevTotCount;

			if( noOfTuples.get( tableNameNo ) != null){

				/**get the count*/
				prevCount = noOfTuples.get(tableNameNo);

				/**total count contributed by this relation*/
				prevTotCount = prevCount * queryBlock.getNoOfGroups();			


				/**get the new total count contributed by this relation*/
				int totCount = prevCount * noOfGroups;

				/**get table name */
				String tableName = tableNameNo.substring(0, tableNameNo.length()-1);

				/**update the total number of output tuples data structre*/
				if( noOfOutputTuples.get(tableName) == null)
					noOfOutputTuples.put(tableNameNo, totCount );
				else
					noOfOutputTuples.put(tableName, noOfOutputTuples.get(tableName)+ totCount - prevTotCount );
			}
		}

		/**Update the number of groups*/
		queryBlock.setNoOfGroups(noOfGroups);

	}


	public BranchQueriesDetails getBranchQueries() {
		return branchQueries;
	}


	public void setBranchQueries(BranchQueriesDetails branchQueries) {
		this.branchQueries = branchQueries;
	}


	public Vector<Table> getTablesOfOriginalQuery() {
		return tablesOfOriginalQuery;
	}


	public void setTablesOfOriginalQuery(Vector<Table> tablesOfOriginalQuery) {
		this.tablesOfOriginalQuery = tablesOfOriginalQuery;
	}


	public String getTypeOfMutation() {
		return typeOfMutation;
	}


	/**sets the type of mutation we are trying to kill*/
	public void setTypeOfMutation(MutationType mutationType, QueryBlock queryBlock) {

		this.typeOfMutation = mutationType.getMutationType() + queryBlock.getQueryBlock();
	}


	public void setTypeOfMutation(String typeOfMutation) {
		this.typeOfMutation = typeOfMutation;
	}


	public HashMap<String, Integer[]> getTableNames() {
		return tableNames;
	}


	public void setTableNames(HashMap<String, Integer[]> tableNames) {
		this.tableNames = tableNames;
	}


	public HashMap<String, Vector<Vector<Node>>> getEquiJoins() {
		return equiJoins;
	}


	public void setEquiJoins(HashMap<String, Vector<Vector<Node>>> equiJoins) {
		this.equiJoins = equiJoins;
	}


}
