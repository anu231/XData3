package killMutations;

import killMutations.outerQueryBlock.*;
import testDataGen.GenerateCVC1;

/**
 * This class generates data sets for killing each type of mutation in outer query block
 * @author mahesh
 *
 */
public class MutationsInOuterBlock {

	public static void generateDataForKillingMutantsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{

		System.out.println("\n----------------------------------");
		System.out.println("MUTANTS  IN OUTER QUERY BLOCK QUERY");
		System.out.println("----------------------------------\n");


		/**killing equivalence class mutations in in outer query blocks*/
		EquivalenceMutationInOuterQueryBlock.generateDataForkillingEquivalenceClassMutationsInOuterQueryBlock(cvc);

		/** killing join predicate mutations in outer query blocks */
		JoinMutationsInOuterQueryBlock.generateDataForkillingJoinMutationsInOuterQueryBlock(cvc);


		/** killing selection mutations in outer query blocks*/
		SelectionMutationsInOuterQueryBlock.generateDataForkillingSelectionMutationsInOuterQueryBlock(cvc);

		/** killing string selection mutations in outer query blocks*/
		StringSelectionMutationsInOuterQueryBlock.generateDataForkillingStringSelectionMutationsInOuterQueryBlock(cvc);

		/** killing like mutations in outer query blocks*/
		LikeMutationsInOuterQueryBlock.generateDataForkillingLikeMutationsInOuterQueryBlock(cvc);

		/** killing null mutations in outer query blocks*/
		//NullMutationsInOuterBlock.generateDataForkillingSelectionMutationsInOuterQueryBlock(cvc);
		
		/** killing aggregate function mutations in outer query blocks*/
		AggMutationsInOuterQueryBlock.generateDataForkillingAggMutationsInOuterQueryBlock(cvc);

		/** killing where clause sub query connective mutations in outer query blocks*/
		WhereClauseSubQueryConnectiveMutations.killWhereClauseSubQueryConnectiveMutations(cvc);

		/** killing having clause mutations in outer query blocks*/
		ConstrainedAggregationMutationsInOuterQueryBlock.generateDataForkillingConstrainedAggregationInOuterQueryBlock(cvc);

		/** kill distinct mutations in outer query blocks*/
		DistinctMutationsInOuterQueryBlock.generateDataForkillingDistinctMutationsInOuterQueryBlock(cvc);

		/** partial group by mutations in outer query blocks*/
		PartialGroupByMutationsInOuterQueryBlock_case1.generateDataForkillingParialGroupByMutationsInOuterQueryBlock(cvc);

		/** Killing partial group by mutations in outer query blocks*/
		PartialGroupByMutationsInOuterQueryBlock_case2.generateDataForkillingParialGroupByMutationsInOuterQueryBlock(cvc);

		/** Killing extra group by attribute mutations in outer query blocks */
		ExtraGroupByMutationsInOuterQueryBlock.generateDataForkillingExtraGroupByMutationsInOuterQueryBlock(cvc);

		/** Killing common name mutations in outer query blocks*/
		UnintendedJoinsMutationsInOuterQueryBlock.generateDataForkillingUnintendedJoinsMutationsInOuterQueryBlock(cvc);
	}
}
