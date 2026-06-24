package se.uu.it.basiclearning.mealy;

import java.io.IOException;
import java.util.Collection;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableSet;

import de.learnlib.sul.SUL;
import se.uu.it.basiclearning.LearnerConfig;

/**
 * Created by ramon on 13-12-16. Adapter by pfg666 some time later.
 */
public class ExampleMealyExperiment {
    /**
     * Example of how to call a learner in a simple way with this class. Learns the ExampleSUL.
     * Supply the option "-h" to show usage page.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String [] args) throws IOException {
        // Load the actual SUL-class
        // For a SUL over a socket, use the SocketSUL-class
        // You can also program an own SUL-class if you extend SUL<String,String> (or SUL<S,T> in
        // general, with S and T the input and output types - but this class assumes strings)
        SUL<String,String> sul = new ExampleSUL();

        // the input alphabet
        Collection<String> inputAlphabet = ImmutableSet.of("a", "b", "c");

        try {
        	LearnerConfig config = new LearnerConfig();
            JCommander commander = new JCommander(config);
            commander.parse(args);
            if (config.isHelp()) {
            	commander.usage();
            } else {
            	MealyLearner<String, String> learner = new MealyLearner<String, String>(config, sul);
            	learner.runExperiment(inputAlphabet);
            }
        } finally {
            if (sul instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) sul).close();
                } catch (Exception exception) {
                    // should not happen
                }
            }
        }
    }
}
