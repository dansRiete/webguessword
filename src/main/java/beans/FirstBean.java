package beans;

import logic.DAO;
import logic.Phrase;

import java.io.Serializable;
import java.sql.*;
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

    public FirstBean() throws SQLException{
        System.out.println("--- Bean was created");
        nextQuestion();
    }

    public boolean checkAnswer(){
        boolean bool = logic.IntelliFind.match(currPhrase.forWord, answer, false);
        System.out.println(currPhrase.forWord + " - " + answer + " - " + bool);
        result = currPhrase.forWord + " - " + answer + "</br>" + result;
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




