package beans;

import logic.DAO;
import logic.Phrase;

import java.io.Serializable;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class FirstBean implements Serializable{

    private DAO dao = new DAO();
    private Phrase currPhrase;
    private String question ="";
    private String answer = "";
    private String result = "";
    private final String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong> ";
    private final String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong> ";
    private final String NONANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong> ";
    private ArrayList<Phrase> listOfPhrases = new ArrayList<>();
    private int shift = 0;
    private int index;

    public FirstBean() throws SQLException{
        System.out.println("--- Bean was created");
        nextQuestion();
    }

    private void resultProcessing(){
        StringBuilder str = new StringBuilder();
        int currPos = listOfPhrases.size() - 1 - shift;
        for(int i = listOfPhrases.size()-1; i>=0; i--){
            if(listOfPhrases.get(i).isAnswered==null)
                str.append((i==index?"<strong>":"") + "[<font size=\"-1\">" + listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "</font> " + NONANSWERED_MESSAGE + "] " +listOfPhrases.get(i).natWord + (i==index?"</strong>":"") + "</br>");
            else if(listOfPhrases.get(i).isAnswered)
                str.append((i==index?"<strong>":"") + "[<font size=\"-1\">" + listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "</font> " + RIGHT_MESSAGE + "] " + listOfPhrases.get(i).natWord + " - " + listOfPhrases.get(i).forWord + (i==index?"</strong>":"") + "</br>");
            else if(!listOfPhrases.get(i).isAnswered)
                str.append((i==index?"<strong>":"") + "[<font size=\"-1\">" + listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "</font> " + WRONG_MESSAGE + "] " + listOfPhrases.get(i).natWord + " - " + listOfPhrases.get(i).forWord + (i==index?"</strong>":"") + "</br>");
        }
        result = str.toString();
    }

    private void newPhraze(){
        listOfPhrases.add(dao.nextPhrase());

    }

    public void rightAnswer(){
        listOfPhrases.get(listOfPhrases.size() - 1 - shift).isAnswered = true;
        resultProcessing();
    }

    public void wrongAnswer(){
        listOfPhrases.get(listOfPhrases.size() - 1 - shift).isAnswered = false;
        resultProcessing();
    }

    public boolean checkTheAnswer(){
        boolean bool = logic.IntelliFind.match(currPhrase.forWord, answer, false);
        LocalTime lt = LocalTime.now();
        System.out.println(currPhrase.forWord + " - " + answer + " - " + bool);
        /*if(bool)
            result = "<strong>" + lt.format(DateTimeFormatter.ofPattern("HH:mm")) + "</strong>" + RIGHT_MESSAGE + currPhrase.natWord + " - " + currPhrase.forWord + "</br>" + result;
        else
            result = "<strong>" + lt.format(DateTimeFormatter.ofPattern("HH:mm")) + "</strong>" + WRONG_MESSAGE + currPhrase.natWord + " - " + currPhrase.forWord + "</br>" + result;*/
        answer = "";
        nextQuestion();
        return bool;
    }

    public void nextQuestion(){


        if(shift==0) {
            newPhraze();
            index = listOfPhrases.size() - 1;
            question = listOfPhrases.get(index).natWord;
        }else {
            index = listOfPhrases.size() - 1 - --shift;
            question = listOfPhrases.get(index).natWord;
        }
        System.out.println("--- nextQuestion() List size="+(listOfPhrases.size()+" Current shift="+shift+" Requested index="+index));
        resultProcessing();
    }

    public void previousQuestion(){
        if(shift<(listOfPhrases.size()-1))
            shift++;
        index = listOfPhrases.size() - 1 - shift;
        if(index<0)
            index = 0;
        question = listOfPhrases.get(index).natWord;
        resultProcessing();
        System.out.println("--- previousQuestion() List size="+(listOfPhrases.size()+" Current shift="+shift+" Requested index="+index));
    }

    public void exit(){
        dao.backupDB();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String res) {
        this.result = res;
    }
}




