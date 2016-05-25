package logic;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Created by Aleks on 11.05.2016.
 */
public class Phrase {

    public ZonedDateTime lt = ZonedDateTime.now(ZoneId.of("EET"));
    public Boolean isAnswered;
    public int id;
    public String forWord;
    public String natWord;
    public String transcr;
    public double prob;
    public Timestamp createDate;
    public String label;
    public Timestamp lastAccs;
    public double indexStart;
    public double indexEnd;
    public boolean exactMatch;
    private DAO dao;
    public boolean isModified;

    /**
     * Saved state of phrase object before changing isAnswered to false or true
     */
    private Phrase unmofifiedPhrase;

    public Phrase(int id, String forWord, String natWord, String transcr, double prob, Timestamp createDate,
                  String label, Timestamp lastAccs, double indexStart, double indexEnd, boolean exactMatch, DAO dao){
        this.dao = dao;
        this.id = id;
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
        this.unmofifiedPhrase = new Phrase(forWord, natWord, transcr, prob, createDate, label, lastAccs, indexStart, indexEnd, exactMatch);
    }

    public Phrase(String forWord, String natWord, String transcr, double prob, Timestamp createDate,
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

    public Phrase returnUnmodified(){
        return unmofifiedPhrase;
    }

    public void rightAnswer(){
        long[] indexes = null;
        if(isAnswered==null){
            if(!isLearnt()){
                prob-=3;
            }
            isAnswered = true;
            indexes = dao.updateProb(this);
        }else if(!isAnswered){
            if(!isLearnt())
                prob-=9;
            isAnswered = true;
            indexes =  dao.updateProb(this);
        }
        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public void wrongAnswer(){
        long[] indexes = null;
        if(isAnswered==null){
            prob+=6;
            isAnswered = false;
            indexes = dao.updateProb(this);
        }else if(isAnswered){
            prob+=9;
            isAnswered = false;
            indexes = dao.updateProb(this);
        }
        if(indexes!=null){
            this.indexStart = indexes[0];
            this.indexEnd = indexes[1];
        }
    }

    public void updatePhrase(){
        dao.updatePhrase(this);
    }

    boolean isLearnt(){
        return prob<=3;
    }

    public String getForWord(){
        return forWord;
    }

    public String toString(){
        return forWord + " - " + natWord + " last. accs:" + lastAccs;
    }
}
