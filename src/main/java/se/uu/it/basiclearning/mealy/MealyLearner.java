package se.uu.it.basiclearning.mealy;

import java.util.Random;

import com.google.common.collect.Lists;

import de.learnlib.acex.AcexAnalyzers;
import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.kv.mealy.KearnsVaziraniMealy;
import de.learnlib.algorithm.lstar.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithm.lstar.closing.ClosingStrategies;
import de.learnlib.algorithm.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithm.ttt.mealy.TTTLearnerMealy;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.filter.statistic.sul.CounterSUL;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.oracle.equivalence.MealyWMethodEQOracle;
import de.learnlib.oracle.equivalence.MealyWpMethodEQOracle;
import de.learnlib.oracle.equivalence.RandomWpMethodEQOracle;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.sul.SUL;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.transducer.MealyMachine;
import net.automatalib.word.Word;
import se.uu.it.basiclearning.BasicLearner;
import se.uu.it.basiclearning.LearnerConfig;
import se.uu.it.basiclearning.LearningSetup;

public class MealyLearner<I, O> extends BasicLearner<I, Word<O>, MealyMachine<?, I, ?, O>> {
	private SUL<I, O> sul;

	public MealyLearner(LearnerConfig config, SUL<I, O> sut) {
		super(config);
		this.sul = sut;

	}

	private LearningAlgorithm<MealyMachine<?, I, ?, O>, I, Word<O>> loadLearner(MealyMembershipOracle<I, O> sulOracle,
			Alphabet<I> alphabet) {
		switch (config.getLearningMethod()) {
		case LStar:
			return new ExtensibleLStarMealy<I, O>(alphabet, sulOracle, Lists.<Word<I>>newArrayList(),
					ObservationTableCEXHandlers.CLASSIC_LSTAR, ClosingStrategies.CLOSE_SHORTEST);
		case RivestSchapire:
			return new ExtensibleLStarMealy<I, O>(alphabet, sulOracle, Lists.<Word<I>>newArrayList(),
					ObservationTableCEXHandlers.RIVEST_SCHAPIRE, ClosingStrategies.CLOSE_SHORTEST);
		case TTT:
			return new TTTLearnerMealy<I, O>(alphabet, sulOracle, AcexAnalyzers.LINEAR_FWD);
		case KearnsVazirani:
			return new KearnsVaziraniMealy<I, O>(alphabet, sulOracle, false, AcexAnalyzers.LINEAR_FWD);
		default:
			throw new RuntimeException("No learner selected");
		}
	}

	private EquivalenceOracle<MealyMachine<?, I, ?, O>, I, Word<O>> loadTester(SUL<I, O> sul,
			MealyMembershipOracle<I, O> sulOracle) {
		switch (config.getTestingMethod()) {
		// simplest method, but doesn't perform well in practice, especially for large
		// models
		case RandomWalk:
			return new RandomWalkEQOracle<>(sul, config.getResetProbability(), config.getMaxSymbols(), true,
					new Random(config.getSeed()));
		// Other methods are somewhat smarter than random testing: state coverage,
		// trying to distinguish states, etc.
		case RandomWpMethod:
			return new RandomWpMethodEQOracle<>(sulOracle, config.getMinSize(), config.getRandLength(), config.getMaxTests());
		case WMethod:
			return new MealyWMethodEQOracle<>(sulOracle, config.getMaxDepth());
		case WpMethod:
			return new MealyWpMethodEQOracle<>(sulOracle, config.getMaxDepth());
		default:
			throw new RuntimeException("No test oracle selected!");
		}
	}

	@Override
	protected MealyLearningSetup buildLearningSetup(Alphabet<I> alphabet) {
		return new MealyLearningSetup(sul, alphabet);
	}

	/**
	 * Helper class to configure a learning and equivalence oracle. Tell it which
	 * learning and testing method you want, and it produces the corresponding
	 * oracles (and counters for statistics) as attributes.
	 */
	public class MealyLearningSetup extends LearningSetup<I, Word<O>, MealyMachine<?, I, ?, O>> {
		public final EquivalenceOracle<MealyMachine<?, I, ?, O>, I, Word<O>> eqOracle;
		public final LearningAlgorithm<MealyMachine<?, I, ?, O>, I, Word<O>> learner;
		public Counter nrSymbols, nrResets;

		public MealyLearningSetup(SUL<I, O> sul, Alphabet<I> alphabet) {
			// Wrap the SUL in a detector for non-determinism
			SUL<I, O> nonDetSul = new NonDeterminismCheckingSUL<I, O>(sul);
			// Wrap the SUL in counters for symbols/resets, so that we can record some
			// statistics
			CounterSUL<I, O> counterSul = new CounterSUL<I, O>(nonDetSul);
			nrSymbols = counterSul.getSymbolCounter();
			nrResets = counterSul.getResetCounter();
			// we should use the sul only through those wrappers
			sul = counterSul;
			// Most testing/learning-algorithms want a membership-oracle instead of a SUL
			// directly
			MealyMembershipOracle<I, O> sulOracle = new SULOracle<>(sul);

			// Choosing an equivalence oracle
			eqOracle = loadTester(sul, sulOracle);

			// Choosing a learner
			learner = loadLearner(sulOracle, alphabet);
		}

		@Override
		public EquivalenceOracle<MealyMachine<?, I, ?, O>, I, Word<O>> getEquivalenceOracle() {
			return eqOracle;
		}

		@Override
		public LearningAlgorithm<MealyMachine<?, I, ?, O>, I, Word<O>> getLearningAlgorithm() {
			return learner;
		}

		@Override
		public Counter getResetCounter() {
			// TODO Auto-generated method stub
			return nrResets;
		}

		@Override
		public Counter getSymbolCounter() {
			return nrSymbols;
		}
	}
}
