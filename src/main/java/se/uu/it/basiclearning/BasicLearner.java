package se.uu.it.basiclearning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.ListAlphabet;
import net.automatalib.automaton.Automaton;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.dot.DOT;

/**
 * General learning testing framework. All basic settings are at the top of this file and can be configured
 * by hard-coding or by simply changing them from your own code. Method "runSimpleExperiment" learns a model and writes
 * it to a file. Method "runControlledExperiment" shows extra statistics and intermediate hypotheses, which you can
 * customize.
 * 
 * Based on the learner experiment setup of Joshua Moerman, https://gitlab.science.ru.nl/moerman/Learnlib-Experiments
 * 
 * @author Ramon Janssen, Paul Fiterau
 */
public abstract class BasicLearner<I, D, M extends Automaton<?, I, ?>> {
	//***********************************************************************************//
 	// Learning settings (hardcoded, simply set to a different value to change learning) //
	//***********************************************************************************//
	/**
	 * name to give to the resulting .dot-file and .pdf-file (extensions are added automatically)
	 */
	public static String // extension .pdf is added automatically
			FINAL_MODEL_FILENAME = "learnedModel",
			INTERMEDIATE_HYPOTHESIS_FILENAME = "hypothesis"; // a number gets appended for every iteration
	protected LearnerConfig config;
	
	private File outputFolder;
	
	public BasicLearner(LearnerConfig config) {
		this.config = config;
		outputFolder = new File(config.getOutputDir());
	}
	
	public LearnerConfig getConfig() {
		return config;
	}
	
	/**
	 * Method for running a learning experiment. Starts learning, and then loops testing,
	 * and if counterexamples are found, refining again. Also prints some statistics about the experiment
	 * @param learner learner Learning algorithm, wrapping the SUL
	 * @param eqOracle Testing algorithm, wrapping the SUL
	 * @param nrSymbols A counter for the number of symbols that have been sent to the SUL (for statistics)
	 * @param nrResets A counter for the number of resets that have been sent to the SUL (for statistics)
	 * @param symbols Symbols forming the input alphabet
	 * @throws IOException
	 */
	public void runExperiment(Collection<I> symbols) throws IOException {
		if (!outputFolder.exists()) {
			outputFolder.mkdirs();
		}
		LearningAlgorithm<M, I, D> learner = null;
		Alphabet<I> alphabet = new ListAlphabet<>(new ArrayList<>(symbols));
		try {
			LearningSetup<I, D, M> learningSetup = buildLearningSetup(alphabet);
			learner = learningSetup.getLearningAlgorithm();
			EquivalenceOracle<M, I, D> eqOracle = learningSetup.getEquivalenceOracle();
			Counter resetCounter = learningSetup.getResetCounter();
			Counter symbolCounter = learningSetup.getSymbolCounter();
			
			
			// prepare some counters for printing statistics
			int stage = 0;
			long lastNrResetsValue = 0, lastNrSymbolsValue = 0;
			
			// learn the first hypothesis
			learner.startLearning();
			
			while(true) {
				// store hypothesis as file
				String outputFilename = INTERMEDIATE_HYPOTHESIS_FILENAME + stage;
				produceOutput(outputFilename, learner.getHypothesisModel(), alphabet, true, false);
				System.out.println("model size " + learner.getHypothesisModel().getStates().size());
	
				// Print statistics
				System.out.println(stage + ": " + Calendar.getInstance().getTime());
				// Log number of queries/symbols
				System.out.println("Hypothesis size: " + learner.getHypothesisModel().size() + " states");
				long roundResets = resetCounter.getCount() - lastNrResetsValue, roundSymbols = symbolCounter.getCount() - lastNrSymbolsValue;
				System.out.println("learning queries/symbols: " + resetCounter.getCount() + "/" + symbolCounter.getCount()
						+ "(" + roundResets + "/" + roundSymbols + " this learning round)");
				lastNrResetsValue = resetCounter.getCount();
				lastNrSymbolsValue = symbolCounter.getCount();
				
				// Search for CE
				@Nullable
				DefaultQuery<I, D> ce = eqOracle.findCounterExample(learner.getHypothesisModel(), alphabet);
				
				// Log number of queries/symbols
				roundResets = resetCounter.getCount() - lastNrResetsValue;
				roundSymbols = symbolCounter.getCount() - lastNrSymbolsValue;
				System.out.println("testing queries/symbols: " + resetCounter.getCount() + "/" + symbolCounter.getCount()
						+ "(" + roundResets + "/" + roundSymbols + " this testing round)");
				lastNrResetsValue = resetCounter.getCount();
				lastNrSymbolsValue = symbolCounter.getCount();
				
				if(ce == null) {
					// No counterexample found, stop learning
					System.out.println("\nFinished learning!");
					produceOutput(FINAL_MODEL_FILENAME, learner.getHypothesisModel(), alphabet, true, true);
					break;
				} else {
					// Counterexample found, rinse and repeat
					System.out.println();
					stage++;
					learner.refineHypothesis(ce);
				}
			}
		} catch (Exception e) {
			String errorHypName = "hyp.before.crash.dot";
			produceOutput(errorHypName, learner.getHypothesisModel(), alphabet, true, true);
			throw e;
		}
	}
	
	protected abstract LearningSetup<I, D, M> buildLearningSetup(Alphabet<I> alphabet);

	//************************//
	// Some auxiliary methods //
	//************************//
	/*
	 * Produces a dot-file and a PDF (if graphviz is installed)
	 * @param fileName filename without extension - will be used for the .dot and .pdf
	 * @param model
	 * @param alphabet
	 * @param verboseError whether to print an error explaing that you need graphviz
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void produceOutput(String fileName, Automaton<?, I, ?> model, Alphabet<I> alphabet, boolean producePdf, boolean verboseError) throws FileNotFoundException, IOException {
		File file = new File(outputFolder, fileName + ".dot");
		PrintWriter dotWriter = new PrintWriter(file);
		GraphDOT.write(model, alphabet, dotWriter);
		if (producePdf) {
			try {
				DOT.runDOT(file, "pdf", new File(outputFolder, fileName + ".pdf"));
			} catch (Exception e) {
				if (verboseError) {
					System.err.println("Warning: Install graphviz to convert dot-files to PDF");
					System.err.println(e.getMessage());
				}
			}
		}
		dotWriter.close();
	}
}
