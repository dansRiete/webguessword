package beans;

import logic.DAO;
import logic.Phrase;

import java.io.Serializable;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private final String wrongMessage = " <strong><font color=\"#ff0000\">неправильно</font></strong> ";
    private final String rightMessage = " <strong><font color=\"green\">правильно</font></strong> ";

    public FirstBean() throws SQLException{
        System.out.println("--- Bean was created");
        nextQuestion();
    }

    public boolean checkAnswer(){
        boolean bool = logic.IntelliFind.match(currPhrase.forWord, answer, false);
        LocalTime lt = LocalTime.now();
        System.out.println(currPhrase.forWord + " - " + answer + " - " + bool);
        if(bool)
            result = "<strong>" + lt.format(DateTimeFormatter.ofPattern("HH:mm")) + "</strong>" + rightMessage + currPhrase.natWord + " - " + currPhrase.forWord + "</br>" + result;
        else
            result = "<strong>" + lt.format(DateTimeFormatter.ofPattern("HH:mm")) + "</strong>" + wrongMessage + currPhrase.natWord + " - " + currPhrase.forWord + "</br>" + result;
        answer = "";
        nextQuestion();
        return bool;
    }

    public void nextQuestion(){
        currPhrase = dao.nextPhrase();
        question = currPhrase.natWord;
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




