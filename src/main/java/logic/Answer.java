package logic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Aleks on 11.11.2016.
 */
public class Answer {
    private boolean answerIsCorrect;
    private final String[] givenPhrases;
    private final String[] referencePhrases;
    private final static int SYLLABLE_SIZE = 2;

    public Answer(String answer, Phrase question) {
        this.givenPhrases = answer.split("[/\\\\]");
        this.referencePhrases = question.getForeignWord().split("[/\\\\]");
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

    private String[] divideOntoSyllables(String givenPhrase){
        int syllabelsSize = 2;
        int positionInWord = 0;
        int wordsPosition = 0;
        String[] words = givenPhrase.split(" ");
        String[] syllables = new String[givenPhrase.length() / syllabelsSize];
        for(String currentWord : words){
            if(currentWord.length() <= 3){
                words[wordsPosition] = currentWord;
                wordsPosition++;
            }else {

            }
        }


    }

    private boolean checkPhrase(String givenPhrase, String referencePhrase){
        String shortedGivenPhrase = removeDoubleLetters(givenPhrase);
        String shortedReferencePhrase = removeDoubleLetters(referencePhrase);
        String[] referenceWords = shortedReferencePhrase.split(" ");
        String[] givenWords = shortedGivenPhrase.split(" ");
        if(referenceWords.length != givenWords.length){
            return false;
        }else {
            for (int i = 0; i < givenWords.length; i++){
                if(!givenWords[i].equalsIgnoreCase(referenceWords[i])){
                    return false;
                }
            }
        }
    }

    private boolean equalsBySyllables(String givenWord, String referenceWord){
        if(referenceWord.length() <= 5 && !givenWord.equalsIgnoreCase(referenceWord)){
            return false;
        }else {

        }
    }

    private String[] divideOntoEvenSyllables(String givenWord){

    }

    private boolean checkOnMatch(String givenWord, String referenceWord) {

        if(givenWord.equalsIgnoreCase(referenceWord)){
            return true;
        } else {
            int syllablesAmount;
            String shortedTestableWord = removeDoubleLetters(givenWord);
            if (shortedTestableWord.equalsIgnoreCase(referenceWord)){
                answerIsCorrect = true;
            }
            else if (shortedTestableWord.length() < 5)
                answerIsCorrect = false;
            else {
                //разбиваем shortWord на слоги
                syllablesAmount = shortedTestableWord.length() / 2; //вычисляем количество слогов по3
                if (shortedTestableWord.length() % 2 > 0)
                    syllablesAmount = syllablesAmount + 1;
                //System.out.println("Количество слогов в shortWord=" + b);

                String arWord[] = new String[syllablesAmount];
                for (int i = 0; i < syllablesAmount; i++) {
                    //Заполняем массив arWord[] слогами
                    arWord[i] = shortedTestableWord.substring(i*2, (i*2+2 > shortedTestableWord.length()) ? shortedTestableWord.length() : i*2+2);
                }
                float matchedSyllables = 0;
                for (String anArWord : arWord) {
                    try {
                        Matcher match = Pattern.compile(anArWord).matcher(shortedTestableWord);
                        if (match.find()) {
                            matchedSyllables++;
                        }
                    } catch (PatternSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                float f=matchedSyllables/(float)syllablesAmount;
                answerIsCorrect = f>0.7;
            }
        }
        return false;

    }



}
