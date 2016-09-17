package logic;

import javax.enterprise.context.Dependent;
import javax.faces.bean.ManagedBean;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;

/**
 * Created by Aleks on 11.05.2016.
 */
@ManagedBean
@Dependent
public class Phrase implements Serializable{

    public static final double INITIAL_RATE = 1.2;
    public int id;
    public String forWord;
    public String natWord;
    public String transcr;
    public BigDecimal prob;
    public String label;
    public Timestamp createDate;
    public Timestamp lastAccs;
    public boolean exactMatch;
    public ZonedDateTime ldt = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
    public Boolean howWasAnswered;
    public double indexStart;
    public double indexEnd;
    public double rate = 1;
    private DAO dao;
    public boolean isModified;
    public String answer;
    public String timeOfReturningFromList;

    /**
     * Saved state of phrase object before changing howWasAnswered to false or true
     */
    public Phrase unmodifiedPhrase;



    public Phrase(){}

    public Phrase(int id, String forWord, String natWord, String transcr, BigDecimal prob, Timestamp createDate,
                  String label, Timestamp lastAccs, double indexStart, double indexEnd, boolean exactMatch, double rate, DAO dao){
        this.dao = dao;
        this.id = id;
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr==null?"":transcr;
        this.prob = prob.setScale(1, RoundingMode.HALF_UP);
        this.createDate = createDate;
        this.label = label==null?"":label;
        this.lastAccs = lastAccs;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.rate = rate<=1 ? 1 : rate;
        this.unmodifiedPhrase = new Phrase(forWord, natWord, transcr, prob, createDate, label, lastAccs, indexStart, indexEnd, exactMatch);
    }

    public Phrase(String forWord, String natWord, String transcr, BigDecimal prob, Timestamp createDate,
                  String label, Timestamp lastAccs, double indexStart, double indexEnd, boolean exactMatch){
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr==null?"":transcr;
        this.prob = prob;
        this.createDate = createDate;
        this.label = label==null?"":label;
        this.lastAccs = lastAccs;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.unmodifiedPhrase = null;
    }

    public Phrase(String forWord, String natWord, String transcr, String label){
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr==null?"":transcr;
        this.prob = new BigDecimal(30);
        this.createDate = new Timestamp(System.currentTimeMillis());
        this.label = label==null?"":label;
        this.exactMatch = false;
    }


    public Phrase returnUnmodified(){
        return unmodifiedPhrase;
    }

    public void rightAnswer(String answer){
        long[] indexes = null;
        this.answer = answer;

        if(howWasAnswered == null){     //Ответ на фразу первый раз

            if(!isLearnt()){

                double rateDepandableOnNumberOfWords = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWords);
                System.out.println("rateDepandableOnNumberOfWords = " + rateDepandableOnNumberOfWords + "; rate=" + rate);
                BigDecimal subtr = new BigDecimal(3 * rateDepandableOnNumberOfWords * rate);

                if(rate <= 1){
                    rate = INITIAL_RATE;
                }else {
                    rate *= INITIAL_RATE;
                }

                prob = prob.subtract(subtr);
            }


        }else if(!howWasAnswered){      //если true значит на фразу уже был неправильный ответ

            if(!unmodifiedPhrase.isLearnt()){   //Если до ответа на фразу она не была изучена
                /*BigDecimal subtr = new BigDecimal(9 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWords) * unmodifiedPhrase.rate);
                prob = prob.subtract(subtr);*/
                prob = unmodifiedPhrase.prob;

                double rateDepandableOnNumberOfWords = Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWords);
                System.out.println("rateDepandableOnNumberOfWords = " + rateDepandableOnNumberOfWords + "; rate=" + rate);
                BigDecimal subtr = new BigDecimal(3 * rateDepandableOnNumberOfWords * rate);

                if(rate <= 1){
                    rate = INITIAL_RATE;
                }else {
                    rate *= INITIAL_RATE;
                }

                prob = prob.subtract(subtr);
            }else{      //Если была, просто возвращаем первоначальное значение prob
                prob = unmodifiedPhrase.prob;
            }

        }

        howWasAnswered = true;
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
        rate = 1;

        if(howWasAnswered == null){     //Ответ на фразу первый раз

            BigDecimal summ = new BigDecimal(6*Math.sqrt((dao.nonLearnedWords) / dao.totalPossibleWords));
            prob = prob.add(summ);
            howWasAnswered = false;
            indexes = dao.updateProb(this);
            dao.setStatistics(this);

        }else if(howWasAnswered){

            if(!unmodifiedPhrase.isLearnt()) {
                BigDecimal summ = new BigDecimal(9 * Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWords));
                prob = prob.add(summ);
            }else{
                BigDecimal summ = new BigDecimal(6*Math.sqrt(dao.nonLearnedWords / dao.totalPossibleWords));
                prob = prob.add(summ);
            }

            howWasAnswered = false;
            indexes = dao.updateProb(this);
            dao.updateStatistics(this);

        }

        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public boolean inLabels(HashSet<String> hashSet){
//        System.out.println("CALL: inLabels(HashSet<String> hashSet) hashSet = " + hashSet);
        if(hashSet!=null){
            if(hashSet.isEmpty())
                return true;
            for(String str : hashSet){
                if(this.label!=null&&this.label.equalsIgnoreCase(str))
                    return true;
            }
            return false;
        }else {
//            throw new RuntimeException("inLabels(HashSet<String> hashSet) hashSet == null");
            return true;
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
        return forWord + " - " + natWord + " last. accs:" + lastAccs;
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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public Timestamp getLastAccs() {
        return lastAccs;
    }

    public void setLastAccs(Timestamp lastAccs) {
        this.lastAccs = lastAccs;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public void setTimeOfReturningFromList(long time){
        timeOfReturningFromList = Double.toString((double) time / 1000000d);
    }

    public String getTimeOfReturningFromList(){
        return timeOfReturningFromList;
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
