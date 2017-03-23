package datamodel;

import logic.DAO;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;

/**
 * Created by Aleks on 11.05.2016.
 */

@Entity
@Table(name = "words")
public class Phrase implements Serializable{

    @Transient
    private static final double RIGHT_ANSWER_MULTIPLIER = 1.44;

    @javax.persistence.Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int id;

    @Column(name = "for_word")
    public String foreignWord;

    @Column(name = "nat_word")
    public String nativeWord;

    @Column(name = "transcr")
    public String transcription;

    @Column(name = "prob_factor")
    public double probabilityFactor;

    @Column
    public String label;

    @Column(name = "create_date")
    public ZonedDateTime collectionAddingDateTime;

    @Column(name = "last_accs_date")
    public ZonedDateTime lastAccessDateTime;

    @Column
    public boolean exactMatch;

    @Column(name = "rate")
    public double multiplier;

    @Transient
    public double previousProbabilityFactor;

    @Transient
    public ZonedDateTime phraseAppearingTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));

    @Transient
    public boolean hasBeenAnsweredCorrectly;

    @Transient
    public boolean hasBeenAnswered;

    @Transient
    public double indexStart;

    @Transient
    public double indexEnd;

    @Transient
    public double previousMultiplier;

    @Transient
    public DAO dao;

    @Transient
    public boolean isModified;

    @Transient
    public String timeOfReturningFromList;

    public Phrase() {
    }

    public Phrase(int id, String foreignWord, String nativeWord, String transcription, double probabilityFactor,
                  ZonedDateTime collectionAddingDateTime, String label, ZonedDateTime lastAccessDateTime,
                  double indexStart, double indexEnd, boolean exactMatch, double multiplier, DAO dao){
        this.id = id;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.transcription = transcription == null ? "" : transcription;
        this.probabilityFactor = probabilityFactor;
        this.previousProbabilityFactor = probabilityFactor;
        this.collectionAddingDateTime = collectionAddingDateTime;
        this.label = (label == null ? "" : label);
        this.lastAccessDateTime = lastAccessDateTime;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.multiplier = multiplier <= 1 ? 1 : multiplier;
        this.previousMultiplier = multiplier <= 1 ? 1 : multiplier;
        this.dao = dao;
    }

    public Phrase(Phrase givenPhrase){
        this.id = givenPhrase.id;
        this.foreignWord = givenPhrase.foreignWord;
        this.nativeWord = givenPhrase.nativeWord;
        this.transcription = givenPhrase.transcription;
        this.probabilityFactor = givenPhrase.probabilityFactor;
        this.previousProbabilityFactor = givenPhrase.probabilityFactor;
        this.collectionAddingDateTime = givenPhrase.collectionAddingDateTime;
        this.label = givenPhrase.label;
        this.lastAccessDateTime = givenPhrase.lastAccessDateTime;
        this.indexStart = givenPhrase.indexStart;
        this.indexEnd = givenPhrase.indexEnd;
        this.exactMatch = givenPhrase.exactMatch;
        this.multiplier = givenPhrase.multiplier;
        this.previousMultiplier = multiplier <= 1 ? 1 : multiplier;
        this.dao = givenPhrase.dao;
    }

    /*//This construcor is used for tests only
    public Phrase(String foreignWord, String nativeWord){
        this.id = 0;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
    }*/




    public void rightAnswer(){

        if(!hasBeenAnswered){

            hasBeenAnsweredCorrectly = true;
            hasBeenAnswered = true;

            if(!isTrained()){

                double activeWordsAmountRatio = Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber());
                double subtrahendForProb = 3 * activeWordsAmountRatio * multiplier;
                probabilityFactor = probabilityFactor -= subtrahendForProb;

                if(activeWordsAmountRatio > 0.6) {
                    if (multiplier <= 1) {
                        multiplier = RIGHT_ANSWER_MULTIPLIER;
                    } else {
                        multiplier *= RIGHT_ANSWER_MULTIPLIER;
                    }
                }
                dao.updateProb(this);
            }
            dao.setStatistics(this);

        }else if(!hasBeenAnsweredCorrectly){

            hasBeenAnsweredCorrectly = true;

            if(!wasTrainedBeforeAnswer()){

//                probabilityFactor = previousProbabilityFactor;
                double rateDepandableOnNumberOfWords = Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber());
                multiplier = previousMultiplier;
                double probFactorSubtrahend = 3 * rateDepandableOnNumberOfWords * multiplier;
                probabilityFactor = previousProbabilityFactor -= probFactorSubtrahend;

                if(rateDepandableOnNumberOfWords > 0.6) {
                    if (multiplier <= 1) {
                        multiplier = RIGHT_ANSWER_MULTIPLIER;
                    } else {
                        multiplier *= RIGHT_ANSWER_MULTIPLIER;
                    }
                }

            } else {
                probabilityFactor = previousProbabilityFactor;
                multiplier = previousMultiplier;
            }

            dao.updateStatistics(this);
            dao.updateProb(this);
        }
    }

    public void wrongAnswer(){

        if(!hasBeenAnswered){

            hasBeenAnswered = true;
            hasBeenAnsweredCorrectly = false;

            System.out.println(new BigDecimal(probabilityFactor).setScale(1, BigDecimal.ROUND_HALF_UP) + " += " + 6 + " * " + multiplier + " * " + "Math.sqrt(" + dao.activePhrasesNumber() + "/" + dao.totalWordsNumber() + ")");
            probabilityFactor  += (6 * multiplier * Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber()));
            multiplier = 1;
            dao.setStatistics(this);
            dao.updateProb(this);

        }else if(hasBeenAnsweredCorrectly){

            hasBeenAnsweredCorrectly = false;

            if(!wasTrainedBeforeAnswer()) {
                System.out.println(new BigDecimal(previousProbabilityFactor).setScale(1, BigDecimal.ROUND_HALF_UP) + " += " + 6 + " * " + previousMultiplier + " * " + "Math.sqrt(" + dao.activePhrasesNumber() + "/" + dao.totalWordsNumber() + ")");
                probabilityFactor += (6 * previousMultiplier * Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber()));
                multiplier = 1;
            }else{
                System.out.println(new BigDecimal(previousProbabilityFactor).setScale(1, BigDecimal.ROUND_HALF_UP) + " += " + 6 + " * " + previousMultiplier + " * " + "Math.sqrt(" + dao.activePhrasesNumber() + "/" + dao.totalWordsNumber() + ")*" + previousMultiplier);
                probabilityFactor += (6 * previousMultiplier * Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber()) * previousMultiplier);
                multiplier = 1;
            }
            dao.updateStatistics(this);
            dao.updateProb(this);
        }
    }

    public boolean isThisPhraseInList(HashSet<String> phrasesList){

        if(phrasesList != null){
            if(phrasesList.isEmpty()) {
                return true;
            }
            for(String str : phrasesList){
                if(this.label != null && this.label.equalsIgnoreCase(str)){
                    return true;
                }
            }
            return false;
        }else{
            return true;
        }
    }

    public void resetPreviousValues(){
        this.previousMultiplier = multiplier;
        this.previousProbabilityFactor = probabilityFactor;
    }

    public String getForWordAndTranscription(){
        if(transcription == null){
            return foreignWord;
        }else {
            return foreignWord + (transcription.equalsIgnoreCase("") ? "" : (" [" + transcription + "]"));
        }
    }

    public void deleteThisPhrase(){
        System.out.println("CALL deleteThisPhrase(), requested id=" + id);
        dao.deletePhrase(this);
    }

    public void updatePhraseInDb(){
        if(dao != null)
            dao.updatePhrase(this);
    }

    public boolean isTrained(){
        return probabilityFactor <= 3;
    }

    public boolean wasTrainedBeforeAnswer(){
        return previousProbabilityFactor <= 3;
    }

    @Override
    public String toString() {
        return "Phrase{" +
                "id=" + id +
                ", foreignWord='" + foreignWord + '\'' +
                ", nativeWord='" + nativeWord + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return foreignWord.hashCode() * (nativeWord.hashCode() + 21);
    }

    //Setters and getters

    public int getId() {
        return id;
    }
    public String getForeignWord(){
        return foreignWord;
    }
    public Phrase setForeignWord(String foreignWord) {
        System.out.println("CALL setForeignWord("+ foreignWord +") from Phrase");
        this.foreignWord = foreignWord;
        updatePhraseInDb();
        return this;
    }
    public String getNativeWord() {
        return nativeWord;
    }
    public Phrase setNativeWord(String nativeWord) {
        System.out.println("CALL setNativeWord("+ nativeWord +") from Phrase");
        this.nativeWord = nativeWord;
        updatePhraseInDb();
        return this;
    }
    public String getTranscription() {
        return transcription;
    }
    public void setTranscription(String transcription) {
        System.out.println("CALL setTranscription("+ transcription +") from Phrase");
        this.transcription = transcription;
        updatePhraseInDb();
    }
    public double getProbabilityFactor() {
        return probabilityFactor;
    }
    public void setProbabilityFactor(double probabilityFactor) {
        this.probabilityFactor = probabilityFactor;
        updatePhraseInDb();
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        System.out.println("CALL setLabel("+label+") from Phrase");
        this.label = label;
        updatePhraseInDb();
    }
    public ZonedDateTime getCollectionAddingDateTime() {
        return collectionAddingDateTime;
    }
    public ZonedDateTime getLastAccessDateTime() {
        return lastAccessDateTime;
    }
    public void setLastAccessDateTime(ZonedDateTime lastAccessDateTime) {
        this.lastAccessDateTime = lastAccessDateTime;
    }
    public void setTimeOfReturningFromList(long time){
        timeOfReturningFromList = Double.toString((double) time / 1000000d);
    }
    public int getIndexStart() {
        return (int) indexStart;
    }
    public void setIndexStart(long indexStart) {
        this.indexStart = indexStart;
    }
    public int getIndexEnd() {
        return (int) indexEnd;
    }
    public void setIndexEnd(long indexEnd) {
        this.indexEnd = indexEnd;
    }
    public DAO getDao() {
        return dao;
    }
    public void setDao(DAO dao) {
        this.dao = dao;
    }


}
