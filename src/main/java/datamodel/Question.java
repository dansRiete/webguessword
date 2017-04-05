package datamodel;

import logic.DatabaseHelper;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 11.11.2016.
 */

@Entity
@Table(name = "questions")
public class Question implements Serializable{

    @Transient
    private static final double RIGHT_ANSWER_MULTIPLIER = 1.44;

    @Transient
    private static final double RIGHT_ANSWER_SUBTRAHEND = 3;

    @Transient
    private static final double WRONG_ANSWER_ADDEND = 6;

    @Transient
    private static final double TRAINED_PROBABILITY_FACTOR = 3;

    @Transient
    private static final int PROBABILITY_FACTOR_ACCURACY = 1;

    @Transient
    private static final int MULTIPLIER_ACCURACY = 2;

    @javax.persistence.Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer")
    private String answer;

    @Column(name = "date")
    private ZonedDateTime askDate = ZonedDateTime.now();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "phrase_key")
    private Phrase askedPhrase;

    @Column(name = "answered_correctly")
    private boolean answerCorrect;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    private long initStartIndex;

    @Transient
    private long initEndIndex;

    @Transient
    private long afterAnswerStartIndex;

    @Transient
    private long afterAnswerEndIndex;

    @Column(name = "init_probability_factor")
    private double initialProbabilityFactor;

    @Column(name = "init_multiplier")
    private double initialProbabilityMultiplier;

    @Column(name = "answered_probability_factor")
    private double afterAnswerProbabilityFactor;

    @Column(name = "answered_multiplier")
    private double afterAnswerProbabilityMultiplier;

    @Transient
    private DatabaseHelper databaseHelper;

    @Transient
    private String questionRepresentation;

    @Transient
    private ZonedDateTime initLastAccessDate;

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    @Transient
    private boolean answered;

    public Question(){}

    private Question(Phrase askedPhrase, DatabaseHelper databaseHelper) {
        System.out.println("CALL: Question(Phrase askedPhrase, DatabaseHelper databaseHelper) from Question");

        this.askedPhrase = askedPhrase;
        this.databaseHelper = databaseHelper;
        initialProbabilityFactor = askedPhrase.getProbabilityFactor();
        initialProbabilityMultiplier = askedPhrase.getMultiplier();
        initStartIndex = askedPhrase.getIndexStart();
        initEndIndex = askedPhrase.getIndexEnd();
        initLastAccessDate = askedPhrase.getLastAccessDateTime();
        questionRepresentation = askedPhrase.nativeWord + " " + shortHint();
        user = askedPhrase.getOwner();
    }

    public static Question compose(Phrase askedPhrase, DatabaseHelper dbHelper){
        System.out.println("CALL: compose(Phrase askedPhrase, DatabaseHelper dbHelper) from Question");
        if(askedPhrase == null){
            throw new IllegalArgumentException("Phrases foreign and native literals can not be null");
        }
        Question question = new Question(askedPhrase, dbHelper);
        askedPhrase.lastAccessDateTime = ZonedDateTime.now();
        return question;
    }

    public Question answerTheQuestion(String answer){
        System.out.println("CALL: answerTheQuestion(String answer) from Question");
        if(!answered()){
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
                this.answerCorrect = matchesAmount == referenceLiteralPhrases.length;
            }
            if(this.answerCorrect){
                rightAnswer();
            }else {
                wrongAnswer();
            }
        }
        return this;
    }

    public Question rightAnswer(){
        System.out.println("CALL: rightAnswer() from Question");

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

        databaseHelper.updateProb(askedPhrase);
        afterAnswerStartIndex = askedPhrase.getIndexStart();
        afterAnswerEndIndex = askedPhrase.getIndexEnd();

        this.answerCorrect = true;

        if(!answered()){
            if(this.answer == null || this.answer.equals("")){
                this.answer = askedPhrase.getForeignWord();
            }
            databaseHelper.peristQuestion(this);

        }else {
            this.answer = askedPhrase.getForeignWord();
            databaseHelper.updateQuestion(this);
        }

        this.answered = true;
        return this;
    }

    public Question wrongAnswer(){
        System.out.println("CALL: wrongAnswer() from Question");

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

        databaseHelper.updateProb(askedPhrase);
        afterAnswerStartIndex = askedPhrase.getIndexStart();
        afterAnswerEndIndex = askedPhrase.getIndexEnd();

        this.answerCorrect = false;

        if(!answered()){
            databaseHelper.peristQuestion(this);
        }else {
            this.answer = null;
            databaseHelper.updateQuestion(this);
        }

        this.answered = true;
        return this;
    }

    private boolean phraseIsAlreadyTrained(){
        return initialProbabilityFactor <= TRAINED_PROBABILITY_FACTOR;
    }

    public String composeProbabilityFactorHistory(){
        if(!answered()){
            return new BigDecimal(initialProbabilityFactor).setScale(PROBABILITY_FACTOR_ACCURACY, BigDecimal.ROUND_HALF_UP).toString();
        }else {
            BigDecimal beforeProbabilityFactor = new BigDecimal(initialProbabilityFactor).setScale(MULTIPLIER_ACCURACY, BigDecimal.ROUND_HALF_UP);
            BigDecimal afterProbabilityFactor = new BigDecimal(afterAnswerProbabilityFactor).setScale(MULTIPLIER_ACCURACY, BigDecimal.ROUND_HALF_UP);
            return beforeProbabilityFactor.toString() + " ➩ " + afterProbabilityFactor.toString() + " (" +
                    (afterProbabilityFactor.doubleValue() > beforeProbabilityFactor.doubleValue() ? "+" : "") +
                    afterProbabilityFactor.subtract(beforeProbabilityFactor) + ")";
        }
    }

    public String composeMultiplierHistory(){
        if(!answered()){
            return new BigDecimal(initialProbabilityMultiplier).setScale(MULTIPLIER_ACCURACY, BigDecimal.ROUND_HALF_UP).toString();
        }else {
            BigDecimal beforeMultiplier = new BigDecimal(initialProbabilityMultiplier).setScale(MULTIPLIER_ACCURACY, BigDecimal.ROUND_HALF_UP);
            BigDecimal afterMultiplier = new BigDecimal(afterAnswerProbabilityMultiplier).setScale(MULTIPLIER_ACCURACY, BigDecimal.ROUND_HALF_UP);
            return beforeMultiplier.toString() + " ➩ " + afterMultiplier.toString()  + " (" +
                    (afterMultiplier.doubleValue() > beforeMultiplier.doubleValue() ? "+" : "") + afterMultiplier.subtract(beforeMultiplier) + ")";
        }
    }

    public String composeLastAccessDate(){
            if(this.initLastAccessDate != null){
                return this.initLastAccessDate.format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"));
            }else {
                return "NEVER ACCESSED";
            }
    }

    public String composeCreationDate(){
        return this.askedPhrase.getCollectionAddingDateTime().format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"));
    }

    public String composeLabel(){
        if(this.askedPhrase.getLabel() != null){
            return this.askedPhrase.getLabel();
        }else {
            return "";
        }
    }

    public String composeAppearingPercentage(){
        String appearingPercentage =  new BigDecimal((double) (initEndIndex - initStartIndex) / (double) databaseHelper.getTheGreatestPhrasesIndex() * 100).setScale(5, BigDecimal.ROUND_HALF_UP).toString();
        if(answered()){
            appearingPercentage +=  " ➩ " +
                    new BigDecimal((double) (afterAnswerEndIndex - afterAnswerStartIndex) / (double) databaseHelper.getTheGreatestPhrasesIndex() * 100).setScale(5, BigDecimal.ROUND_HALF_UP);
        }
        return appearingPercentage;
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

    public boolean answerIsCorrect() {
        return answered() && answerCorrect;
    }

    public boolean answered(){
        return answered;
    }

    public int trainedAfterAnswer(){
        if(answered()){
            if(initialProbabilityFactor > Phrase.TRAINED_PROBABILITY_FACTOR && afterAnswerProbabilityFactor <= Phrase.TRAINED_PROBABILITY_FACTOR){
                return 1;
            }else  if(initialProbabilityFactor <= Phrase.TRAINED_PROBABILITY_FACTOR && afterAnswerProbabilityFactor > Phrase.TRAINED_PROBABILITY_FACTOR){
                return -1;
            }
        }
        return 0;
    }

    //Getters and setters

    public ZonedDateTime getAskDate() {
        return askDate;
    }

    public Phrase getAskedPhrase() {
        return askedPhrase;
    }

    public Long getId() {
        return id;
    }

    public String getQuestionRepresentation() {
        return questionRepresentation;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isAnswerCorrect() {
        return answerCorrect;
    }
}
