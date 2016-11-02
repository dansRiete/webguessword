package logic;

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
public class Phrase implements Serializable{

    private static final double RIGHT_ANSWER_RATIO = 1.2;
    public final int id;
    public String foreignWord;
    public String nativeWord;
    public String transcription;
    public BigDecimal probabilityFactor;
    public BigDecimal previousProbabilityFactor;
    public String label;
    public ZonedDateTime creationDate = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
    public Timestamp addingToCollectionDate;
    public Timestamp lastAccessDate;
    public boolean exactMatch;
    public boolean answeredCorrectly;
    public boolean wasAnswered = false;
    public double indexStart;
    public double indexEnd;
    public double multiplier;
    public double previousMultiplier;
    public DAO dao;
    public boolean isModified;
//    public String answer;
    public String timeOfReturningFromList;

    public Phrase(int id, String foreignWord, String nativeWord, String transcription, BigDecimal probabilityFactor, Timestamp addingToCollectionDate,
                  String label, Timestamp lastAccessDate, double indexStart, double indexEnd, boolean exactMatch, double multiplier, DAO dao){
        this.dao = dao;
        this.id = id;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.transcription = transcription == null ? "" : transcription;
        this.probabilityFactor = probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        this.previousProbabilityFactor = probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        this.addingToCollectionDate = addingToCollectionDate;
        this.label = (label == null ? "" : label);
        this.lastAccessDate = lastAccessDate;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.multiplier = multiplier <= 1 ? 1 : multiplier;
        this.previousMultiplier = multiplier <= 1 ? 1 : multiplier;
    }

    public Phrase(Phrase givenPhrase){
        this.dao = givenPhrase.dao;
        this.id = givenPhrase.id;
        this.foreignWord = givenPhrase.foreignWord;
        this.nativeWord = givenPhrase.nativeWord;
        this.transcription = givenPhrase.transcription;
        this.probabilityFactor = givenPhrase.probabilityFactor;
        this.previousProbabilityFactor = givenPhrase.probabilityFactor;
        this.addingToCollectionDate = givenPhrase.addingToCollectionDate;
        this.label = givenPhrase.label;
        this.lastAccessDate = givenPhrase.lastAccessDate;
        this.indexStart = givenPhrase.indexStart;
        this.indexEnd = givenPhrase.indexEnd;
        this.exactMatch = givenPhrase.exactMatch;
        this.multiplier = givenPhrase.multiplier;
        this.previousMultiplier = multiplier <= 1 ? 1 : multiplier;
    }

    public void resetPreviousValues(){
        this.previousMultiplier = multiplier;
        this.previousProbabilityFactor = probabilityFactor;
    }

    public void rightAnswer(String givenAnswer){
        long[] indexes;
//        this.answer = givenAnswer;

        if(!wasAnswered){     //Ответ на фразу первый раз

            if(!isLearnt()){

                double activeWordsAmountRatio = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount);
                System.out.println("activeWordsAmountRatio = " + activeWordsAmountRatio);
                BigDecimal subtrahendForProb = new BigDecimal(3 * activeWordsAmountRatio * multiplier);

                if(activeWordsAmountRatio > 0.6) {
                    if (multiplier <= 1) {
                        multiplier = RIGHT_ANSWER_RATIO;
                    } else {
                        multiplier *= RIGHT_ANSWER_RATIO;
                    }
                }
                probabilityFactor = probabilityFactor.subtract(subtrahendForProb);
            }
            dao.setStatistics(this);


        }else if(!answeredCorrectly){      //если true значит на фразу уже был неправильный ответ

            if(!previousIsLearnt()){   //Если до ответа на фразу она не была изучена

                probabilityFactor = previousProbabilityFactor;
                double rateDepandableOnNumberOfWords = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount);
                System.out.println("multiplier = " + multiplier + " ");
                multiplier = previousMultiplier;
                System.out.print("multiplier = " + multiplier + " ");
                BigDecimal subtr = new BigDecimal(3 * rateDepandableOnNumberOfWords * multiplier);

                if(rateDepandableOnNumberOfWords > 0.6) {
                    if (multiplier <= 1) {
                        multiplier = RIGHT_ANSWER_RATIO;
                    } else {
                        multiplier *= RIGHT_ANSWER_RATIO;
                    }
                }

                probabilityFactor = probabilityFactor.subtract(subtr);

            } else {      //Если была, просто возвращаем первоначальное значение probabilityFactor

                probabilityFactor = previousProbabilityFactor;
                multiplier = previousMultiplier;
            }
            dao.setStatistics(this);
        }

        answeredCorrectly = true;
        wasAnswered = true;

        indexes = dao.updateProb(this);

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public void wrongAnswer(String answer){

        long[] indexes = null;
//        this.answer = answer;

        if(!wasAnswered){     // The first answer
            multiplier = 1;
            probabilityFactor = probabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount)));
            answeredCorrectly = false;
            indexes = dao.updateProb(this);
            dao.setStatistics(this);
            dao.updateStatistics(this);

        }else if(answeredCorrectly){

            if(!previousIsLearnt()) {
                multiplier = 1;
                probabilityFactor = probabilityFactor.add(new BigDecimal(9 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount)));
            }else{
                probabilityFactor = probabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount) * multiplier));
                multiplier = 1;
            }
            dao.updateStatistics(this);
        }

        answeredCorrectly = false;
        wasAnswered = true;
        indexes = dao.updateProb(this);

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public boolean isInLabels(HashSet<String> hashSet){

        if(hashSet!=null){
            if(hashSet.isEmpty())
                return true;
            for(String str : hashSet){
                if(this.label!=null&&this.label.equalsIgnoreCase(str))
                    return true;
            }
            return false;
        } else {
            return true;
        }
    }

    public String getForWordAndTranscription(){
        return foreignWord + (transcription.equalsIgnoreCase("") ? "" : (" [" + transcription + "]"));
    }

    public void delete(){
        System.out.println("CALL delete(), requested id=" + id);
        dao.deletePhrase(this);
    }

    public void updatePhrase(){
        dao.updatePhrase(this);
    }

    public boolean isLearnt(){
        return probabilityFactor.doubleValue() <= 3;
    }

    public boolean previousIsLearnt(){
        return previousProbabilityFactor.doubleValue() <= 3;
    }

    public String getForeignWord(){
        return foreignWord;
    }

    public String toString(){
        return foreignWord + " - " + nativeWord + " last. accs:" + lastAccessDate;
    }

    public int getId() {
        return id;
    }

    public void setForeignWord(String foreignWord) {
        System.out.println("CALL setForeignWord("+ foreignWord +") from Phrase");
        this.foreignWord = foreignWord;
        updatePhrase();
    }

    public String getNativeWord() {
        return nativeWord;
    }

    public void setNativeWord(String nativeWord) {
        System.out.println("CALL setNativeWord("+ nativeWord +") from Phrase");
        this.nativeWord = nativeWord;
        updatePhrase();
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        System.out.println("CALL setTranscription("+ transcription +") from Phrase");
        this.transcription = transcription;
        updatePhrase();
    }

    public BigDecimal getProbabilityFactor() {
        return probabilityFactor;
    }

    public void setProbabilityFactor(BigDecimal probabilityFactor) {
        System.out.println("CALL setProbabilityFactor("+ probabilityFactor +") from Phrase");
        this.probabilityFactor = probabilityFactor;
        updatePhrase();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        System.out.println("CALL setLabel("+label+") from Phrase");
        this.label = label;
        updatePhrase();
    }

    public Timestamp getAddingToCollectionDate() {
        return addingToCollectionDate;
    }

    public Timestamp getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(Timestamp lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
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


}
