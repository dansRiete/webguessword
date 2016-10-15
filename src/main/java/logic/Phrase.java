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

    public static final double RIGHT_ANSWER_RATIO = 1.2;
    public int id;
    public String forWord;
    public String natWord;
    public String transcr;
    public BigDecimal prob;
    public String label;
    public Timestamp addingToCollectionDate;
    public Timestamp lastAccessDate;
    public boolean exactMatch;
    public ZonedDateTime creationDate = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
    public Boolean answeredCorrectly;
    public boolean wasAnswered = false;
    public double indexStart;
    public double indexEnd;
    public double rate = 1;
    private DAO dao;
    public boolean isModified;
    public String answer;
    public String timeOfReturningFromList;
    public Phrase originalPhrase;

    public Phrase(int id, String forWord, String natWord, String transcr, BigDecimal prob, Timestamp addingToCollectionDate,
                  String label, Timestamp lastAccessDate, double indexStart, double indexEnd, boolean exactMatch, double rate, DAO dao){
        this.dao = dao;
        this.id = id;
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr == null ? "" : transcr;
        this.prob = prob.setScale(1, RoundingMode.HALF_UP);
        this.addingToCollectionDate = addingToCollectionDate;
        this.label = label==null?"":label;
        this.lastAccessDate = lastAccessDate;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.rate = rate <= 1 ? 1 : rate;
        this.originalPhrase = new Phrase(forWord, natWord, transcr, prob, addingToCollectionDate, label, lastAccessDate,
                indexStart, indexEnd, exactMatch, rate);
    }

    public Phrase(String forWord, String natWord, String transcr, BigDecimal prob, Timestamp addingToCollectionDate,
                  String label, Timestamp lastAccessDate, double indexStart, double indexEnd, boolean exactMatch, double rate){
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr==null?"":transcr;
        this.prob = prob.setScale(1, RoundingMode.HALF_UP);
        this.addingToCollectionDate = addingToCollectionDate;
        this.label = label==null?"":label;
        this.lastAccessDate = lastAccessDate;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.rate = rate;
    }

    public Phrase(String forWord, String natWord, String transcr, String label){
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr==null?"":transcr;
        this.prob = new BigDecimal(30);
        this.addingToCollectionDate = new Timestamp(System.currentTimeMillis());
        this.label = label==null?"":label;
        this.exactMatch = false;
    }


    public Phrase returnUnmodified(){
        return originalPhrase;
    }

    public void rightAnswer(String answer){
        long[] indexes;
        this.answer = answer;

        if(!wasAnswered){     //Ответ на фразу первый раз

            if(!isLearnt()){

                double activeWordsAmountRatio = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount);
                BigDecimal subtrahendForProb = new BigDecimal(3 * activeWordsAmountRatio * rate);

                if(activeWordsAmountRatio>0.6) {
                    if (rate <= 1) {
                        rate = RIGHT_ANSWER_RATIO;
                    } else {
                        rate *= RIGHT_ANSWER_RATIO;
                    }
                }
                prob = prob.subtract(subtrahendForProb);
            }


        }else if(!answeredCorrectly){      //если true значит на фразу уже был неправильный ответ

            if(!originalPhrase.isLearnt()){   //Если до ответа на фразу она не была изучена

                prob = originalPhrase.prob;
                double rateDepandableOnNumberOfWords = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount);
                rate = originalPhrase.rate;
                BigDecimal subtr = new BigDecimal(3 * rateDepandableOnNumberOfWords * rate);

                if(rateDepandableOnNumberOfWords > 0.6) {
                    if (rate <= 1) {
                        rate = RIGHT_ANSWER_RATIO;
                    } else {
                        rate *= RIGHT_ANSWER_RATIO;
                    }
                }

                prob = prob.subtract(subtr);

            } else {      //Если была, просто возвращаем первоначальное значение prob

                prob = originalPhrase.prob;
                rate = originalPhrase.rate;
            }
        }

        answeredCorrectly = true;
        wasAnswered = true;
        dao.setStatistics(this);
        indexes = dao.updateProb(this);

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public void wrongAnswer(String answer){

        long[] indexes = null;
        this.answer = answer;

        if(!wasAnswered){     // The first answer
            rate = 1;
            BigDecimal summ = new BigDecimal(6 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount));
            prob = prob.add(summ);
            answeredCorrectly = false;
            indexes = dao.updateProb(this);
            dao.setStatistics(this);

        }else if(answeredCorrectly){

            if(!originalPhrase.isLearnt()) {
                rate = 1;
                BigDecimal summ = new BigDecimal(9 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWordsAmount));
                prob = prob.add(summ);
            }else{
                BigDecimal summ = new BigDecimal(6 * rate);
                prob = prob.add(summ);
                rate = 1;
            }
        }

        answeredCorrectly = false;
        wasAnswered = true;
        indexes = dao.updateProb(this);
        dao.updateStatistics(this);

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public boolean inLabels(HashSet<String> hashSet){

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

    public String getPercentChanceView(){

        System.out.println("indexEnd - indexStart " + indexStart + " - " + indexEnd);
        System.out.println("returnUnmodified.indexEnd - returnUnmodified.indexStart " + returnUnmodified().indexStart + " - " + returnUnmodified().indexEnd);
        if(!wasAnswered)
            return new BigDecimal(indexEnd - indexStart).divide(new BigDecimal(1.0e+9*100), BigDecimal.ROUND_HALF_UP).setScale(5, RoundingMode.HALF_UP) + "%";
        else {
            BigDecimal currentPercentChance = new BigDecimal(indexEnd - indexStart).divide(new BigDecimal(1.0e+9*100), BigDecimal.ROUND_HALF_UP).setScale(5, RoundingMode.HALF_UP);
            BigDecimal previousPercentChance = new BigDecimal(returnUnmodified().indexEnd - returnUnmodified().indexStart).divide(new BigDecimal(1.0e+9*100), BigDecimal.ROUND_HALF_UP).setScale(5, RoundingMode.HALF_UP);
            return previousPercentChance + "% ➩ " + currentPercentChance + "%";
        }
    }

    public String getForWordAndTranscription(){
        return forWord + (transcr.equalsIgnoreCase("") ? "" : (" [" + transcr + "]"));
    }

    public void delete(){
        System.out.println("CALL delete(), requested id=" + id);
        dao.deletePhrase(this);
    }

    public void updatePhrase(){
        dao.updatePhrase(this);
    }

    public boolean isLearnt(){
        return prob.doubleValue()<=3;
    }

    public String getForWord(){
        return forWord;
    }

    public String toString(){
        return forWord + " - " + natWord + " last. accs:" + lastAccessDate;
    }

    public int getId() {
        return id;
    }

    public void setForWord(String forWord) {
        System.out.println("CALL setForWord("+forWord+") from Phrase");
        this.forWord = forWord;
        updatePhrase();
    }

    public String getNatWord() {
        return natWord;
    }

    public void setNatWord(String natWord) {
        System.out.println("CALL setNatWord("+natWord+") from Phrase");
        this.natWord = natWord;
        updatePhrase();
    }

    public String getTranscr() {
        return transcr;
    }

    public void setTranscr(String transcr) {
        System.out.println("CALL setTranscr("+transcr+") from Phrase");
        this.transcr = transcr;
        updatePhrase();
    }

    public BigDecimal getProb() {
        return prob;
    }

    public void setProb(BigDecimal prob) {
        System.out.println("CALL setProb("+prob+") from Phrase");
        this.prob = prob;
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
