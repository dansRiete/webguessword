package datamodel;

import logic.DAO;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;

/**
 * Created by Aleks on 11.05.2016.
 */

@Entity
@Table(name = "aleks")
public class Phrase implements Serializable{

    @javax.persistence.Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public int id;

    @Transient
    private static final double RIGHT_ANSWER_MULTIPLIER = 1.2;

    @Column(name = "for_word")
    public String foreignWord;

    @Column(name = "nat_word")
    public String nativeWord;

    @Column(name = "transcr")
    public String transcription;

    @Column(name = "prob_factor")
    public BigDecimal probabilityFactor;

    @Transient
    public BigDecimal previousProbabilityFactor;

    @Column
    public String label;

    @Transient
    public ZonedDateTime phraseAppearingTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));

    @Column(name = "create_date")
    public ZonedDateTime collectionAddingDateTime;

    @Column(name = "last_accs_date")
    public ZonedDateTime lastAccessDateTime;

    @Column
    public boolean exactMatch;

    @Transient
    public boolean hasBeenAnsweredCorrectly;

    @Transient
    public boolean hasBeenAnswered;

    @Transient
    public double indexStart;

    @Transient
    public double indexEnd;

    @Column(name = "rate")
    public double multiplier;

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

    public Phrase(int id, String foreignWord, String nativeWord, String transcription, BigDecimal probabilityFactor, ZonedDateTime collectionAddingDateTime, String label, ZonedDateTime lastAccessDateTime, double indexStart, double indexEnd, boolean exactMatch, double multiplier, DAO dao){
        this.id = id;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.transcription = transcription == null ? "" : transcription;
        this.probabilityFactor = probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        this.previousProbabilityFactor = probabilityFactor.setScale(1, RoundingMode.HALF_UP);
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

    //This construcor is used for tests only
    public Phrase(String foreignWord, String nativeWord){
        this.id = 0;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
    }




    public void rightAnswer(){

        if(!hasBeenAnswered){

            hasBeenAnsweredCorrectly = true;
            hasBeenAnswered = true;

            if(!isTrained()){

                double activeWordsAmountRatio = Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber());
                BigDecimal subtrahendForProb = new BigDecimal(3 * activeWordsAmountRatio * multiplier);
                probabilityFactor = probabilityFactor.subtract(subtrahendForProb);

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
                BigDecimal probFactorSubtrahend = new BigDecimal(3 * rateDepandableOnNumberOfWords * multiplier);
                probabilityFactor = previousProbabilityFactor.subtract(probFactorSubtrahend);

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

            multiplier = 1;
            System.out.println(probabilityFactor.setScale(1, BigDecimal.ROUND_HALF_UP) + "+=" + 6 + "*" + "Math.sqrt(" + dao.activePhrasesNumber() + "/" + dao.totalWordsNumber() + ")");
            probabilityFactor = probabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber())));
            dao.setStatistics(this);
            dao.updateProb(this);

        }else if(hasBeenAnsweredCorrectly){

            hasBeenAnsweredCorrectly = false;

            if(!wasTrainedBeforeAnswer()) {
                multiplier = 1;
                System.out.println(previousProbabilityFactor.setScale(1, BigDecimal.ROUND_HALF_UP) + "+=" + 6 + "*" + "Math.sqrt(" + dao.activePhrasesNumber() + "/" + dao.totalWordsNumber() + ")");
                probabilityFactor = previousProbabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber())));
            }else{
                System.out.println(previousProbabilityFactor.setScale(1, BigDecimal.ROUND_HALF_UP) + "+=" + 6 + "*" + "Math.sqrt(" + dao.activePhrasesNumber() + "/" + dao.totalWordsNumber() + ")*" + multiplier);
                probabilityFactor = previousProbabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.activePhrasesNumber() / dao.totalWordsNumber()) * multiplier));
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

        } else {
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
        return probabilityFactor.doubleValue() <= 3;
    }

    public boolean wasTrainedBeforeAnswer(){
        if(previousProbabilityFactor == null){
            return false;
        }else {
            return previousProbabilityFactor.doubleValue() <= 3;
        }
    }

    @Override
    public String toString() {
        return "Phrase{" +
                "id=" + id +
                ", foreignWord='" + foreignWord + '\'' +
                ", nativeWord='" + nativeWord + '\'' +
                '}';
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
    public BigDecimal getProbabilityFactor() {
        return probabilityFactor;
    }
    public void setProbabilityFactor(BigDecimal probabilityFactor) {
        System.out.println("CALL setProbabilityFactor("+ probabilityFactor +") from Phrase");
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
