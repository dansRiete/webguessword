import logic.Answer;
import logic.Phrase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


public class AnswerTest {

    @org.junit.Test
    public void testCorrectBehavior_WithEmptyAnswer(){
        assertThat(Answer.compose(0, "", "Привет", "Hello").isCorrect(), is(false));
        assertThat(Answer.compose(0, "", "Hello World", "Привет мир").isCorrect(), is(false));
        assertThat(Answer.compose(0, "", "Hello World/Hello everyone", "Привет мир/Привет всем").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingCorectnessSinglePhrase() {
        assertThat(Answer.compose(0, "Hello", "Hello", "Привет").isCorrect(),             is(true));
        assertThat(Answer.compose(0, "hello", "Hello", "Привет").isCorrect(),             is(true));
        assertThat(Answer.compose(0, "Hello", "hello", "Привет").isCorrect(),             is(true));
        assertThat(Answer.compose(0, "Hello world", "Hello world", "Привет").isCorrect(), is(true));
        assertThat(Answer.compose(0, "hello world", "Hello world", "Привет").isCorrect(), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException1() {
        Answer.compose(0, null, "Hello", "Привет");
    }
    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException2() {
        Answer.compose(0, "Hello", null, "Привет");
    }
    @Test(expected = RuntimeException.class)
    public void testNullArgumentThrowsException3() {
        Answer.compose(0, "Hello", "Привет", null);
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InSinglePhrase(){
        //In the only words which have more than six total letters spelling errors are ignored
        assertThat(Answer.compose(0, "Pretty", "Pretty", "Красивый").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Prety", "Pretty", "Красивый").isCorrect(), is(false));
        assertThat(Answer.compose(0, "Intelligent", "intelligent", "Умный").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Inteligent", "intelligent", "Умный").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Pretty intelligent", "Pretty intelligent", "Достаточно умный").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Pretty inteligent", "Pretty intelligent", "Достаточно умный").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Prety intelligent", "Pretty intelligent", "Достаточно умный").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InMultiplePhrases(){
        assertThat(Answer.compose(0, "all are pointless/all are intelligent", "all are pointless/all are intelligent", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "al are pointless/all are intelligent", "all are pointless/all are intelligent", "").isCorrect(), is(false));
        assertThat(Answer.compose(0, "all ar pointless/all are intelligent", "all are pointless/all are intelligent", "").isCorrect(), is(false));
        assertThat(Answer.compose(0, "all are pointless/al are intelligent", "all are pointless/all are intelligent", "").isCorrect(), is(false));
        assertThat(Answer.compose(0, "all are pointles/all are intelligent", "all are pointless/all are intelligent", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "all are pointles/all are inteligent", "all are pointless/all are intelligent", "").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testOrderInMultiplyPhrasesDoesNotMatter(){
        assertThat(Answer.compose(0, "Hi all/Hello World/Hello everyone", "Hello World/Hello everyone/Hi all", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Hello everyone/Hi all/Hello World/", "Hello World/Hello everyone/Hi all", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Hello everyone/Hi all/Helo World/", "Hello World/Hello everyone/Hi all", "").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testOrderOfWordsWithinPhraseMatters(){
        assertThat(Answer.compose(0, "Are you good", "You are good", "").isCorrect(), is(false));
        assertThat(Answer.compose(0, "We are good/Are you good", "You are good/We are good", "").isCorrect(), is(false));
    }

    @org.junit.Test
    public void testBothSlashesAreAcceptable(){
        assertThat(Answer.compose(0, "Hello World\\Hello everyone/Hi all", "Hello World/Hello everyone/Hi all", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Hello World\\Hello everyone\\Hi all", "Hello World/Hello everyone\\Hi all", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "Hello World\\Hello everyone/Hi all", "Hello World\\Hello everyone\\Hi all", "").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testSpacesDontMatter(){
        assertThat(Answer.compose(0, "You are good", "You  are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are good", "  You  are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are good", "  You  are good  ", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are good    ", "  You    are good  ", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You  are good", "You are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "  You  are good", "You are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "  You  are good   ", "You are good      ", "").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testPunctuationsMarksDontMatter(){
        assertThat(Answer.compose(0, "You are good", "You are good!", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are good", "You are, good!", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are $$# good", "You are, good!", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are - good", "You are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You are - good", "You@#$^)(*&@#% are good!@#$&*(%", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You! are - good", "You are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "You! are$^#& - good", "You are good", "").isCorrect(), is(true));
    }

    @org.junit.Test
    public void testDashAndSpaceAreTheSame(){
        assertThat(Answer.compose(0, "dining room", "dining-room", "").isCorrect(), is(true));

    }
    @org.junit.Test
    public void testLetterCaseDoesNotMatter(){
        assertThat(Answer.compose(0, "YOU ARE GOOD", "You are good", "").isCorrect(), is(true));
        assertThat(Answer.compose(0, "YoU ArE GOod", "YOU ARE GOOD", "").isCorrect(), is(true));
    }
}
