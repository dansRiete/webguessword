package logic;

import javax.enterprise.context.Dependent;
import javax.faces.bean.ManagedBean;
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
@ManagedBean
@Dependent
public class Phrase implements Serializable{

    public int id;
    public String forWord;
    public String natWord;
    public String transcr;
    public BigDecimal prob;
    public String label;
    public Timestamp createDate;
    public Timestamp lastAccs;
    public boolean exactMatch;
    public double index_start;
    public double index_end;



    public ZonedDateTime lt = ZonedDateTime.now(ZoneId.of("Europe/Kiev"));
    public Boolean howWasAnswered;
    public double indexStart;
    public double indexEnd;
    private DAO dao;
    public boolean isModified;
    public String answer;
    /**
     * Saved state of phrase object before changing howWasAnswered to false or true
     */
    private Phrase unmofifiedPhrase;



    public Phrase(){}

    public Phrase(int id, String forWord, String natWord, String transcr, BigDecimal prob, Timestamp createDate,
                  String label, Timestamp lastAccs, double indexStart, double indexEnd, boolean exactMatch, DAO dao){
        this.dao = dao;
        this.id = id;
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr;
        this.prob = prob.setScale(1, RoundingMode.HALF_UP);
        this.createDate = createDate;
        this.label = label;
        this.lastAccs = lastAccs;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.unmofifiedPhrase = new Phrase(forWord, natWord, transcr, prob, createDate, label, lastAccs, indexStart, indexEnd, exactMatch);
    }



    public Phrase(String forWord, String natWord, String transcr, BigDecimal prob, Timestamp createDate,
                  String label, Timestamp lastAccs, double indexStart, double indexEnd, boolean exactMatch){
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr;
        this.prob = prob;
        this.createDate = createDate;
        this.label = label;
        this.lastAccs = lastAccs;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.exactMatch = exactMatch;
        this.unmofifiedPhrase = null;
    }
    public Phrase(String forWord, String natWord, String transcr, String label){
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr.equals("")||transcr==null?null:transcr;
        this.prob = new BigDecimal(30);
        this.createDate = new Timestamp(System.currentTimeMillis());
        this.label = label.equals("")||label==null?null:label;
        this.exactMatch = false;
    }


    public Phrase returnUnmodified(){
        return unmofifiedPhrase;
    }

    public void rightAnswer(String answer){
        long[] indexes = null;
        this.answer = answer;
        if(howWasAnswered == null){
            if(!isLearnt()){
                BigDecimal subtr = new BigDecimal(3*Math.sqrt((dao.nonLearnedWords) / dao.totalWords));
                prob = prob.subtract(subtr);
//                dao.setProbById(id, prob.doubleValue());
            }
            howWasAnswered = true;
            indexes = dao.updateProb(this);
        }else if(!howWasAnswered){
            if(!unmofifiedPhrase.isLearnt()){
                BigDecimal subtr = new BigDecimal(9*Math.sqrt((dao.nonLearnedWords) / dao.totalWords));
                prob = prob.subtract(subtr);
//                dao.setProbById(id, prob.doubleValue());
            } else{
                prob = unmofifiedPhrase.prob;
//                dao.setProbById(id, prob.doubleValue());
//                prob=3*Math.sqrt((dao.nonLearnedWords + dao.learnedWords) / 1500d);
            }


            howWasAnswered = true;
            indexes =  dao.updateProb(this);
        }
        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public void wrongAnswer(String answer){
        long[] indexes = null;
        this.answer = answer;
        if(howWasAnswered == null){
            BigDecimal summ = new BigDecimal(6*Math.sqrt((dao.nonLearnedWords) / dao.totalWords));
            prob = prob.add(summ);
//            dao.setProbById(id, prob.doubleValue());
            howWasAnswered = false;
            indexes = dao.updateProb(this);
        }else if(howWasAnswered){
            if(!unmofifiedPhrase.isLearnt()) {
                BigDecimal summ = new BigDecimal(9 * Math.sqrt((dao.nonLearnedWords) / dao.totalWords));
                prob = prob.add(summ);
//                dao.setProbById(id, prob.doubleValue());
            }else{
                BigDecimal summ = new BigDecimal(6*Math.sqrt((dao.nonLearnedWords) / dao.totalWords));
                prob = prob.add(summ);
//                dao.setProbById(id, prob.doubleValue());
            }
            howWasAnswered = false;
            indexes = dao.updateProb(this);
        }
        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
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

    public boolean inLabels(HashSet<String> hashSet){
        if(hashSet!=null){
            if(hashSet.isEmpty())
                return true;
            for(String str : hashSet){
                if(this.label.equalsIgnoreCase(str))
                    return true;
            }
            return false;
        }else {
            System.out.println("Exception in inLabels from Phrase recieved hashset collection == null");
//            throw new RuntimeException();
        }

    }

    public int getIndex_start() {
        return (int) index_start;
    }

    public void setIndex_start(long index_start) {
        this.index_start = index_start;
    }

    public int getIndex_end() {
        return (int) index_end;
    }

    public void setIndex_end(long index_end) {
        this.index_end = index_end;
    }


}
