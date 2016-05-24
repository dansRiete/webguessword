package beans;

import logic.DAO;
import logic.Phrase;
import logic.RetDiff;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
    private String pdLastAccs;
    private String pdCreateDate;
    private String label;
    private String strLastAccs;
    private String strCreateDate;
    //<<

    //>>Current phrase data
    private String currPhrNatWord;
    private String currPhrForWord;
    private String currPhrTransc;
    private String currPhrLabel;
    //<<

    private RetDiff retDiff = new RetDiff();
    private DAO dao;
    private Phrase currPhrase;
    private String question ="";
    private String answer = "";
    private String result = "";
    private final String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong>";
    private final String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private final String NONANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private ArrayList<Phrase> listOfPhrases = new ArrayList<>();
    private ArrayList<String> listOfChooses;
    private String choosedLabel;
    private String resultChoosedLabel;
    private HashSet<String> hshset = new HashSet<>();
    private int shift = 0;
    private int index;


    public InterfaceBean() throws SQLException{
        System.out.println("CALL: InterfaceBean constructor");
        init();
    }

    @PostConstruct
    private void init(){
        System.out.println("CALL: init() from InterfaceBean");
        if(loginBean!=null)
            dao = loginBean.returnDAO();
        if(dao!=null){
            listOfChooses = dao.labels;
            nextQuestion();
        }
    }

    private void reloadPhraseData(){
        System.out.println("CALL: reloadPhraseData() from InterfaceBean");
        pDprob = new BigDecimal(listOfPhrases.get(index).prob).setScale(1, RoundingMode.HALF_UP).doubleValue();
        pdLastAccs = LocalDateTime.ofInstant(currPhrase.lastAccs.toInstant(),
                ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH)).toString();
        pdCreateDate = LocalDateTime.ofInstant(currPhrase.createDate.toInstant(),
                ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH)).toString();
        label = listOfPhrases.get(index).label;
//        System.out.println("--- Curr. phrase is " + currPhrase);
        strLastAccs = retDiff.retDiffInTime(System.currentTimeMillis() - currPhrase.lastAccs.getTime());
        strCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - currPhrase.createDate.getTime());
    }

    public void setTable() {
        System.out.println("CALL: setTable() from InterfaceBean");

        if (choosedLabel != null &&  (!choosedLabel.equalsIgnoreCase(""))){

            if(!choosedLabel.equalsIgnoreCase("all")){
                hshset.add(choosedLabel);
            }else{
                hshset.clear();
            }
        }

        resultChoosedLabel = "";
        boolean temp = true;

        for(String str : hshset){
            if(temp){
                resultChoosedLabel += "'" + str + "'";
                temp = false;
            }else {
                resultChoosedLabel += ",'" + str + "'";
            }
        }

        if(!resultChoosedLabel.equalsIgnoreCase(""))
            dao.table = "(SELECT * FROM " + loginBean.getUser() + " WHERE LABEL IN(" + resultChoosedLabel + ")) As custom";
        else{
            resultChoosedLabel = dao.table = loginBean.getUser();
        }
    }

    private void calculateSessionStatistics(){
        System.out.println("CALL: calculateSessionStatistics() from InterfaceBean");
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
    }

    public void resultProcessing(){
        System.out.println("CALL: resultProcessing() from InterfaceBean");
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
        currPhrForWord = listOfPhrases.get(index).forWord;
        currPhrNatWord = listOfPhrases.get(index).natWord;
        currPhrTransc = listOfPhrases.get(index).transcr;
        currPhrLabel = listOfPhrases.get(index).label;
        if(currPhrase.isModified)
            updatePhrase();
    }

    private void newPhrase(){
        System.out.println("CALL: newPhrase() from InterfaceBean");
        long starTime = System.nanoTime();
        dao.reloadIndices();
        Phrase phrase = dao.createRandPhrase();
        if(phrase!=null){
            listOfPhrases.add(phrase);
            currPhrase = phrase;
        }
        avgTimeOfAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000)).setScale(2, RoundingMode.HALF_UP);
    }

    public void rightAnswer(){
        System.out.println("CALL: rightAnswer() from InterfaceBean");
        try {
            index = listOfPhrases.size() - 1 - shift;
            currPhrase = listOfPhrases.get(index);
//            currPhrase.isAnswered = true;
            currPhrase.rightAnswer();
            if (shift == 0)
                nextQuestion();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in rightAnswer() from InterfaceBean");
            e.printStackTrace();
        }
    }

    public void wrongAnswer(){
        System.out.println("CALL: wrongAnswer() from InterfaceBean");
        try{
            index = listOfPhrases.size() - 1 - shift;
            currPhrase = listOfPhrases.get(index);
//            currPhrase.isAnswered = false;
            currPhrase.wrongAnswer();
            if(shift==0)
                nextQuestion();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in wrongAnswer() from InterfaceBean");
            e.printStackTrace();
        }
    }

    public void previousRight(){
        System.out.println("CALL: previousRight() from InterfaceBean");
        try{
            if(shift==0){
                index = listOfPhrases.size() - 2;
                currPhrase = listOfPhrases.get(index);
//                currPhrase.isAnswered = true;
                currPhrase.rightAnswer();
                resultProcessing();
            }
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousRight() from InterfaceBean");
            e.printStackTrace();
        }
    }

    public void previousWrong(){
        System.out.println("CALL: previousWrong() from InterfaceBean");
        try{
            if(shift==0){
                index = listOfPhrases.size() - 2;
                currPhrase = listOfPhrases.get(index);
//                currPhrase.isAnswered = false;
                currPhrase.wrongAnswer();
                resultProcessing();
            }
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousWrong() from InterfaceBean");
            e.printStackTrace();
        }
    }

    public void checkTheAnswer(){
        System.out.println("CALL: checkTheAnswer() from InterfaceBean");
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
        System.out.println("CALL: nextQuestion() from InterfaceBean");
        if(shift==0) {
            newPhrase();
            index = listOfPhrases.size() - 1;
            currPhrase = listOfPhrases.get(index);
            question = currPhrase.natWord;
        }else {
            index = listOfPhrases.size() - 1 - --shift;
            currPhrase = listOfPhrases.get(index);
            question = currPhrase.natWord;
        }
        resultProcessing();
//        System.out.println("--- nextQuestion() List size="+(listOfPhrases.size()+" Current shift="+shift+" Requested index="+index));
    }

    public void previousQuestion(){
        System.out.println("CALL: previousQuestion() from InterfaceBean");
        if(shift<(listOfPhrases.size()-1))
            shift++;
        index = listOfPhrases.size() - 1 - shift;
        if(index<0)
            index = 0;
        currPhrase = listOfPhrases.get(index);
        question = currPhrase.natWord;
        resultProcessing();
//        System.out.println("--- previousQuestion() List size="+(listOfPhrases.size()+" Current shift="+shift+" Requested index="+index));
    }

    public void updatePhrase(){
        currPhrase.updatePhrase();
    }

    public void exit(){
        System.out.println("CALL: exit() from InterfaceBean");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpSession sess = request.getSession();
        sess.invalidate();
        try {
            context.getExternalContext().redirect("index.xhtml");
        } catch (IOException e) {
            System.out.println("EXCEPTION: in exit() from InterfaceBean");
            e.printStackTrace();
        }

//        dao.backupDB();
    }

    /*public ArrayList<Phrase> returnListOfPhrases(){
        return listOfPhrases;   //Was comented 24/5/2016
    }*/

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

    public String getPdLastAccs() {
        return pdLastAccs;
    }
    public void setPdLastAccs(String pdLastAccs) {
        this.pdLastAccs = pdLastAccs;
    }

    public String getPdCreateDate() {
        return pdCreateDate;
    }
    public void setPdCreateDate(String pdCreateDate) {
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

    public String getStrLastAccs() {
        return strLastAccs;
    }
    public void setStrLastAccs(String strLastAccs) {
        this.strLastAccs = strLastAccs;
    }

    public String getStrCreateDate() {
        return strCreateDate;
    }
    public void setStrCreateDate(String strCreateDate) {
        this.strCreateDate = strCreateDate;
    }

    public ArrayList<Phrase> getListOfPhrases(){
        return listOfPhrases;
    }
    public void setListOfPhrases(ArrayList<Phrase> listOfPhrases){
        this.listOfPhrases = listOfPhrases;
    }

    public String getChoosedLabel() {
        return choosedLabel;
    }
    public void setChoosedLabel(String choosedLabel) {
        this.choosedLabel = choosedLabel;
    }

    public ArrayList<String> getListOfChooses() {
        return listOfChooses;
    }
    public void setListOfChooses(ArrayList<String> listOfChooses) {
        this.listOfChooses = listOfChooses;
    }

    public String getResultChoosedLabel() {
        return resultChoosedLabel;
    }
    public void setResultChoosedLabel(String resultChoosedLabel) {
        this.resultChoosedLabel = resultChoosedLabel;

    }

    public String getCurrPhrNatWord() {
        return currPhrNatWord;
    }
    public void setCurrPhrNatWord(String currPhrNatWord) {
        this.currPhrNatWord = currPhrNatWord;
        currPhrase.natWord = this.currPhrNatWord;
        currPhrase.isModified = true;
    }

    public String getCurrPhrForWord() {
//        System.out.println("--- inside getCurrPhrForWord() currPhrForWord is " + currPhrForWord);
        return currPhrForWord;
    }
    public void setCurrPhrForWord(String currPhrForWord) {
        this.currPhrForWord = currPhrForWord;
        currPhrase.forWord = this.currPhrForWord;
        currPhrase.isModified = true;
    }

    public String getCurrPhrTransc() {
        return currPhrTransc;
    }
    public void setCurrPhrTransc(String currPhrTransc) {
        this.currPhrTransc = currPhrTransc;
        currPhrase.transcr = this.currPhrTransc;
        currPhrase.isModified = true;
    }

    public String getCurrPhrLabel() {
        return currPhrLabel;
    }
    public void setCurrPhrLabel(String currPhrLabel) {
        this.currPhrLabel = currPhrLabel;
        currPhrase.label = this.currPhrLabel;
        currPhrase.isModified = true;
    }
}




