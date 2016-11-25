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
        List<String> givenPhraseWords = splitPhraseToWords(givenPhrase);
        List<String> referencePhraseWords = splitPhraseToWords(referencePhrase);

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

    private List<String> splitPhraseToWords(String givenPhrase){
        ArrayList<String> words = new ArrayList<>();
        for(String currentWord : givenPhrase.split(" ")){
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

    /*private boolean checkOnMatch(String givenWord, String referenceWord) {

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

    }*/
}
