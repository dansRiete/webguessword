package beans;

import logic.DAO;
import logic.Phrase;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class InterfaceBean implements Serializable{

    @ManagedProperty(value="#{stat}")
    private StatBean statBean;
    public void setStatBean(StatBean statBean) {
        this.statBean = statBean;
    }

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    //>>Session statistics
    private int numOfPhrForSession = 0;
    private int numOfAnswForSession = 0;
    private int numOfRightAnswForSession = 0;
    private int numOfWrongAnswForSession = 0;
    private int totalNumberOfWords;
    private int totalNumberOfLearnedWords;
    private String percentOfRightAnswers;
    private int[] timeOfAccsToDbArr = new int[5];
    private int timeOfAccsToDbArrCounter;
    private BigDecimal avgTimeOfAccsToDb;
    //<<

    //>>Phrase data
    private String pDpercentOfAppearance;
    private double pDprob;
    private Timestamp pdLastAccs;
    private Timestamp pdCreateDate;
    private String label;
    //<<

    private void reloadPhraseData(){
        pDprob = listOfPhrases.get(index).prob;
        pdLastAccs = listOfPhrases.get(index).lastAccs;
        pdCreateDate = listOfPhrases.get(index).createDate;
        label = listOfPhrases.get(index).label;
    }


    private DAO dao;
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
        init();
    }

    @PostConstruct
    private void init(){
        System.out.println("--- loginbean is " + loginBean);
        if(loginBean!=null)
            dao = loginBean.returnDAO();
        if(dao!=null)
            nextQuestion();
    }

    public ArrayList<Phrase> returnListOfPhrases(){
        return listOfPhrases;
    }

    private void calculateSessionStatistics(){
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        numOfPhrForSession = listOfPhrases.size();
        for(Phrase phrs : listOfPhrases){
            if(phrs.isAnswered==null)
                numOfNonAnswForSession++;
            else if(phrs.isAnswered)
                numOfRightAnswForSession++;
        }
        numOfAnswForSession = numOfPhrForSession-numOfNonAnswForSession;
        this.numOfRightAnswForSession = numOfRightAnswForSession;
        this.numOfWrongAnswForSession = numOfAnswForSession-numOfRightAnswForSession;
        //Формирует строку с процентным соотношением правильных ответов к общему кол-ву ответов
        percentOfRightAnswers = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(numOfAnswForSession==0?1:numOfAnswForSession),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        //>>Рассчитываем среднее время доступа к базе данных
        double summ = 0;
        double counter = 0;
        for(int a : timeOfAccsToDbArr)
            if (a!=0){
                summ+=a;
                counter++;
            }
        avgTimeOfAccsToDb = new BigDecimal(summ/(counter==0?1:counter)).setScale(0, RoundingMode.HALF_UP);

    }


    private void resultProcessing(){
        calculateSessionStatistics();
        reloadPhraseData();
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
        long starTime = System.currentTimeMillis();
        Phrase phrase = dao.nextPhrase();
        if(phrase!=null){
            listOfPhrases.add(phrase);
        }
        if(timeOfAccsToDbArrCounter==5)
            timeOfAccsToDbArrCounter=0;
        timeOfAccsToDbArr[timeOfAccsToDbArrCounter++] = (int)(System.currentTimeMillis()-starTime);
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

    //>>Setters an getters
    //*************************************************************************************
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

    public int getNumOfPhrForSession() {
        return numOfPhrForSession;
    }
    public void setNumOfPhrForSession(int numOfPhrForSession) {
        this.numOfPhrForSession = numOfPhrForSession;
    }

    public int getTotalNumberOfWords() {
        return totalNumberOfWords;
    }
    public void setTotalNumberOfWords(int totalNumberOfWords) {
        this.totalNumberOfWords = totalNumberOfWords;
    }

    public int getTotalNumberOfLearnedWords() {
        return totalNumberOfLearnedWords;
    }
    public void setTotalNumberOfLearnedWords(int totalNumberOfLearnedWords) {
        this.totalNumberOfLearnedWords = totalNumberOfLearnedWords;
    }

    public String getPercentOfRightAnswers() {
        return percentOfRightAnswers;
    }
    public void setPercentOfRightAnswers(String percentOfRightAnswers) {
        this.percentOfRightAnswers = percentOfRightAnswers;
    }

    public int getNumOfAnswForSession() {
        return numOfAnswForSession;
    }
    public void setNumOfAnswForSession(int numOfAnswForSession) {
        this.numOfAnswForSession = numOfAnswForSession;
    }

    public int getNumOfRightAnswForSession() {
        return numOfRightAnswForSession;
    }
    public void setNumOfRightAnswForSession(int numOfRightAnswForSession) {
        this.numOfRightAnswForSession = numOfRightAnswForSession;
    }

    public int getNumOfWrongAnswForSession() {
        return numOfWrongAnswForSession;
    }
    public void setNumOfWrongAnswForSession(int numOfWrongAnswForSession) {
        this.numOfWrongAnswForSession = numOfWrongAnswForSession;
    }

    public String getpDpercentOfAppearance() {
        return pDpercentOfAppearance;
    }

    public void setpDpercentOfAppearance(String pDpercentOfAppearance) {
        this.pDpercentOfAppearance = pDpercentOfAppearance;
    }

    public double getpDprob() {
        return pDprob;
    }

    public void setpDprob(float pDprob) {
        this.pDprob = pDprob;
    }

    public Timestamp getPdLastAccs() {
        return pdLastAccs;
    }

    public void setPdLastAccs(Timestamp pdLastAccs) {
        this.pdLastAccs = pdLastAccs;
    }

    public Timestamp getPdCreateDate() {
        return pdCreateDate;
    }

    public void setPdCreateDate(Timestamp pdCreateDate) {
        this.pdCreateDate = pdCreateDate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getAvgTimeOfAccsToDb() {
        return avgTimeOfAccsToDb;
    }

    public void setAvgTimeOfAccsToDb(BigDecimal avgTimeOfAccsToDb) {
        this.avgTimeOfAccsToDb = avgTimeOfAccsToDb;
    }
}




