package logic;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 11.11.2016.
 */
public class Answer {

    private final int phrasesId;
    private boolean answerIsCorrect;
    private final ZonedDateTime answersDate = ZonedDateTime.now(ZoneId.of("UTC"));
    private final String phrasesForeignLiteral;
    private final String phrasesNativeLiteral;
    private final String givenAnswerLiteral;

    private Answer(int phrasesId, String phrasesForeignLiteral, String phrasesNativeLiteral, String givenAnswerLiteral) {
        this.phrasesId = phrasesId;
        this.phrasesForeignLiteral = phrasesForeignLiteral;
        this.phrasesNativeLiteral = phrasesNativeLiteral;
        this.givenAnswerLiteral = givenAnswerLiteral;

    }

    public static Answer compose(int phrasesId, String givenAnswerLiteral, String phrasesForeignLiteral, String phrasesNativeLiteral){
        if(phrasesForeignLiteral == null || phrasesNativeLiteral == null|| givenAnswerLiteral == null){
            throw new IllegalArgumentException("Phrases foreign and native literals can not be null");
        }
        Answer composedAnswer = new Answer(phrasesId, phrasesForeignLiteral, phrasesNativeLiteral, givenAnswerLiteral);
        composedAnswer.checkTheAnswer();
        return composedAnswer;
    }

    public boolean isCorrect(){
        return answerIsCorrect;
    }

    private void checkTheAnswer(){
        if(givenAnswerLiteral.equals("")){
            answerIsCorrect = false;
        }else if(!givenAnswerLiteral.contains("\\") && !givenAnswerLiteral.contains("/")){
            answerIsCorrect = phrasesEquals(givenAnswerLiteral, phrasesForeignLiteral);
        }else {
            String [] givenLiteralPhrases = givenAnswerLiteral.split("[/\\\\]");
            String [] referenceLiteralPhrases = phrasesForeignLiteral.split("[/\\\\]");
            int matchesAmount = 0;
            for (String referenceLiteralPhrase : referenceLiteralPhrases) {
                for (String givenLiteralPhrase : givenLiteralPhrases) {
                    if (phrasesEquals(referenceLiteralPhrase, givenLiteralPhrase)) {
                        matchesAmount++;
                    }
                }
            }
            answerIsCorrect = matchesAmount == referenceLiteralPhrases.length;
        }
    }

    private boolean phrasesEquals(String givenPhrase, String referencePhrase){
        List<String> givenPhraseWords = splitToWords(givenPhrase);
        List<String> referencePhraseWords = splitToWords(referencePhrase);

        for (int i = 0; i < referencePhraseWords.size(); i++){
            if(!wordsEquals(referencePhraseWords.get(i), givenPhraseWords.get(i))){
                return false;
            }
        }
        return true;
    }

    private boolean wordsEquals(String givenWord, String referenceWord){
        if(givenWord.length() <= 6){
            return givenWord.replaceAll("\\W|_", "").equalsIgnoreCase(referenceWord.replaceAll("\\W|_", ""));
        }else {
            return removeDoubleLetters(givenWord.replaceAll("\\W|_", "")).equalsIgnoreCase(removeDoubleLetters(referenceWord.replaceAll("\\W|_", "")));
        }
    }

    private List<String> splitToWords(String givenPhrase){
        ArrayList<String> words = new ArrayList<>();
        for(String currentWord : givenPhrase.split("[ -]")){
            if(isWord(currentWord)){
                words.add(currentWord);
            }
        }
        return words;
    }

    private String removeDoubleLetters(String givenWord){
        StringBuilder shortAnswer = new StringBuilder().append(givenWord.charAt(0));
        for (int i = 1, y = 0; i < givenWord.length(); i++) {
            if (givenWord.toLowerCase().charAt(i) != shortAnswer.charAt(y)) {
                shortAnswer.append(givenWord.toLowerCase().charAt(i));
                y++;
            }
        }
        return shortAnswer.toString();
    }

    private boolean isWord(String givenWord){
        for(char currentChar : givenWord.toCharArray()){
            if(Character.isLetter(currentChar)){
                return true;
            }
        }
        return false;
    }

    public boolean isAnswerIsCorrect() {
        return answerIsCorrect;
    }

    public void setAnswerIsCorrect(boolean answerIsCorrect) {
        this.answerIsCorrect = answerIsCorrect;
    }
}
