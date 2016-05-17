package beans;

import logic.DAO;
import logic.Phrase;

import java.io.Serializable;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class InterfaceBean implements Serializable{


    private DAO dao = new DAO();
    private Phrase currPhrase;
    private String question ="";
    private String answer = "";
    private String result = "";
    private final String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong>";
    private final String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private final String NONANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private ArrayList<Phrase> listOfPhrases = new ArrayList<>();
    private int shift = 0;
    private int index;

    public InterfaceBean() throws SQLException{
        System.out.println("--- Bean was created");
        nextQuestion();
    }

    private void resultProcessing(){
        StringBuilder str = new StringBuilder();
//        int currPos = listOfPhrases.size() - 1 - shift; //is never used
        for(int i = listOfPhrases.size()-1; i>=0; i--){
            if(listOfPhrases.get(i).isAnswered==null)
                str.append(i==index?"<strong>":"").append("[").append(listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append(NONANSWERED_MESSAGE).append("] ").append(listOfPhrases.get(i).natWord).append((i==index?"</strong>":"")).append("</br>");
            else if(listOfPhrases.get(i).isAnswered)
                str.append(i==index?"<strong>":"").append("[").append(listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append(RIGHT_MESSAGE).append("] ").append(listOfPhrases.get(i).natWord).append(" - ").append(listOfPhrases.get(i).forWord).append((i==index?"</strong>":"")).append("</br>");
            else if(!listOfPhrases.get(i).isAnswered)
                str.append(i==index?"<strong>":"").append("[").append(listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append(WRONG_MESSAGE).append("] ").append(listOfPhrases.get(i).natWord).append(" - ").append(listOfPhrases.get(i).forWord).append((i==index?"</strong>":"")).append("</br>");
        }
        result = str.toString();
    }

    private void newPhrase(){
        listOfPhrases.add(dao.nextPhrase());
    }

    public void rightAnswer(){
        try {
            listOfPhrases.get(listOfPhrases.size() - 1 - shift).isAnswered = true;
            if (shift == 0)
                nextQuestion();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println(listOfPhrases + " size=" + (listOfPhrases==null?"listOfPhrases=null":listOfPhrases.size()));
            e.printStackTrace();
        }
    }

    public void wrongAnswer(){
        try{
            listOfPhrases.get(listOfPhrases.size() - 1 - shift).isAnswered = false;
            if(shift==0)
                nextQuestion();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println(listOfPhrases + " size=" + (listOfPhrases==null?"listOfPhrases=null":listOfPhrases.size()));
            e.printStackTrace();
        }
    }

    public void previousRight(){
        try{
            if(shift==0){
                listOfPhrases.get(listOfPhrases.size() - 2).isAnswered = true;
                resultProcessing();
            }
        }catch (NullPointerException e){
            System.out.println(listOfPhrases + " size=" + (listOfPhrases==null?"listOfPhrases=null":listOfPhrases.size()));
            e.printStackTrace();
        }
    }

    public void previousWrong(){
        try{
            if(shift==0){
                listOfPhrases.get(listOfPhrases.size() - 2).isAnswered = false;
                resultProcessing();
            }
        }catch (NullPointerException e){
            System.out.println(listOfPhrases + " size=" + (listOfPhrases==null?"listOfPhrases=null":listOfPhrases.size()));
            e.printStackTrace();
        }
    }

    public void checkTheAnswer(){
        if(answer!=null){
            TableBean.orderList[0].qty = TableBean.orderList[0].qty + 1;
            if(!(answer.equals("")||answer.equals("+")||answer.equals("-")||answer.equals("++")||answer.equals("--"))){
                boolean bool = logic.IntelliFind.match(listOfPhrases.get(listOfPhrases.size() - 1 - shift).forWord, answer, false);
                if(bool)
                    rightAnswer();
                else
                    wrongAnswer();
            }else if (answer.equals("+")){
                rightAnswer();
            }else if (answer.equals("-")){
                wrongAnswer();
            }else if (answer.equals("++")){
                previousRight();
            }else if (answer.equals("--")){
                previousWrong();
            }
            answer="";
        }
    }

    public void nextQuestion(){
        if(shift==0) {
            newPhrase();
            index = listOfPhrases.size() - 1;
            question = listOfPhrases.get(index).natWord;
        }else {
            index = listOfPhrases.size() - 1 - --shift;
            question = listOfPhrases.get(index).natWord;
        }
        resultProcessing();
//        System.out.println("--- nextQuestion() List size="+(listOfPhrases.size()+" Current shift="+shift+" Requested index="+index));
    }

    public void previousQuestion(){
        if(shift<(listOfPhrases.size()-1))
            shift++;
        index = listOfPhrases.size() - 1 - shift;
        if(index<0)
            index = 0;
        question = listOfPhrases.get(index).natWord;
        resultProcessing();
//        System.out.println("--- previousQuestion() List size="+(listOfPhrases.size()+" Current shift="+shift+" Requested index="+index));
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




