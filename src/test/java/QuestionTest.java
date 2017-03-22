import logic.Question;
import datamodel.Phrase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


public class QuestionTest {

    @org.junit.Test
    public void testCorrectBehavior_WithEmptyAnswer(){
        assertThat(Question.compose(new Phrase().setForeignWord(""), "Hello").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord(""), "Hello World").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord(""), "Hello World/Hello everyone").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingCorectnessSinglePhrase() {
        assertThat(Question.compose(new Phrase().setForeignWord("Hello"), "Hello").isCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("hello"), "Hello").isCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello"), "hello").isCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello world"), "Hello world").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("hello world"), "Hello world").isCorrect(), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException1() {
        Question.compose(new Phrase().setForeignWord(null), "Hello");
    }
    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException2() {
        Question.compose(new Phrase().setForeignWord("Hello"), null);
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InSinglePhrase(){
        //In the only words which have more than six total letters spelling errors are ignored
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty"), "Pretty").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Prety"), "Pretty").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("Intelligent"), "intelligent").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Inteligent"), "intelligent").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty intelligent"), "Pretty intelligent").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty inteligent"), "Pretty intelligent").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Prety intelligent"), "Pretty intelligent").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InMultiplePhrases(){
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointless/all are intelligent"), "all are pointless/all are intelligent").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("al are pointless/all are intelligent"), "all are pointless/all are intelligent").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all ar pointless/all are intelligent"), "all are pointless/all are intelligent").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointless/al are intelligent"), "all are pointless/all are intelligent").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointles/all are intelligent"), "all are pointless/all are intelligent").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointles/all are inteligent"), "all are pointless/all are intelligent").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testOrderInMultiplyPhrasesDoesNotMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("Hi all/Hello World/Hello everyone"), "Hello World/Hello everyone/Hi all").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello everyone/Hi all/Hello World/"), "Hello World/Hello everyone/Hi all").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello everyone/Hi all/Helo World/"), "Hello World/Hello everyone/Hi all").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testOrderOfWordsWithinPhraseMatters(){
        assertThat(Question.compose(new Phrase().setForeignWord("Are you good"), "You are good").isCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("We are good/Are you good"), "You are good/We are good").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testBothSlashesAreAcceptable(){
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone/Hi all"), "Hello World/Hello everyone/Hi all").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone\\Hi all"), "Hello World/Hello everyone\\Hi all").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone/Hi all"), "Hello World\\Hello everyone\\Hi all").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testSpacesDontMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("You are good"), "You  are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good"), "  You  are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good"), "  You  are good  ").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good    "), "  You    are good  ").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You  are good"), "You are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("  You  are good"), "You are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("  You  are good   "), "You are good      ").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testPunctuationsMarksDontMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("You are good"), "You are good!").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good"), "You are, good!").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are $$# good"), "You are, good!").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are - good"), "You are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are - good"), "You@#$^)(*&@#% are good!@#$&*(%").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You! are - good"), "You are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You! are$^#& - good"), "You are good").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testDashAndSpaceAreTheSame(){
        assertThat(Question.compose(new Phrase().setForeignWord("dining room"), "dining-room").isCorrect(), is(true));

    }
    @org.junit.Test
    public void testLetterCaseDoesNotMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("YOU ARE GOOD"), "You are good").isCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("YoU ArE GOod"), "YOU ARE GOOD").isCorrect(), is(true));
    }
}
