package logic;

import java.sql.Timestamp;

/**
 * Created by Aleks on 11.05.2016.
 */
public class Phrase {
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

    public Phrase(int id, String forWord, String natWord, String transcr, double prob, Timestamp createDate,
                  String label, Timestamp lastAccs, double indexStart, double indexEnd, boolean exactMatch){
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
}
