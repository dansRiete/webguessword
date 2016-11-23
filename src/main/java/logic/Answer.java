package logic;

import java.util.regex.*;

/**
 * Created by Aleks on 11.11.2016.
 */
public class Answer {
    private boolean answerIsCorrect;
//    private final String givenAnswerLiteral;
//    private final String referenceAnswerLiteral;
    private final static int SYLLABLE_SIZE = 2;
    private final static int EXACT_MATCH_WORD_SIZE = 6;

    public Answer(String answerLiteral, Phrase referencePhrase) {
//        this.givenAnswerLiteral = answerLiteral == null ? "" : answerLiteral.replaceAll("\\W|_", "");
//        this.referenceAnswerLiteral = referencePhrase.getForeignWord().replaceAll("\\W|_", "");
        this.answerIsCorrect = literalEquals(answerLiteral, referencePhrase.getForeignWord());
    }

    private boolean literalEquals(String givenLiteral, String referenceLiteral){
        if(givenLiteral == null){
            return false;
        }else if(!givenLiteral.contains("\\") && !givenLiteral.contains("/")){
            return phraseEquals(givenLiteral, referenceLiteral);
        }else {
            String [] givenLiteralPhrases = givenLiteral.split("[/\\\\]");
            String [] referenceLiteralPhrases = referenceLiteral.split("[/\\\\]");
            int matchesAmount = 0;
            for (int i1 = 0; i1 < referenceLiteralPhrases.length; i1++){
                for (int i2 = 0; i2 < givenLiteralPhrases.length; i2++){
                    if(phraseEquals(referenceLiteralPhrases[i1], givenLiteralPhrases[i2])){
                        matchesAmount++;
                    }
                }
                /*if(!phraseEquals(givenLiteralPhrases[i1], referenceLiteralPhrases[i1])){
                    return false;
                }*/
            }
            return matchesAmount == referenceLiteralPhrases.length;
        }
    }

    private boolean phraseEquals(String givenPhrase, String referencePhrase){
        String [] givenPhraseWords = givenPhrase.split(" ");
        String [] referencePhraseWords = referencePhrase.split(" ");
        if(givenPhraseWords.length != referencePhraseWords.length){
            return false;
        }else {
            for (int i = 0; i < referencePhraseWords.length; i++){
                if(!wordEquals(givenPhraseWords[i], referencePhraseWords[i])){
                    return false;
                }
            }
            return true;
        }
    }

    private boolean wordEquals(String givenWord, String referenceWord){
        if(givenWord.length() <= 6){
            return givenWord.replaceAll("\\W|_", "").equalsIgnoreCase(referenceWord.replaceAll("\\W|_", ""));
        }else {
            return removeDoubleLetters(givenWord.replaceAll("\\W|_", "")).equalsIgnoreCase(removeDoubleLetters(referenceWord.replaceAll("\\W|_", "")));
        }
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

    /*private boolean equalsIgnoreDoubleLetters(String givenSentence, String referenceSentence){
        return removeDoubleLetters(givenSentence).equalsIgnoreCase(removeDoubleLetters(referenceSentence));
    }

    private boolean checkTheAnswerOnCorrectness(){
        if(!referenceAnswerLiteral.contains("\\") && !referenceAnswerLiteral.contains("/")){
            if(referenceAnswerLiteral.length() > 6){
                return equalsIgnoreDoubleLetters(givenAnswerLiteral, referenceAnswerLiteral);
            }else {
                return givenAnswerLiteral.equalsIgnoreCase(referenceAnswerLiteral);
            }

        }else {
            String [] givenAnswers = givenAnswerLiteral.split("[/\\\\]");
            String [] referenceAnswers = referenceAnswerLiteral.split("[/\\\\]");
        }
    }

    private boolean equalsBySyllables(String givenWord, String referenceWord){
        if(referenceWord.length() <= 5 && !givenWord.equalsIgnoreCase(referenceWord)){
            return false;
        }else {

        }
    }

    *//*private String[] divideOntoEvenSyllables(String givenAnswerLiteral){
        String[] splitedWords = givenAnswerLiteral.split(" ");
        ArrayList<String> syllables = new ArrayList<>();
        for(String currentSplitedWord : splitedWords){
            if(currentSplitedWord.length() <= EXACT_MATCH_WORD_SIZE){
                syllables.add(currentSplitedWord);
            }else {
                int positionInWord = 0;
                while (true){
                    syllables.add(currentSplitedWord.substring(positionInWord, positionInWord += SYLLABLE_SIZE));
                }
            }
        }
    }*//*

    private String[] divideOntoEvenSyllables(String givenWord, int startDividingPosition){

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

    }*/

    public boolean isTheAnswerCorrect(){
        return answerIsCorrect;
    }



}
