package se.uu.it.basiclearning;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.oracle.EquivalenceOracle;
import net.automatalib.automaton.Automaton;

public abstract class LearningSetup<I, D, M extends Automaton<?, I, ?>> {
	
	public abstract EquivalenceOracle<M, I, D> getEquivalenceOracle();
	
	public abstract LearningAlgorithm<M, I, D> getLearningAlgorithm();
	
	public abstract Counter getResetCounter();
	
	public abstract Counter getSymbolCounter();
}
