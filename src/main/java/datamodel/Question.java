package datamodel;

import logic.DatabaseHelper;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 11.11.2016.
 */

@Entity
@Table(name = "questions")
public class Question {

    @Transient
    private static final double RIGHT_ANSWER_MULTIPLIER = 1.44;

    @Transient
    private static final double RIGHT_ANSWER_SUBTRAHEND = 3;

    @Transient
    private static final double WRONG_ANSWER_ADDEND = 6;

    @Transient
    private static final double TRAINED_PROBABILITY_FACTOR = 3;

    @javax.persistence.Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Column(name = "answer")
    private String answer;

    @Column(name = "date")
    private final ZonedDateTime askDate = ZonedDateTime.now();

    @Column(name = "phrase_key")
    private final Phrase askedPhrase;

    @Column(name = "answered_correctly")
    private boolean answerCorrect;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    private double initialProbabilityFactor;

    @Transient
    private double initialProbabilityMultiplier;

    @Transient
    private double afterAnswerProbabilityFactor;

    @Transient
    private double afterAnswerProbabilityMultiplier;

    @Transient
    private DatabaseHelper databaseHelper;

    @Transient
    private String questionRepresentation;

    private Question(Phrase askedPhrase, DatabaseHelper databaseHelper) {
        this.askedPhrase = askedPhrase;
        this.databaseHelper = databaseHelper;
        initialProbabilityFactor = askedPhrase.getProbabilityFactor();
        initialProbabilityMultiplier = askedPhrase.getMultiplier();
        questionRepresentation = askedPhrase.foreignWord + " " + shortHint();
    }

    public static Question compose(Phrase askedPhrase){
        if(askedPhrase == null){
            throw new IllegalArgumentException("Phrases foreign and native literals can not be null");
        }
        return new Question(askedPhrase, null);
    }

    public static Question compose(Phrase askedPhrase, DatabaseHelper dbHelper){
        if(askedPhrase == null){
            throw new IllegalArgumentException("Phrases foreign and native literals can not be null");
        }
        return new Question(askedPhrase, dbHelper);
    }

    public void deletePhrase(){

    }

    public Question answerTheQuestion(String answer){
        this.answer = answer;
        if(this.answer.equals("") || askedPhrase.getForeignWord().equals("")){
            answerCorrect = false;
        }else if(!this.answer.contains("\\") && !this.answer.contains("/")){
            answerCorrect = phrasesEquals(this.answer, askedPhrase.getForeignWord());
        }else {
            String [] givenLiteralPhrases = this.answer.split("[/\\\\]");
            String [] referenceLiteralPhrases = askedPhrase.getForeignWord().split("[/\\\\]");
            int matchesAmount = 0;
            for (String referenceLiteralPhrase : referenceLiteralPhrases) {
                for (String givenLiteralPhrase : givenLiteralPhrases) {
                    if (phrasesEquals(referenceLiteralPhrase, givenLiteralPhrase)) {
                        matchesAmount++;
                    }
                }
            }
            answerCorrect = matchesAmount == referenceLiteralPhrases.length;
        }
        return this;
    }

    public Question rightAnswer(){
        this.answer = askedPhrase.getForeignWord();
        this.answerCorrect = true;
        Phrase phraseInDb = databaseHelper.getPhrase(askedPhrase);

        if(!phraseIsAlreadyTrained()){

            afterAnswerProbabilityFactor = initialProbabilityFactor - RIGHT_ANSWER_SUBTRAHEND * initialProbabilityMultiplier;
            afterAnswerProbabilityMultiplier = initialProbabilityMultiplier * RIGHT_ANSWER_MULTIPLIER;
            askedPhrase.setProbabilityFactor(afterAnswerProbabilityFactor);
            askedPhrase.setMultiplier(afterAnswerProbabilityMultiplier);


        }else{

            afterAnswerProbabilityFactor = initialProbabilityFactor;
            afterAnswerProbabilityMultiplier = initialProbabilityMultiplier;
            askedPhrase.setProbabilityFactor(afterAnswerProbabilityFactor);
            askedPhrase.setMultiplier(afterAnswerProbabilityMultiplier);

        }

        databaseHelper.updatePhrase(askedPhrase);
        return this;
    }

    public Question wrongAnswer(){
        this.answer = "Had not been given";
        this.answerCorrect = false;

        if(!phraseIsAlreadyTrained()){

            afterAnswerProbabilityFactor = initialProbabilityFactor + WRONG_ANSWER_ADDEND;
            afterAnswerProbabilityMultiplier = 1;
            askedPhrase.setProbabilityFactor(afterAnswerProbabilityFactor);
            askedPhrase.setMultiplier(afterAnswerProbabilityMultiplier);


        }else{

            afterAnswerProbabilityFactor = initialProbabilityFactor + WRONG_ANSWER_ADDEND * initialProbabilityMultiplier;
            afterAnswerProbabilityMultiplier = 1;
            askedPhrase.setProbabilityFactor(afterAnswerProbabilityFactor);
            askedPhrase.setMultiplier(afterAnswerProbabilityMultiplier);

        }

        databaseHelper.updatePhrase(askedPhrase);
        return this;
    }

    private boolean questionHasBeenAnswered(){
        return answer != null;
    }

    private boolean phraseIsAlreadyTrained(){
        return initialProbabilityFactor <= TRAINED_PROBABILITY_FACTOR;
    }

    /**
     * Gets the phrase in English as a parameter. If a slash occurs in the phrase (for example: car \ my auto) returns
     * a hint *** \ ** ****, if the phrase does not contain a slash ("/") returns ""
     * @return a hint *** \ ** ****, if the phrase does not contain a slash ("/") returns ""
     */
    public String longHint(){
        if(askedPhrase.foreignWord.contains("/")||askedPhrase.foreignWord.contains("\\")||askedPhrase.foreignWord.contains(" ")||askedPhrase.foreignWord.contains("-")||
                askedPhrase.foreignWord.contains("`")||askedPhrase.foreignWord.contains("'")||askedPhrase.foreignWord.contains(",")){
            char[] hintAr = askedPhrase.foreignWord.toCharArray();
            char[] newHintAr = new char[hintAr.length+2];
            int i = 1;
            newHintAr[0]='(';
            newHintAr[newHintAr.length-1]=')';
            for(char temp : hintAr){
                if(temp==' '){
                    newHintAr[i]=' ';
                    i++;
                }else if(temp=='/'){
                    newHintAr[i]='/';
                    i++;
                }else if(temp=='-'){
                    newHintAr[i]='-';
                    i++;
                }else if(temp=='`'){
                    newHintAr[i]='`';
                    i++;
                }else if(temp=='\''){
                    newHintAr[i]='\'';
                    i++;
                }else if(temp=='\\'){
                    newHintAr[i]='\\';
                    i++;
                }else if(temp==','){
                    newHintAr[i]=',';
                    i++;
                }else if(temp=='’'){
                    newHintAr[i]='’';
                    i++;
                }else{
                    newHintAr[i]='*';
                    i++;
                }
            }
            return new String(newHintAr);
        }else
            return "";
    }

    public String shortHint(){

        int numberOfVariants = 1;   // There is at least one
        int serialNumberOfHint = 0;
        StringBuilder finalHint = new StringBuilder("");
        boolean wasFirstSlash = false;

        for(char currentChar : askedPhrase.foreignWord.toCharArray()){     //Count numbers of variants
            if(currentChar == '/' || currentChar == '\\'){
                numberOfVariants++;
            }
        }

        if(numberOfVariants > 1){
            finalHint.append('(');
            for(int i = 0; i < numberOfVariants; i++){
                finalHint.append(wasFirstSlash ? "/" : "").append(++serialNumberOfHint);
                wasFirstSlash = true;
            }
            finalHint.append(')');
        }

        return finalHint.toString();

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

    public long getId() {
        return id;
    }

    public String getQuestionRepresentation() {
        return questionRepresentation;
    }

    public String getAnswer() {
        return answer;
    }

    public ZonedDateTime getAskDate() {
        return askDate;
    }

    public Phrase getAskedPhrase() {
        return askedPhrase;
    }

    public boolean answerIsCorrect() {
        return answered() && answerCorrect;
    }

    public boolean answered(){
        return answer != null;
    }
}
