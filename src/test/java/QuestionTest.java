import datamodel.Phrase;
import datamodel.Question;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class QuestionTest {

    @org.junit.Test
    public void testCorrectBehavior_WithEmptyAnswer(){
        assertThat(Question.compose(new Phrase().setForeignWord("")).answerTheQuestion("Hello").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("")).answerTheQuestion("Hello World").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("")).answerTheQuestion("Hello World/Hello everyone").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingCorectnessSinglePhrase() {
        assertThat(Question.compose(new Phrase().setForeignWord("Hello")).answerTheQuestion("Hello").answerIsCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("hello")).answerTheQuestion("Hello").answerIsCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello")).answerTheQuestion("hello").answerIsCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello world")).answerTheQuestion("Hello world").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("hello world")).answerTheQuestion("Hello world").answerIsCorrect(), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException1() {
        Question.compose(new Phrase().setForeignWord(null)).answerTheQuestion("Hello");
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException2() {
        Question.compose(new Phrase().setForeignWord("Hello")).answerTheQuestion(null);
    }

    @org.junit.Test
    public void unansweredPhraseIsNotCorrectAnswered(){
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void isQuestionAnsweredMethodCheck(){
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answered(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answerTheQuestion("").answered(), is(true));
    }


    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InSinglePhrase(){
        //In the only words which have more than six total letters spelling errors are ignored
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answerTheQuestion("Pretty").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Prety")).answerTheQuestion("Pretty").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("Intelligent")).answerTheQuestion("intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Inteligent")).answerTheQuestion("intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty intelligent")).answerTheQuestion("Pretty intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty inteligent")).answerTheQuestion("Pretty intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Prety intelligent")).answerTheQuestion("Pretty intelligent").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InMultiplePhrases(){
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointless/all are intelligent")).answerTheQuestion("all are pointless/all are intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("al are pointless/all are intelligent")).answerTheQuestion("all are pointless/all are intelligent").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all ar pointless/all are intelligent")).answerTheQuestion("all are pointless/all are intelligent").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointless/al are intelligent")).answerTheQuestion("all are pointless/all are intelligent").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointles/all are intelligent")).answerTheQuestion("all are pointless/all are intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointles/all are inteligent")).answerTheQuestion("all are pointless/all are intelligent").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testOrderInMultiplyPhrasesDoesNotMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("Hi all/Hello World/Hello everyone")).answerTheQuestion("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello everyone/Hi all/Hello World/")).answerTheQuestion("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello everyone/Hi all/Helo World/")).answerTheQuestion("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testOrderOfWordsWithinPhraseMatters(){
        assertThat(Question.compose(new Phrase().setForeignWord("Are you good")).answerTheQuestion("You are good").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("We are good/Are you good")).answerTheQuestion("You are good/We are good").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testBothSlashesAreAcceptable(){
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone/Hi all")).answerTheQuestion("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone\\Hi all")).answerTheQuestion("Hello World/Hello everyone\\Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone/Hi all")).answerTheQuestion("Hello World\\Hello everyone\\Hi all").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testSpacesDontMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerTheQuestion("You  are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerTheQuestion("  You  are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerTheQuestion("  You  are good  ").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good    ")).answerTheQuestion("  You    are good  ").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You  are good")).answerTheQuestion("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("  You  are good")).answerTheQuestion("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("  You  are good   ")).answerTheQuestion("You are good      ").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testPunctuationsMarksDontMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerTheQuestion("You are good!").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerTheQuestion("You are, good!").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are $$# good")).answerTheQuestion("You are, good!").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are - good")).answerTheQuestion("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are - good")).answerTheQuestion("You@#$^)(*&@#% are good!@#$&*(%").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You! are - good")).answerTheQuestion("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You! are$^#& - good")).answerTheQuestion("You are good").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testDashAndSpaceAreTheSame(){
        assertThat(Question.compose(new Phrase().setForeignWord("dining room")).answerTheQuestion("dining-room").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testLetterCaseDoesNotMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("YOU ARE GOOD")).answerTheQuestion("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("YoU ArE GOod")).answerTheQuestion("YOU ARE GOOD").answerIsCorrect(), is(true));
    }
}
