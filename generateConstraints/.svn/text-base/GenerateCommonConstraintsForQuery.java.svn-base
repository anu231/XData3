package generateConstraints;

import java.util.*;

import parsing.Column;
import parsing.Conjunct;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.PopulateTestData;
import testDataGen.QueryBlockDetails;
import testDataGen.WriteFileAndUploadDatasets;
import util.Configuration;
import util.TableMap;
import util.TagDatasets;

/**
 * Generates constraints related to null conditions and database constraints
 * @author mahesh
 *
 */
public class GenerateCommonConstraintsForQuery {

	public static void generateDataSetForConstraints(GenerateCVC1 cvc) throws Exception{

		/** Add null constraints for the query */
		getNullConstraintsForQuery(cvc);

		/** Solve the string constraints for the query */
		if(!cvc.getStringConstraints().isEmpty()) {
			cvc.getConstraints().add("\n%---------------------------------\n% TEMP VECTOR CONSTRAINTS\n%---------------------------------\n");
			Vector<String> tempVector = cvc.getStringSolver().solveOrConstraints( new Vector<String>(cvc.getStringConstraints()), cvc.getResultsetColumns(), cvc.getTableMap());		
			//if(cvc.getTypeOfMutation().equalsIgnoreCase(TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock()))
			cvc.getConstraints().addAll(tempVector);
				
		}

		if( cvc.getCVCStr() == null)
			cvc.setCVCStr("");


		String CVCStr = cvc.getCVCStr();

		/**Add constraints related to database */
		CVCStr += AddDataBaseConstraints.addDBConstraints(cvc);



		/** Add not null constraints */
		cvc.getConstraints().add("\n%---------------------------------\n% NOT NULL CONSTRAINTS\n%---------------------------------\n");
		cvc.getConstraints().add( GenerateCVCConstraintForNode.cvcSetNotNull(cvc));

		/** Add constraints, if there are branch queries*/
		if( cvc.getBranchQueries().getBranchQuery() != null)
		{
			cvc.getConstraints().add("\n%---------------------------------\n%BRANCHQUERY CONSTRAINTS\n%---------------------------------\n");
			cvc.getConstraints().add( GenerateConstraintsRelatedToBranchQuery.addBranchQueryConstraints( cvc ));
			cvc.getConstraints().add("\n%---------------------------------\n%END OF BRANCHQUERY CONSTRAINTS\n%---------------------------------\n");
		}

		for(int k=0; k < cvc.getConstraints().size(); k++){
			CVCStr += "\n" + cvc.getConstraints().get(k);
		}

		cvc.setDatatypeColumns( new ArrayList<String>() );

		String CVC3_HEADER = GetCVC3HeaderAndFooter.generateCVC3_Header(cvc);

		CVCStr += GetCVC3HeaderAndFooter.generateCvc3_Footer();

		/** add mutation type and CVC3 header*/
		CVCStr = "%--------------------------------------------\n\n%MUTATION TYPE: " + cvc.getTypeOfMutation() +"\n\n%--------------------------------------------\n\n\n\n" + CVC3_HEADER + CVCStr;
		
		cvc.setCVCStr(CVCStr);

		/** Add extra tuples to satisfy branch queries constraints*/
		for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++){
			
			HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();
			
			for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
				
				cvc.getNoOfOutputTuples().put(tempTab.getTableName(), cvc.getNoOfOutputTuples().get(tempTab.getTableName()) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
		}


		/** Call CVC3 Solver with constraints */
		System.out.println("cvc count =="+cvc.getCount());
		WriteFileAndUploadDatasets.writeFile(Configuration.homeDir + "/temp_cvc" + cvc.getFilePath() + "/cvc3_" + cvc.getCount() + ".cvc", CVCStr);
		cvc.setOutput( cvc.getOutput() + new PopulateTestData().killedMutants("cvc3_" + cvc.getCount() + ".cvc", cvc.getQuery(), "DS" + cvc.getCount(), cvc.getQueryString(), cvc.getFilePath(), cvc.getNoOfOutputTuples(), cvc.getTableMap(), cvc.getResultsetColumns()) );
		cvc.setCount(cvc.getCount() + 1);



		/** remove extra tuples for Branch query */		
		for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++){
			
			HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();
			
			for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
				
				cvc.getNoOfOutputTuples().put(tempTab.getTableName(), cvc.getNoOfOutputTuples().get(tempTab.getTableName()) - noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
		}
			
	}


	/**
	 * Used to get null constraints for each block of the input query
	 * @param cvc
	 */
	public static void getNullConstraintsForQuery(GenerateCVC1 cvc) throws Exception{

		/**Generate null constraints for outer query block */
		cvc.getConstraints().add( "\n%---------------------------------\n%NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY\n%---------------------------------\n" );
		cvc.getConstraints().add( getNullCOnstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );
		cvc.getConstraints().add( "\n%---------------------------------\n%END OF NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY\n%---------------------------------\n" );

		/**Generate null constraints for each from clause sub query block */
		for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getFromClauseSubQueries()){

			cvc.getConstraints().add( "\n%---------------------------------\n%NULL CONSTRAINTS FOR FROM CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
			cvc.getConstraints().add( getNullCOnstraintsForQueryBlock(cvc, queryBlock) );
			cvc.getConstraints().add( "\n%---------------------------------\n%END OF NULL CONSTRAINTS FOR FROM CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
		}

		/**Generate null constraints for each where clause sub query block */
		for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getWhereClauseSubQueries()){

			cvc.getConstraints().add( "\n%---------------------------------\n%NULL CONSTRAINTS FOR WHERE CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
			cvc.getConstraints().add( getNullCOnstraintsForQueryBlock(cvc, queryBlock) );
			cvc.getConstraints().add( "\n%---------------------------------\n%END OF NULL CONSTRAINTS FOR WHERE CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
		}
	}

	/**
	 * Used to get null constraints for given query block
	 * @param cvc
	 * @param queryBlock
	 * @return
	 */
	public static String getNullCOnstraintsForQueryBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

		String ConstraintString = "";


		ArrayList< Node > isNullConds = new ArrayList<Node>();
		/** Get constraints for each conjunct*/
		for(Conjunct conjunct : queryBlock.getConjuncts()){

			/**Get null conditions in this conjunct*/
			isNullConds.addAll( new ArrayList<Node>(conjunct.getIsNullConds()) );

			/** for each node in the null conditions */
			for(Node n:isNullConds){

				int noOfTuples = cvc.getNoOfTuples().get(n.getLeft().getTableNameNo()) * queryBlock.getNoOfGroups();
				int offset = cvc.getRepeatedRelNextTuplePos().get(n.getLeft().getTableNameNo())[1];
				for(int i=0; i < noOfTuples; i++)
					if(n.getOperator().equals("="))
						ConstraintString += GenerateCVCConstraintForNode.cvcSetNull(cvc, n.getLeft().getColumn(), (offset+i)+"");

			}
		}
		return ConstraintString;
	}
}
