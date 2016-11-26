package logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 11.11.2016.
 */
public class Answer {

    private boolean answerIsCorrect;
    public Answer(String answerLiteral, Phrase referencePhrase) {
        this.answerIsCorrect = literalsEquals(answerLiteral, referencePhrase.getForeignWord());
    }

    public boolean isCorrect(){
        return answerIsCorrect;
    }

    private boolean literalsEquals(String givenLiteral, String referenceLiteral){
        if(givenLiteral == null || givenLiteral.equals("")){
            return false;
        }else if(!givenLiteral.contains("\\") && !givenLiteral.contains("/")){
            return phrasesEquals(givenLiteral, referenceLiteral);
        }else {
            String [] givenLiteralPhrases = givenLiteral.split("[/\\\\]");
            String [] referenceLiteralPhrases = referenceLiteral.split("[/\\\\]");
            int matchesAmount = 0;
            for (String referenceLiteralPhrase : referenceLiteralPhrases) {
                for (String givenLiteralPhrase : givenLiteralPhrases) {
                    if (phrasesEquals(referenceLiteralPhrase, givenLiteralPhrase)) {
                        matchesAmount++;
                    }
                }
            }
            return matchesAmount == referenceLiteralPhrases.length;
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
}
