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
    public long uniqueId = System.currentTimeMillis();
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
    /**
     * Saved state of phrase object before changing isAnswered to false or true
     */
    private Phrase currentPhrase;

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
    }

    public void rightAnswer(){
        if(isAnswered==null){
            if(!isLearnt()){
                prob-=3;
            }
            isAnswered = true;
            dao.updateProb(this);
        }else if(!isAnswered){
            if(!isLearnt())
                prob-=9;
            isAnswered = true;
            dao.updateProb(this);
        }

    }

    public void wrongAnswer(){
        if(isAnswered==null){
            prob+=6;
            isAnswered = false;
            dao.updateProb(this);
        }else if(isAnswered){
            prob+=9;
            isAnswered = false;
            dao.updateProb(this);
        }

    }



    boolean isLearnt(){
        return prob<=3;
    }

    public String getForWord(){
        return forWord;
    }

    public String toString(){
        return forWord + " - " + natWord;
    }
}
