package se.uu.it.basiclearning.dfa;

import de.learnlib.oracle.SingleQueryOracle.SingleQueryOracleDFA;
import net.automatalib.word.Word;

public class ExampleDFAOracle implements SingleQueryOracleDFA<String> {
	
	private String regex;

	ExampleDFAOracle(String regex) {
		this.regex = regex;
	}
	
	@Override
	public Boolean answerQuery(Word<String> prefix, Word<String> suffix) {
		Word<String> word = prefix.concat(suffix);
		String string = word.stream().reduce("", (a,b) -> a + b);
		return string.matches(regex);
	}

}
