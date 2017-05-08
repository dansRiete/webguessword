package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 06.05.2017.
 */
public class AnswerChecker {

    public static boolean checkLiterals(String answer, String referenceLiteral){
        boolean correct;
        if (answer.equals("") || referenceLiteral.equals("")) {
            correct = false;
        } else if (!answer.contains("\\") && !answer.contains("/")) {
            correct = phrasesEquals(answer, referenceLiteral);
        } else {
            String[] givenLiteralPhrases = answer.split("[/\\\\]");
            String[] referenceLiteralPhrases = referenceLiteral.split("[/\\\\]");
            int matchesAmount = 0;
            for (String referenceLiteralPhrase : referenceLiteralPhrases) {
                for (String givenLiteralPhrase : givenLiteralPhrases) {
                    if (phrasesEquals(referenceLiteralPhrase, givenLiteralPhrase)) {
                        matchesAmount++;
                    }
                }
            }
            correct = matchesAmount == referenceLiteralPhrases.length;
        }
        return correct;
    }

    public static boolean phrasesEquals(String givenPhrase, String referencePhrase) {
        List<String> givenPhraseWords = splitToWords(givenPhrase);
        List<String> referencePhraseWords = splitToWords(referencePhrase);

        for (int i = 0; i < referencePhraseWords.size(); i++) {
            if (!wordsEquals(referencePhraseWords.get(i), givenPhraseWords.get(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean wordsEquals(String givenWord, String referenceWord) {
        if (givenWord.length() <= 6) {
            return givenWord.replaceAll("\\W|_", "").equalsIgnoreCase(referenceWord.replaceAll("\\W|_", ""));
        } else {
            return removeDoubleLetters(givenWord.replaceAll("\\W|_", "")).
                    equalsIgnoreCase(removeDoubleLetters(referenceWord.replaceAll("\\W|_", "")));
        }
    }

    public static List<String> splitToWords(String givenPhrase) {
        ArrayList<String> words = new ArrayList<>();
        for (String currentWord : givenPhrase.split("[ -]")) {
            if (isWord(currentWord)) {
                words.add(currentWord);
            }
        }
        return words;
    }

    public static String removeDoubleLetters(String givenWord) {
        StringBuilder shortAnswer = new StringBuilder().append(givenWord.charAt(0));
        for (int i1 = 1, i2 = 0; i1 < givenWord.length(); i1++) {
            if (givenWord.toLowerCase().charAt(i1) != shortAnswer.charAt(i2)) {
                shortAnswer.append(givenWord.toLowerCase().charAt(i1));
                i2++;
            }
        }
        return shortAnswer.toString();
    }

    public static boolean isWord(String givenWord) {
        for (char currentChar : givenWord.toCharArray()) {
            if (Character.isLetter(currentChar)) {
                return true;
            }
        }
        return false;
    }
}
