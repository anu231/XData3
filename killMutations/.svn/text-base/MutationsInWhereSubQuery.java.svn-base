package killMutations;

import killMutations.whereClauseNestedBlock.*;
import testDataGen.GenerateCVC1;

/**
 * This class generates data sets for killing each type of mutation inside each where clause nested sub query block
 * @author mahesh
 *
 */
public class MutationsInWhereSubQuery {

	public static void generateDataForKillingMutantsInWhereSubQuery(GenerateCVC1 cvc) throws Exception{

		System.out.println("\n----------------------------------");
		System.out.println("Where SUBQUERY MUTANTS  IN WHERE CLAUSE NESTED SUBQUERY BLOCK QUERY");
		System.out.println("----------------------------------\n");

		MutationsInNotExistsSubquery.genDataToKillMutantsInNotExistsSubquery(cvc);

		/**killing equivalence class mutations in Where clause nested sub query blocks*/
		EquivalenceMutationInWhereSubQuery.generateDataForkillingEquivalenceClassMutationsInWhereSubquery(cvc);

		/** killing join predicate mutations in Where clause nested sub query blocks */
		JoinMutationsInWhereSubQuery.generateDataForkillingJoinMutationsInWhereSubquery(cvc);


		/** killing selection mutations in Where clause nested sub query blocks*/
		SelectionMutationsInWhereSubquery.generateDataForkillingSelectionMutationsInWhereSubquery(cvc);

		/** killing string selection mutations in Where clause nested sub query blocks*/
		StringSelectionMutationsInWhereSubquery.generateDataForkillingStringSelectionMutationsInWhereSubquery(cvc);

		/** killing like mutations in Where clause nested sub query blocks*/
		LikeMutationsInWhereSubquery.generateDataForkillingLikeMutationsInWhereSubquery(cvc);

		/** killing aggregate function mutations in Where clause nested sub query blocks*/
		AggMutationsInWhereSubQuery.generateDataForkillingAggMutationsInWhereSubquery(cvc);

		/** killing having clause mutations in Where clause nested sub query blocks*/
		ConstrainedAggregationMutationsInWhereSubQuery.generateDataForkillingConstrainedAggregationInWhereSubquery(cvc);

		/** kill distinct mutations in Where clause nested sub query blocks*/
		DistinctMutationsInWhereSubQuery.generateDataForkillingDistinctMutationsInWhereSubquery(cvc);

		/** partial group by mutations in Where clause nested sub query blocks*/
		PartialGroupByMutationsInWhereSubQuery_case1.generateDataForkillingParialGroupByMutationsInWhereSubquery(cvc);

		/** Killing partial group by mutations in Where clause nested sub query blocks*/
		PartialGroupByMutationsInWhereSubQuery_case2.generateDataForkillingParialGroupByMutationsInWhereSubquery(cvc);

		/** Killing extra group by attribute mutations in Where clause nested sub query blocks */
		ExtraGroupByMutationsInWhereSubQuery.generateDataForkillingExtraGroupByMutationsInWhereSubquery(cvc);

		/** Killing common name mutations in Where clause nested sub query blocks*/
		UnintendedJoinsMutationsInWhereSubQuery.generateDataForkillingUnintendedJoinsMutationsInWhereSubquery(cvc);
	}
}
