package logic;

import java.sql.Timestamp;

/**
 * Created by Aleks on 31.05.2016.
 */
public class PhraseDb {
    private int id;
    private String forWord;
    private String natWord;
    private String transcr;
    private double probFactor;
    private String label;
    private Timestamp createDate;
    private Timestamp lastAccsDate;
    private boolean exactmatch;

    PhraseDb(int id, String forWord, String natWord, String transcr, double probFactor,
             String label, Timestamp createDate, Timestamp lastAccsDate, boolean exactmatch){
        this.id = id;
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr;
        this.probFactor = probFactor;
        this.label = label;
        this.createDate = createDate;
        this.lastAccsDate = lastAccsDate;
        this.exactmatch = exactmatch;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getForWord() {
        return forWord;
    }

    public void setForWord(String forWord) {
        this.forWord = forWord;
    }

    public String getNatWord() {
        return natWord;
    }

    public void setNatWord(String natWord) {
        this.natWord = natWord;
    }

    public String getTranscr() {
        return transcr;
    }

    public void setTranscr(String transcr) {
        this.transcr = transcr;
    }

    public double getProbFactor() {
        return probFactor;
    }

    public void setProbFactor(double probFactor) {
        this.probFactor = probFactor;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastAccsDate() {
        return lastAccsDate;
    }

    public void setLastAccsDate(Timestamp lastAccsDate) {
        this.lastAccsDate = lastAccsDate;
    }

    public boolean isExactmatch() {
        return exactmatch;
    }

    public void setExactmatch(boolean exactmatch) {
        this.exactmatch = exactmatch;
    }
}
