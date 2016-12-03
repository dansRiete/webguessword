import logic.Answer;
import logic.Phrase;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


public class AnswerTest {

    @org.junit.Test
    public void testCorrectBehavior_WithNullAndEmptyAnswer(){
        assertThat(Answer.compose(0, "Hello", "Привет", "Hello").isCorrect(), is(false));
        assertThat(new Answer("", new Phrase("Hello World", "Привет мир")).isCorrect(), is(false));
        assertThat(new Answer("", new Phrase("Hello World/Hello everyone", "Привет мир/Привет всем")).isCorrect(), is(false));
        assertThat(new Answer(null, new Phrase("Hello", "Привет")).isCorrect(), is(false));
        assertThat(new Answer(null, new Phrase("Hello World", "Привет мир")).isCorrect(), is(false));
        assertThat(new Answer(null, new Phrase("Hello World/Hello everyone", "Привет мир/Привет всем")).isCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingCorectnessSinglePhrase() {
        assertThat(new Answer("Hello", new Phrase("Hello", "Привет")).isCorrect(),             is(true));
        assertThat(new Answer("hello", new Phrase("Hello", "Привет")).isCorrect(),             is(true));
        assertThat(new Answer("Hello", new Phrase("hello", "Привет")).isCorrect(),             is(true));
        assertThat(new Answer("Hello world", new Phrase("Hello world", "Привет")).isCorrect(), is(true));
        assertThat(new Answer("hello world", new Phrase("Hello world", "Привет")).isCorrect(), is(true));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InSinglePhrase(){
        //In the only words which have more than six total letters spelling errors are ignored
        assertThat(new Answer("Pretty", new Phrase("Pretty", "Красивый")).isCorrect(), is(true));
        assertThat(new Answer("Prety", new Phrase("Pretty", "Красивый")).isCorrect(), is(false));
        assertThat(new Answer("Intelligent", new Phrase("intelligent", "Умный")).isCorrect(), is(true));
        assertThat(new Answer("Inteligent", new Phrase("intelligent", "Умный")).isCorrect(), is(true));
        assertThat(new Answer("Pretty intelligent", new Phrase("Pretty intelligent", "Достаточно умный")).isCorrect(), is(true));
        assertThat(new Answer("Pretty inteligent", new Phrase("Pretty intelligent", "Достаточно умный")).isCorrect(), is(true));
        assertThat(new Answer("Prety intelligent", new Phrase("Pretty intelligent", "Достаточно умный")).isCorrect(), is(false));
    }

    @org.junit.Test
    public void testRecognizingSpellingErrorsWithDoubleLetters_InMultiplePhrase(){
        assertThat(new Answer("all are pointless/all are intelligent", new Phrase("all are pointless/all are intelligent", "")).isCorrect(), is(true));
        assertThat(new Answer("al are pointless/all are intelligent", new Phrase("all are pointless/all are intelligent", "")).isCorrect(), is(false));
        assertThat(new Answer("all ar pointless/all are intelligent", new Phrase("all are pointless/all are intelligent", "")).isCorrect(), is(false));
        assertThat(new Answer("all are pointless/al are intelligent", new Phrase("all are pointless/all are intelligent", "")).isCorrect(), is(false));
        assertThat(new Answer("all are pointles/all are intelligent", new Phrase("all are pointless/all are intelligent", "")).isCorrect(), is(true));
        assertThat(new Answer("all are pointles/all are inteligent", new Phrase("all are pointless/all are intelligent", "")).isCorrect(), is(true));
    }

    @org.junit.Test
    public void testOrderInMultiplyPhrasesDoesNotMatter(){
        assertThat(new Answer("Hi all/Hello World/Hello everyone", new Phrase("Hello World/Hello everyone/Hi all", "")).isCorrect(), is(true));
        assertThat(new Answer("Hello everyone/Hi all/Hello World/", new Phrase("Hello World/Hello everyone/Hi all", "")).isCorrect(), is(true));
        assertThat(new Answer("Hello everyone/Hi all/Helo World/", new Phrase("Hello World/Hello everyone/Hi all", "")).isCorrect(), is(false));
    }

    @org.junit.Test
    public void testOrderOfWordsWithinPhraseMatters(){
        assertThat(new Answer("Are you good", new Phrase("You are good", "")).isCorrect(), is(false));
        assertThat(new Answer("We are good/Are you good", new Phrase("You are good/We are good", "")).isCorrect(), is(false));
    }

    @org.junit.Test
    public void testBothSlashesAreAcceptable(){
        assertThat(new Answer("Hello World\\Hello everyone/Hi all", new Phrase("Hello World/Hello everyone/Hi all", "")).isCorrect(), is(true));
        assertThat(new Answer("Hello World\\Hello everyone\\Hi all", new Phrase("Hello World/Hello everyone\\Hi all", "")).isCorrect(), is(true));
        assertThat(new Answer("Hello World\\Hello everyone/Hi all", new Phrase("Hello World\\Hello everyone\\Hi all", "")).isCorrect(), is(true));
    }

    @org.junit.Test
    public void testSpacesDontMatter(){
        assertThat(new Answer("You are good", new Phrase("You  are good", "")).isCorrect(), is(true));
        assertThat(new Answer("You are good", new Phrase("  You  are good", "")).isCorrect(), is(true));
        assertThat(new Answer("You are good", new Phrase("  You  are good  ", "")).isCorrect(), is(true));
        assertThat(new Answer("You are good    ", new Phrase("  You    are good  ", "")).isCorrect(), is(true));
        assertThat(new Answer("You  are good", new Phrase("You are good", "")).isCorrect(), is(true));
        assertThat(new Answer("  You  are good", new Phrase("You are good", "")).isCorrect(), is(true));
        assertThat(new Answer("  You  are good   ", new Phrase("You are good      ", "")).isCorrect(), is(true));
    }

    @org.junit.Test
    public void testPunctuationsMarksDontMatter(){
        assertThat(new Answer("You are good", new Phrase("You are good!", "")).isCorrect(), is(true));
        assertThat(new Answer("You are good", new Phrase("You are, good!", "")).isCorrect(), is(true));
        assertThat(new Answer("You are $$# good", new Phrase("You are, good!", "")).isCorrect(), is(true));
        assertThat(new Answer("You are - good", new Phrase("You are good", "")).isCorrect(), is(true));
        assertThat(new Answer("You are - good", new Phrase("You@#$^)(*&@#% are good!@#$&*(%", "")).isCorrect(), is(true));
        assertThat(new Answer("You! are - good", new Phrase("You are good", "")).isCorrect(), is(true));
        assertThat(new Answer("You! are$^#& - good", new Phrase("You are good", "")).isCorrect(), is(true));
    }

    @org.junit.Test
    public void testDashAndSpaceAreTheSame(){
        assertThat(new Answer("dining room", new Phrase("dining-room", "")).isCorrect(), is(true));

    }
    @org.junit.Test
    public void testLetterCaseDoesNotMatter(){
        assertThat(new Answer("YOU ARE GOOD", new Phrase("You are good", "")).isCorrect(), is(true));
        assertThat(new Answer("YoU ArE GOod", new Phrase("YOU ARE GOOD", "")).isCorrect(), is(true));
    }
}
