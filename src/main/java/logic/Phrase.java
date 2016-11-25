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

    private static final double RIGHT_ANSWER_MULTIPLIER = 1.2;
    public final int id;
    public String foreignWord;
    public String nativeWord;
    public String transcription;
    public BigDecimal probabilityFactor;
    public BigDecimal beforeCurrentAnswerProbabilityFactor;
    public String label;
    public ZonedDateTime creationDate = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
    public Timestamp addingToCollectionDate;
    public Timestamp lastAccessDate;
    public boolean exactMatch;
    public boolean thisPhraseHadBeenAnsweredCorrectly;
    public boolean thisPhraseHadBeenAnswered;
    public double indexStart;
    public double indexEnd;
    public double multiplier;
    public double beforeCurrentAnswerMultiplier;
    public DAO dao;
    public boolean isModified;
    public String timeOfReturningFromList;

    public Phrase(int id, String foreignWord, String nativeWord, String transcription, BigDecimal probabilityFactor, Timestamp addingToCollectionDate,
                  String label, Timestamp lastAccessDate, double indexStart, double indexEnd, boolean exactMatch, double multiplier, DAO dao){
        this.id = id;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.transcription = transcription == null ? "" : transcription;
        this.probabilityFactor = probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        this.beforeCurrentAnswerProbabilityFactor = probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        this.addingToCollectionDate = addingToCollectionDate;
        this.label = (label == null ? "" : label);
        this.lastAccessDate = lastAccessDate;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.multiplier = multiplier <= 1 ? 1 : multiplier;
        this.beforeCurrentAnswerMultiplier = multiplier <= 1 ? 1 : multiplier;
        this.dao = dao;
    }

    public Phrase(Phrase givenPhrase){
        this.id = givenPhrase.id;
        this.foreignWord = givenPhrase.foreignWord;
        this.nativeWord = givenPhrase.nativeWord;
        this.transcription = givenPhrase.transcription;
        this.probabilityFactor = givenPhrase.probabilityFactor;
        this.beforeCurrentAnswerProbabilityFactor = givenPhrase.probabilityFactor;
        this.addingToCollectionDate = givenPhrase.addingToCollectionDate;
        this.label = givenPhrase.label;
        this.lastAccessDate = givenPhrase.lastAccessDate;
        this.indexStart = givenPhrase.indexStart;
        this.indexEnd = givenPhrase.indexEnd;
        this.exactMatch = givenPhrase.exactMatch;
        this.multiplier = givenPhrase.multiplier;
        this.beforeCurrentAnswerMultiplier = multiplier <= 1 ? 1 : multiplier;
        this.dao = givenPhrase.dao;
    }

    //This construcor is used for tests only
    public Phrase(String foreignWord, String nativeWord){
        this.id = 0;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
    }


    public void rightAnswer(){
        long[] indexes;

        if(!thisPhraseHadBeenAnswered){

            if(!hasThisPhraseBeenLearnt()){

                double activeWordsAmountRatio = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount);
                BigDecimal subtrahendForProb = new BigDecimal(3 * activeWordsAmountRatio * multiplier);
                probabilityFactor = probabilityFactor.subtract(subtrahendForProb);

                if(activeWordsAmountRatio > 0.6) {
                    if (multiplier <= 1) {
                        multiplier = RIGHT_ANSWER_MULTIPLIER;
                    } else {
                        multiplier *= RIGHT_ANSWER_MULTIPLIER;
                    }
                }
            }

            dao.setStatistics(this);


        }else if(!thisPhraseHadBeenAnsweredCorrectly){

            if(!hadThisPhraseBeenLearntBeforeCurrentAnswer()){

                probabilityFactor = beforeCurrentAnswerProbabilityFactor;
                double rateDepandableOnNumberOfWords = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount);
                multiplier = beforeCurrentAnswerMultiplier;
                BigDecimal probFactorSubtrahend = new BigDecimal(3 * rateDepandableOnNumberOfWords * multiplier);
                probabilityFactor = probabilityFactor.subtract(probFactorSubtrahend);

                if(rateDepandableOnNumberOfWords > 0.6) {
                    if (multiplier <= 1) {
                        multiplier = RIGHT_ANSWER_MULTIPLIER;
                    } else {
                        multiplier *= RIGHT_ANSWER_MULTIPLIER;
                    }
                }

            } else {
                probabilityFactor = beforeCurrentAnswerProbabilityFactor;
                multiplier = beforeCurrentAnswerMultiplier;
            }

            dao.setStatistics(this);
        }

        thisPhraseHadBeenAnsweredCorrectly = true;
        thisPhraseHadBeenAnswered = true;
        indexes = dao.updateProb(this);

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public void wrongAnswer(){

        long[] indexes = null;

        if(!thisPhraseHadBeenAnswered){
            multiplier = 1;
            probabilityFactor = probabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount)));
            thisPhraseHadBeenAnsweredCorrectly = false;
            indexes = dao.updateProb(this);
            dao.setStatistics(this);
            dao.updateStatistics(this);

        }else if(thisPhraseHadBeenAnsweredCorrectly){

            if(!hadThisPhraseBeenLearntBeforeCurrentAnswer()) {
                multiplier = 1;
                probabilityFactor = probabilityFactor.add(new BigDecimal(9 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount)));
            }else{
                probabilityFactor = probabilityFactor.add(new BigDecimal(6 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount) * multiplier));
                multiplier = 1;
            }
            dao.updateStatistics(this);
        }

        thisPhraseHadBeenAnsweredCorrectly = false;
        thisPhraseHadBeenAnswered = true;
        indexes = dao.updateProb(this);

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
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
        this.beforeCurrentAnswerMultiplier = multiplier;
        this.beforeCurrentAnswerProbabilityFactor = probabilityFactor;
    }

    public String getForWordAndTranscription(){
        return foreignWord + (transcription.equalsIgnoreCase("") ? "" : (" [" + transcription + "]"));
    }

    public void deleteThisPhrase(){
        System.out.println("CALL deleteThisPhrase(), requested id=" + id);
        dao.deletePhrase(this);
    }

    public void updatePhraseInDb(){
        dao.updatePhrase(this);
    }

    public boolean hasThisPhraseBeenLearnt(){
        return probabilityFactor.doubleValue() <= 3;
    }

    public boolean hadThisPhraseBeenLearntBeforeCurrentAnswer(){
        return beforeCurrentAnswerProbabilityFactor.doubleValue() <= 3;
    }

    public String getForeignWord(){
        return foreignWord;
    }
    public int getId() {
        return id;
    }
    public void setForeignWord(String foreignWord) {
        System.out.println("CALL setForeignWord("+ foreignWord +") from Phrase");
        this.foreignWord = foreignWord;
        updatePhraseInDb();
    }
    public String getNativeWord() {
        return nativeWord;
    }
    public void setNativeWord(String nativeWord) {
        System.out.println("CALL setNativeWord("+ nativeWord +") from Phrase");
        this.nativeWord = nativeWord;
        updatePhraseInDb();
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
