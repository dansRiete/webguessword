import org.junit.Test;
import aleks.kuzko.utils.AnswerChecker;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@SuppressWarnings({"Duplicates", "SpellCheckingInspection"})
public class AnswerCheckerTests {

    @org.junit.Test
    public void testCorrectBehavior_WithEmptyAnswer(){
        assertThat(AnswerChecker.checkLiterals("","Hello"), is(false));
        assertThat(AnswerChecker.checkLiterals("","Hello World"), is(false));
        assertThat(AnswerChecker.checkLiterals("","Hello World/Hello everyone"), is(false));
    }

    @org.junit.Test
    public void testRecognizingCorrectnessSinglePhrase() {
        assertThat(AnswerChecker.checkLiterals("Hello","Hello"),             is(true));
        assertThat(AnswerChecker.checkLiterals("hello","Hello"),             is(true));
        assertThat(AnswerChecker.checkLiterals("Hello","hello"),             is(true));
        assertThat(AnswerChecker.checkLiterals("Hello world","Hello world"), is(true));
        assertThat(AnswerChecker.checkLiterals("hello world","Hello world"), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException1() {
        AnswerChecker.checkLiterals(null,"Hello");
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException2() {
        AnswerChecker.checkLiterals("Hello",null);
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InSinglePhrase(){
        //In the only words which have more than six total letters spelling errors are ignored
        assertThat(AnswerChecker.checkLiterals("Pretty","Pretty"), is(true));
        assertThat(AnswerChecker.checkLiterals("Prety","Pretty"), is(false));
        assertThat(AnswerChecker.checkLiterals("Intelligent","intelligent"), is(true));
        assertThat(AnswerChecker.checkLiterals("Inteligent","intelligent"), is(true));
        assertThat(AnswerChecker.checkLiterals("Pretty intelligent","Pretty intelligent"), is(true));
        assertThat(AnswerChecker.checkLiterals("Pretty inteligent","Pretty intelligent"), is(true));
        assertThat(AnswerChecker.checkLiterals("Prety intelligent","Pretty intelligent"), is(false));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InMultiplePhrases(){
        assertThat(AnswerChecker.checkLiterals("all are pointless/all are intelligent","all are pointless/all are intelligent"), is(true));
        assertThat(AnswerChecker.checkLiterals("al are pointless/all are intelligent","all are pointless/all are intelligent"), is(false));
        assertThat(AnswerChecker.checkLiterals("all ar pointless/all are intelligent","all are pointless/all are intelligent"), is(false));
        assertThat(AnswerChecker.checkLiterals("all are pointless/al are intelligent","all are pointless/all are intelligent"), is(false));
        assertThat(AnswerChecker.checkLiterals("all are pointles/all are intelligent","all are pointless/all are intelligent"), is(true));
        assertThat(AnswerChecker.checkLiterals("all are pointles/all are inteligent","all are pointless/all are intelligent"), is(true));
    }

    @org.junit.Test
    public void testOrderInMultiplyPhrasesDoesNotMatter(){
        assertThat(AnswerChecker.checkLiterals("Hi all/Hello World/Hello everyone","Hello World/Hello everyone/Hi all"), is(true));
        assertThat(AnswerChecker.checkLiterals("Hello everyone/Hi all/Hello World/","Hello World/Hello everyone/Hi all"), is(true));
        assertThat(AnswerChecker.checkLiterals("Hello everyone/Hi all/Helo World/","Hello World/Hello everyone/Hi all"), is(false));
    }

    @org.junit.Test
    public void testOrderOfWordsWithinPhraseMatters(){
        assertThat(AnswerChecker.checkLiterals("Are you good","You are good"), is(false));
        assertThat(AnswerChecker.checkLiterals("We are good/Are you good","You are good/We are good"), is(false));
    }

    @org.junit.Test
    public void testBothSlashesAreAcceptable(){
        assertThat(AnswerChecker.checkLiterals("Hello World\\Hello everyone/Hi all","Hello World/Hello everyone/Hi all"), is(true));
        assertThat(AnswerChecker.checkLiterals("Hello World\\Hello everyone\\Hi all","Hello World/Hello everyone\\Hi all"), is(true));
        assertThat(AnswerChecker.checkLiterals("Hello World\\Hello everyone/Hi all","Hello World\\Hello everyone\\Hi all"), is(true));
    }

    @org.junit.Test
    public void testSpacesDontMatter(){
        assertThat(AnswerChecker.checkLiterals("You are good","You  are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("You are good","  You  are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("You are good","  You  are good  "), is(true));
        assertThat(AnswerChecker.checkLiterals("You are good    ","  You    are good  "), is(true));
        assertThat(AnswerChecker.checkLiterals("You  are good","You are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("  You  are good","You are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("  You  are good   ","You are good      "), is(true));
    }

    @org.junit.Test
    public void testPunctuationsMarksDontMatter(){
        assertThat(AnswerChecker.checkLiterals("You are good","You are good!"), is(true));
        assertThat(AnswerChecker.checkLiterals("You are good","You are, good!"), is(true));
        assertThat(AnswerChecker.checkLiterals("You are $$# good","You are, good!"), is(true));
        assertThat(AnswerChecker.checkLiterals("You are - good","You are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("You are - good","You@#$^)(*&@#% are good!@#$&*(%"), is(true));
        assertThat(AnswerChecker.checkLiterals("You! are - good","You are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("You! are$^#& - good","You are good"), is(true));
    }

    @org.junit.Test
    public void testDashAndSpaceAreTheSame(){
        assertThat(AnswerChecker.checkLiterals("dining room","dining-room"), is(true));
    }

    @org.junit.Test
    public void testLetterCaseDoesNotMatter(){
        assertThat(AnswerChecker.checkLiterals("YOU ARE GOOD","You are good"), is(true));
        assertThat(AnswerChecker.checkLiterals("YoU ArE GOod", "YOU ARE GOOD"), is(true));
    }
}
