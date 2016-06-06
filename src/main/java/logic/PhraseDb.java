package logic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

/**
 * Created by Aleks on 31.05.2016.
 */
public class PhraseDb implements Serializable {
    private int id;
    private String forWord;
    private String natWord;
    private String transcr;
    private BigDecimal probFactor;
    private String label;
    private Timestamp createDate;
    private Timestamp lastAccsDate;
    private boolean exactmatch;
    private boolean editable = false;

    public PhraseDb(int id, String forWord, String natWord, String transcr, double probFactor,
             String label, Timestamp createDate, Timestamp lastAccsDate, boolean exactmatch){
        this.id = id;
        this.forWord = forWord;
        this.natWord = natWord;
        this.transcr = transcr;
        this.probFactor = new BigDecimal(probFactor).setScale(2, RoundingMode.HALF_UP);
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
        System.out.println("CALL: setForWord(String forWord) from PhraseDb");
        this.forWord = forWord;
    }

    public String getNatWord() {
//        System.out.println("CALL: getNatWord() from PhraseDb");
        return natWord;
    }

    public void setNatWord(String natWord) {
        System.out.println("CALL: setNatWord(String natWord) from PhraseDb");
        this.natWord = natWord;
    }

    public String getTranscr() {
        return transcr;
    }

    public void setTranscr(String transcr) {
        this.transcr = transcr;
    }

    public BigDecimal getProbFactor() {
        return probFactor;
    }

    public void setProbFactor(BigDecimal probFactor) {
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

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
