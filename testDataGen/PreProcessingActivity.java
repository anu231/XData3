package testDataGen;

import generateConstraints.GetCVC3HeaderAndFooter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;

import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;
import parsing.ParseSQL;
import parsing.Query;
import parsing.QueryParser;
import util.Configuration;
import util.TableMap;

/**
 * This class contains functions to do pre processing actions before actual data generation is done
 * @author mahesh
 *
 */
public class PreProcessingActivity {

	public static void preProcessingActivity(GenerateCVC1 cvc) throws Exception{


		/** check if there are branch queries and upload the details */
		RelatedToPreprocessing.uploadBranchQueriesDetails(cvc);


		/** To store input query string */
		String queryString = "";
		BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/queries.txt"));
		try {

			/**Read the input query */
			while (( queryString = input.readLine()) != null){

				/****************************/
				//using JSQLPARSER
				QueryParser q = new QueryParser(TableMap.getInstances());
				ParseSQL.parseQuery(queryString, "1",q);
				cvc.setqParser(ParseSQL.cxt.origQP);
				/****************************/
				
				/**Create a new query parser*/
				//cvc.setqParser( new QueryParser(cvc.getTableMap()));

				/** Parse the query */
				//cvc.getqParser().parseQuery("q1", queryString);

				/**Initialize the query details to the object*/
				cvc.initializeQueryDetails(cvc.getqParser() );

				/**Delete data sets in the path*/
				RelatedToPreprocessing.deleteDatasets(cvc.getFilePath());

				/**Check if the input query contains set operators */
				if( cvc.getqParser().isUnion || cvc.getqParser().isIntersectOrExcept){

					/**create object for handling set operations*/
					cvc.setUnionCVC(new GenerateUnionCVC(cvc, cvc.getqParser()) );

					RelatedToPreprocessing.populateData(cvc);

					/**Generate CVC3 Header, This is need to initialize the CVC3 Data Type field of each column of each table */
					cvc.setCVC3_HEADER( GetCVC3HeaderAndFooter.generateCVC3_Header( cvc ) );

					cvc.getUnionCVC().generateDataForApp();

					cvc.getUnionCVC().generateToKill();

					continue;
				}

				cvc.getBranchQueries().intitializeDetails(cvc);

				/**Populate the values from the data base 
				 * Needed so that the generated values looks realistic */	
				RelatedToPreprocessing.populateData(cvc);

				/**Initialize cvc3 headers etc>,*/				
				cvc.initializeOtherDetails();

				/** Segregate selection conditions in each query block */
				RelatedToPreprocessing.segregateSelectionConditions(cvc);


				/** Call the method for generating the data sets */
				cvc.generateDatasetsToKillMutations(cvc);

			}
		}
		finally {
			input.close();
		}
	}
}
