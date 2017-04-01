import datamodel.Phrase;
import datamodel.Question;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class QuestionTest {

    /*@org.junit.Test
    public void testCorrectBehavior_WithEmptyAnswer(){
        assertThat(Question.compose(new Phrase().setForeignWord("")).answerButtonAction("Hello").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("")).answerButtonAction("Hello World").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("")).answerButtonAction("Hello World/Hello everyone").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingCorectnessSinglePhrase() {
        assertThat(Question.compose(new Phrase().setForeignWord("Hello")).answerButtonAction("Hello").answerIsCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("hello")).answerButtonAction("Hello").answerIsCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello")).answerButtonAction("hello").answerIsCorrect(),             is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello world")).answerButtonAction("Hello world").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("hello world")).answerButtonAction("Hello world").answerIsCorrect(), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException1() {
        Question.compose(new Phrase().setForeignWord(null)).answerButtonAction("Hello");
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException2() {
        Question.compose(new Phrase().setForeignWord("Hello")).answerButtonAction(null);
    }

    @org.junit.Test
    public void unansweredPhraseIsNotCorrectAnswered(){
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void isQuestionAnsweredMethodCheck(){
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answered(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answerButtonAction("").answered(), is(true));
    }


    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InSinglePhrase(){
        //In the only words which have more than six total letters spelling errors are ignored
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty")).answerButtonAction("Pretty").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Prety")).answerButtonAction("Pretty").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("Intelligent")).answerButtonAction("intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Inteligent")).answerButtonAction("intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty intelligent")).answerButtonAction("Pretty intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Pretty inteligent")).answerButtonAction("Pretty intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Prety intelligent")).answerButtonAction("Pretty intelligent").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InMultiplePhrases(){
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointless/all are intelligent")).answerButtonAction("all are pointless/all are intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("al are pointless/all are intelligent")).answerButtonAction("all are pointless/all are intelligent").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all ar pointless/all are intelligent")).answerButtonAction("all are pointless/all are intelligent").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointless/al are intelligent")).answerButtonAction("all are pointless/all are intelligent").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointles/all are intelligent")).answerButtonAction("all are pointless/all are intelligent").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("all are pointles/all are inteligent")).answerButtonAction("all are pointless/all are intelligent").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testOrderInMultiplyPhrasesDoesNotMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("Hi all/Hello World/Hello everyone")).answerButtonAction("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello everyone/Hi all/Hello World/")).answerButtonAction("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello everyone/Hi all/Helo World/")).answerButtonAction("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testOrderOfWordsWithinPhraseMatters(){
        assertThat(Question.compose(new Phrase().setForeignWord("Are you good")).answerButtonAction("You are good").answerIsCorrect(), is(false));
        assertThat(Question.compose(new Phrase().setForeignWord("We are good/Are you good")).answerButtonAction("You are good/We are good").answerIsCorrect(), is(false));
    }

    @org.junit.Test
    public void testBothSlashesAreAcceptable(){
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone/Hi all")).answerButtonAction("Hello World/Hello everyone/Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone\\Hi all")).answerButtonAction("Hello World/Hello everyone\\Hi all").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("Hello World\\Hello everyone/Hi all")).answerButtonAction("Hello World\\Hello everyone\\Hi all").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testSpacesDontMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerButtonAction("You  are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerButtonAction("  You  are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerButtonAction("  You  are good  ").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good    ")).answerButtonAction("  You    are good  ").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You  are good")).answerButtonAction("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("  You  are good")).answerButtonAction("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("  You  are good   ")).answerButtonAction("You are good      ").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testPunctuationsMarksDontMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerButtonAction("You are good!").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are good")).answerButtonAction("You are, good!").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are $$# good")).answerButtonAction("You are, good!").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are - good")).answerButtonAction("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You are - good")).answerButtonAction("You@#$^)(*&@#% are good!@#$&*(%").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You! are - good")).answerButtonAction("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("You! are$^#& - good")).answerButtonAction("You are good").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testDashAndSpaceAreTheSame(){
        assertThat(Question.compose(new Phrase().setForeignWord("dining room")).answerButtonAction("dining-room").answerIsCorrect(), is(true));
    }

    @org.junit.Test
    public void testLetterCaseDoesNotMatter(){
        assertThat(Question.compose(new Phrase().setForeignWord("YOU ARE GOOD")).answerButtonAction("You are good").answerIsCorrect(), is(true));
        assertThat(Question.compose(new Phrase().setForeignWord("YoU ArE GOod")).answerButtonAction("YOU ARE GOOD").answerIsCorrect(), is(true));
    }*/
}
