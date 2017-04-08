package datamodel;

import logic.DatabaseHelper;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;

/**
 * Created by Aleks on 11.05.2016.
 */

@Entity
@Table(name = "words")
public class Phrase implements Serializable {

    @Transient
    private static final double RIGHT_ANSWER_MULTIPLIER = 1.44;

    @Transient
    public static final double TRAINED_PROBABILITY_FACTOR = 3;

    @javax.persistence.Id
//    @GeneratedValue(strategy=GenerationType.AUTO)
    public long id;

    @Column(name = "for_word")
    public String foreignWord;

    @Column(name = "nat_word")
    public String nativeWord;

    @Column(name = "transcr")
    public String transcription;

    @Column(name = "prob_factor")
    public double probabilityFactor;

    @Column
    public String label;

    @Column(name = "create_date")
    public ZonedDateTime collectionAddingDateTime;

    @Column(name = "last_accs_date")
    public ZonedDateTime lastAccessDateTime;

    @Column(name = "rate")
    public double multiplier;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    public User owner;

    @Column(name = "is_deleted")
    public boolean isDeleted;

    @Transient
    public double previousProbabilityFactor;

    @Transient
    public ZonedDateTime phraseAppearingTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));

    @Transient
    public boolean hasBeenAnsweredCorrectly;

    @Transient
    public boolean hasBeenAnswered;

    @Transient
    public int indexStart;

    @Transient
    public int indexEnd;

    @Transient
    public double previousMultiplier;

    @Transient
    public DatabaseHelper databaseHelper;

    @Transient
    public boolean isModified;

    @Transient
    public String timeOfReturningFromList;

    public Phrase() {
    }

    public Phrase(long id, String foreignWord, String nativeWord, String transcription, double probabilityFactor,
                  ZonedDateTime collectionAddingDateTime, String label, ZonedDateTime lastAccessDateTime, double multiplier, DatabaseHelper databaseHelper, User owner){
        this.id = id;
        this.owner = owner;
        this.foreignWord = foreignWord;
        this.nativeWord = nativeWord;
        this.transcription = transcription == null ? "" : transcription;
        this.probabilityFactor = probabilityFactor;
        this.previousProbabilityFactor = probabilityFactor;
        this.collectionAddingDateTime = collectionAddingDateTime;
        this.label = (label == null ? "" : label);
        this.lastAccessDateTime = lastAccessDateTime;
        this.multiplier = multiplier <= 1 ? 1 : multiplier;
        this.previousMultiplier = multiplier <= 1 ? 1 : multiplier;
        this.databaseHelper = databaseHelper;
    }

    public Phrase(Phrase givenPhrase){
        this.id = givenPhrase.id;
        this.foreignWord = givenPhrase.foreignWord;
        this.nativeWord = givenPhrase.nativeWord;
        this.transcription = givenPhrase.transcription;
        this.probabilityFactor = givenPhrase.probabilityFactor;
        this.previousProbabilityFactor = givenPhrase.probabilityFactor;
        this.collectionAddingDateTime = givenPhrase.collectionAddingDateTime;
        this.label = givenPhrase.label;
        this.lastAccessDateTime = givenPhrase.lastAccessDateTime;
        this.indexStart = givenPhrase.indexStart;
        this.indexEnd = givenPhrase.indexEnd;
        this.multiplier = givenPhrase.multiplier;
        this.previousMultiplier = multiplier <= 1 ? 1 : multiplier;
        this.databaseHelper = givenPhrase.databaseHelper;
    }

    public boolean isInList(HashSet<String> phrasesList){

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
        }else{
            return true;
        }
    }

    public void resetPreviousValues(){
        this.previousMultiplier = multiplier;
        this.previousProbabilityFactor = probabilityFactor;
    }

    public void update(){
        /*if(databaseHelper != null)
            databaseHelper.updatePhrase(this);*/
    }

    public boolean isTrained(){
        return probabilityFactor <= TRAINED_PROBABILITY_FACTOR;
    }

    public boolean wasTrainedBeforeAnswer(){
        return previousProbabilityFactor <= 3;
    }

    @Override
    public String toString() {
        return "Phrase{" +
                "indexStart=" + indexStart +
                ", indexEnd=" + indexEnd +
                '}';
    }

    @Override
    public int hashCode() {
        return foreignWord.hashCode() * (nativeWord.hashCode() + 21);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Phrase phrase = (Phrase) o;

        return id == phrase.id;

    }

    //Setters and getters

    public long getId() {
        return id;
    }
    public String getForeignWord(){
        return foreignWord;
    }
    public void setForeignWord(String foreignWord) {
        System.out.println("CALL setForeignWord("+ foreignWord +") from Phrase");
        this.foreignWord = foreignWord;
        update();
    }
    public String getNativeWord() {
        return nativeWord;
    }
    public void setNativeWord(String nativeWord) {
        System.out.println("CALL setNativeWord("+ nativeWord +") from Phrase");
        this.nativeWord = nativeWord;
        update();
    }
    public String getTranscription() {
        return transcription;
    }
    public void setTranscription(String transcription) {
        System.out.println("CALL setTranscription("+ transcription +") from Phrase");
        this.transcription = transcription;
        update();
    }
    public double getProbabilityFactor() {
        return probabilityFactor;
    }
    public void setProbabilityFactor(double probabilityFactor) {
        this.probabilityFactor = probabilityFactor;
        update();
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        System.out.println("CALL setLabel("+label+") from Phrase");
        this.label = label;
        update();
    }
    public ZonedDateTime getCollectionAddingDateTime() {
        return collectionAddingDateTime;
    }
    public ZonedDateTime getLastAccessDateTime() {
        return lastAccessDateTime;
    }
    public void setLastAccessDateTime(ZonedDateTime lastAccessDateTime) {
        this.lastAccessDateTime = lastAccessDateTime;
    }
    public void setTimeOfReturningFromList(long time){
        timeOfReturningFromList = Double.toString((double) time / 1000000d);
    }
    public int getIndexStart() {
        return indexStart;
    }
    public void setIndexStart(int indexStart) {
        this.indexStart = indexStart;
    }
    public int getIndexEnd() {
        return indexEnd;
    }
    public void setIndexEnd(int indexEnd) {
        this.indexEnd = indexEnd;
    }
    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }
    public void setDatabaseHelper(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }


}
