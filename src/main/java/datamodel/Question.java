package datamodel;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 11.11.2016.
 */

@Entity
@Table(name = "questions")
public class Question {

    @javax.persistence.Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Column(name = "answer")
    private String answer;

    @Column(name = "date")
    private final ZonedDateTime askDate = ZonedDateTime.now();

    @Column(name = "phrase_key")
    private final Phrase askedPhrase;

    @Column(name = "answered_correctly")
    private boolean answerCorrect;

    public boolean isSelected() {
        return selected;
    }

    @Transient
    private boolean selected;

    private Question(Phrase askedPhrase) {
        this.askedPhrase = askedPhrase;
    }



    public static Question compose(Phrase askedPhrase){
        if(askedPhrase == null){
            throw new IllegalArgumentException("Phrases foreign and native literals can not be null");
        }
        return new Question(askedPhrase);
    }

    public Question answerTheQuestion(String answer){
        this.answer = answer;
        if(this.answer.equals("") || askedPhrase.getForeignWord().equals("")){
            answerCorrect = false;
        }else if(!this.answer.contains("\\") && !this.answer.contains("/")){
            answerCorrect = phrasesEquals(this.answer, askedPhrase.getForeignWord());
        }else {
            String [] givenLiteralPhrases = this.answer.split("[/\\\\]");
            String [] referenceLiteralPhrases = askedPhrase.getForeignWord().split("[/\\\\]");
            int matchesAmount = 0;
            for (String referenceLiteralPhrase : referenceLiteralPhrases) {
                for (String givenLiteralPhrase : givenLiteralPhrases) {
                    if (phrasesEquals(referenceLiteralPhrase, givenLiteralPhrase)) {
                        matchesAmount++;
                    }
                }
            }
            answerCorrect = matchesAmount == referenceLiteralPhrases.length;
        }
        return this;
    }

    public Question rightAnswer(){
        this.answer = askedPhrase.getForeignWord();
        this.answerCorrect = true;
        return this;
    }

    public Question wrongAnswer(){
        this.answer = "Had not been given";
        this.answerCorrect = false;
        return this;
    }

    private boolean phrasesEquals(String givenPhrase, String referencePhrase){
        List<String> givenPhraseWords = splitToWords(givenPhrase);
        List<String> referencePhraseWords = splitToWords(referencePhrase);

        for (int i = 0; i < referencePhraseWords.size(); i++){
            if(!wordsEquals(referencePhraseWords.get(i), givenPhraseWords.get(i))){
                return false;
            }
        }
        return true;
    }

    private boolean wordsEquals(String givenWord, String referenceWord){
        if(givenWord.length() <= 6){
            return givenWord.replaceAll("\\W|_", "").equalsIgnoreCase(referenceWord.replaceAll("\\W|_", ""));
        }else {
            return removeDoubleLetters(givenWord.replaceAll("\\W|_", "")).equalsIgnoreCase(removeDoubleLetters(referenceWord.replaceAll("\\W|_", "")));
        }
    }

    private List<String> splitToWords(String givenPhrase){
        ArrayList<String> words = new ArrayList<>();
        for(String currentWord : givenPhrase.split("[ -]")){
            if(isWord(currentWord)){
                words.add(currentWord);
            }
        }
        return words;
    }

    private String removeDoubleLetters(String givenWord){
        StringBuilder shortAnswer = new StringBuilder().append(givenWord.charAt(0));
        for (int i = 1, y = 0; i < givenWord.length(); i++) {
            if (givenWord.toLowerCase().charAt(i) != shortAnswer.charAt(y)) {
                shortAnswer.append(givenWord.toLowerCase().charAt(i));
                y++;
            }
        }
        return shortAnswer.toString();
    }

    private boolean isWord(String givenWord){
        for(char currentChar : givenWord.toCharArray()){
            if(Character.isLetter(currentChar)){
                return true;
            }
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public String getAnswer() {
        return answer;
    }

    public ZonedDateTime getAskDate() {
        return askDate;
    }

    public Phrase getAskedPhrase() {
        return askedPhrase;
    }

    public boolean answerIsCorrect() {
        return answered() && answerCorrect;
    }

    public boolean answered(){
        return answer != null;
    }
}
