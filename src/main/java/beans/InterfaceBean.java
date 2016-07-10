package beans;

import logic.*;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    //>>Session statisticss
    private int numOfPhrForSession;
    private int numOfAnswForSession;
    private int totalNumberOfWords;
    private int totalNumberOfLearnedWords;
    private int learnedWords;
    private int nonLearnedWords;
    private int totalNumberOfPhrases;
    private int numberOfLearnedPhrasePerSession;
    private String percentOfRightAnswers;
    private BigDecimal timeOfLastAccsToDb;
    private BigDecimal timeOfReturningPhraseFromCollection;
    //<<


    //>>Current phrase data
    private String currPhrNatWord;
    private String currPhrForWord;
    private String currPhrTransc;
    private String currPhrLabel;
    private String currPhrPercentOfAppearance;
    private String currPhrProb;
    private String currPhrAbsLastAccsDate;
    private String currPhrAbsCreateDate;
    private String currPhrRelLastAccsDate;
    private String currPhrRelCreateDate;
    private int currPhrId;
    //<<

    private RetDiff retDiff = new RetDiff();
    private DAO dao;
    private Phrase currPhrase;
    private String question ="";
    private String answer = "";
    private String result = "";
    private final static String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong>";
    private final static String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private final static String NONANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private ArrayList<Phrase> listOfPhrases = new ArrayList<>();
    private ArrayList<String> listOfChooses;
    private String choosedLabel;
    private String resultChoosedLabel;
    private String previousResultChoosedLabel = "";
    private HashSet<String> hshset = new HashSet<>();
    private int shift = 0;
    private int index;
    private Hints hint = new Hints();


    public InterfaceBean(){
        System.out.println("CALL: InterfaceBean constructor");
        init();
    }

    @PostConstruct
    private void init(){

        if(loginBean!=null)
            dao = loginBean.getDao();
        if(dao!=null){
            listOfChooses = dao.possibleLabels;
            nextQuestion();
        }
    }

    public void setTable() {
        System.out.println("CALL: setTable() from InterfaceBean");

        if (choosedLabel != null && (!choosedLabel.equalsIgnoreCase(""))){

            if(!choosedLabel.equalsIgnoreCase("all")){
                hshset.add(choosedLabel);
            }else{
                hshset.clear();
            }
        }

        resultChoosedLabel = "";
        boolean temp = true;

        //>>Makes a "WHERE LABEL IN" clause string from collection
        for(String str : hshset){
            if(temp){
                resultChoosedLabel += "'" + str + "'";
                temp = false;
            }else {
                resultChoosedLabel += ",'" + str + "'";
            }
        }
        //<<

        //If clause was changed
        if(!resultChoosedLabel.equals(previousResultChoosedLabel)){
            if(!resultChoosedLabel.equalsIgnoreCase("")){
//                dao.table = "(SELECT * FROM " + loginBean.getUser() + " WHERE LABEL IN(" + resultChoosedLabel + ")) As custom";
            }
            else{
//                dao.table = loginBean.getUser();
            }
            dao.chosedLabels = hshset;
            dao.reloadCollectionOfPhrases();
            previousResultChoosedLabel = resultChoosedLabel;
            dao.reloadIndices(1);
            reloadStatTableData();
        }
    }

     private void reloadStatTableData(){
        System.out.println("CALL: reloadStatTableData() from InterfaceBean");

        //After the answer creates String like this - "40.2 ➩ 37.3"
        if(currPhrase.howWasAnswered == null)

            currPhrProb = currPhrase.prob.setScale(1, RoundingMode.HALF_UP).toString();
        else

            currPhrProb = currPhrase.returnUnmodified().prob.setScale(1, RoundingMode.HALF_UP) + "➩"
                    + currPhrase.prob.setScale(1, RoundingMode.HALF_UP);

        //After the answer creates String like this - "0.06116% ➩ 0.07294%"
        if(currPhrase.howWasAnswered == null)

            currPhrPercentOfAppearance = new BigDecimal(currPhrase.indexEnd-currPhrase.indexStart).divide(new BigDecimal(1.0e+7))
                    .setScale(5, RoundingMode.HALF_UP) + "%";
        else {

            BigDecimal percentOfAppear = new BigDecimal(currPhrase.indexEnd-currPhrase.indexStart).divide(new BigDecimal(1.0e+7))
                    .setScale(5, RoundingMode.HALF_UP);

            BigDecimal previousPercentOfAppear = new BigDecimal(currPhrase.returnUnmodified().indexEnd-currPhrase.returnUnmodified().indexStart)
                    .divide(new BigDecimal(1.0e+7)).setScale(5, RoundingMode.HALF_UP);

            currPhrPercentOfAppearance = previousPercentOfAppear + "% ➩ " + percentOfAppear + "%";
        }

        if(currPhrase.lastAccs!=null){

            currPhrAbsLastAccsDate = LocalDateTime.ofInstant(currPhrase.lastAccs.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelLastAccsDate = retDiff.retDiffInTime(System.currentTimeMillis() - currPhrase.lastAccs.getTime());
        }

        if(currPhrase.createDate!=null){

            currPhrAbsCreateDate = LocalDateTime.ofInstant(currPhrase.createDate.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - currPhrase.createDate.getTime());
        }

        currPhrLabel = currPhrase.label;

        //>>Calculate seesion statistic
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        currPhrId = currPhrase.id;

        numOfPhrForSession = listOfPhrases.size();
        for(Phrase phrs : listOfPhrases){
            if(phrs.howWasAnswered ==null)
                numOfNonAnswForSession++;
            else if(phrs.howWasAnswered)
                numOfRightAnswForSession++;
        }
        numOfAnswForSession = numOfPhrForSession-numOfNonAnswForSession;
        //Формирует строку с процентным соотношением правильных ответов к общему кол-ву ответов
        percentOfRightAnswers = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(numOfAnswForSession==0?1:numOfAnswForSession),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        learnedWords = (int) dao.learnedWords;
        nonLearnedWords = (int) dao.nonLearnedWords;
        totalNumberOfPhrases = learnedWords+nonLearnedWords;
        //<<

    }

    public void resultProcessing(){
        System.out.println("CALL: resultProcessing() from InterfaceBean");

//        calculateSessionStatistics();

        reloadStatTableData();

        try {
            StringBuilder str = new StringBuilder();
            int countOfLearnedPhrases = 0;
            for (int i = listOfPhrases.size() - 1; i >= 0; i--) {
                //If the phrase has been learnt and had not been learnt before then increase the counter of learnt phrases per current session
                if (listOfPhrases.get(i).isLearnt() && !listOfPhrases.get(i).returnUnmodified().isLearnt()) {
                    countOfLearnedPhrases++;
                }
                //If vice-versa --
                if (!listOfPhrases.get(i).isLearnt() && listOfPhrases.get(i).returnUnmodified().isLearnt()) {
                    countOfLearnedPhrases--;
                }
                if (listOfPhrases.get(i).howWasAnswered == null)
                    str.append(i == index ? "<strong>" : "").append("[").append(listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                            .append(NONANSWERED_MESSAGE).append("] ").append(listOfPhrases.get(i).isLearnt() ? "<font color=\"green\">" : "")
                            .append(listOfPhrases.get(i).natWord).append(listOfPhrases.get(i).isLearnt() ? "</font>" : "")
                            .append((i == index ? "</strong>" : "")).append("</br>");
                else if (listOfPhrases.get(i).howWasAnswered)
                    str.append(i == index ? "<strong>" : "").append("[").append(listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                            .append(RIGHT_MESSAGE).append("] ").append(listOfPhrases.get(i).isLearnt() ? "<font color=\"green\">" : "")
                            .append(listOfPhrases.get(i).natWord).append(" - ").append(listOfPhrases.get(i).forWord).append(listOfPhrases.get(i).transcr == null ? "" : (" - " + listOfPhrases.get(i).transcr)).append(listOfPhrases.get(i).isLearnt() ? "</font>" : "")
                            .append((i == index ? "</strong>" : "")).append("</br>");
                else if (!listOfPhrases.get(i).howWasAnswered)
                    str.append(i == index ? "<strong>" : "").append("[").append(listOfPhrases.get(i).lt.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                            .append(WRONG_MESSAGE).append("] ").append(listOfPhrases.get(i).isLearnt() ? "<font color=\"green\">" : "")
                            .append(listOfPhrases.get(i).natWord).append(" - ").append(listOfPhrases.get(i).forWord).append(listOfPhrases.get(i).transcr == null || listOfPhrases.get(i).transcr.equals("") ? "" : (" - " + listOfPhrases.get(i).transcr))
                            .append(listOfPhrases.get(i).isLearnt() ? "</font>" : "").append((i == index ? "</strong>" : "")).append("</br>");

            }
            numberOfLearnedPhrasePerSession = countOfLearnedPhrases;
            result = str.toString();
            currPhrForWord = listOfPhrases.get(index).forWord;
            currPhrNatWord = listOfPhrases.get(index).natWord;
            currPhrTransc = listOfPhrases.get(index).transcr;
            currPhrLabel = listOfPhrases.get(index).label;
            if (currPhrase.isModified)
                currPhrase.updatePhrase();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private void newPhrase(){
        System.out.println("CALL: newPhrase() from InterfaceBean");
        long starTime = System.nanoTime();
        Phrase phrase = dao.createRandPhrase();
        if(phrase!=null){
            listOfPhrases.add(phrase);
            currPhrase = phrase;
        }
//        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000)).setScale(2, RoundingMode.HALF_UP);
    }

    public void rightAnswer(String answer){
        System.out.println("CALL: rightAnswer() from InterfaceBean");
        long starTime = System.nanoTime();
        try {
            index = listOfPhrases.size() - 1 - shift;
            currPhrase = listOfPhrases.get(index);
            currPhrase.rightAnswer(answer);
            nextQuestion();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in rightAnswer() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000)).setScale(2, RoundingMode.HALF_UP);
    }

    public void wrongAnswer(String answer){
        System.out.println("CALL: wrongAnswer() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            index = listOfPhrases.size() - 1 - shift;
            currPhrase = listOfPhrases.get(index);
            currPhrase.wrongAnswer(answer);
            nextQuestion();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in wrongAnswer() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000)).setScale(2, RoundingMode.HALF_UP);
    }

    public void previousRight(){
        System.out.println("CALL: previousRight() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            listOfPhrases.get(index-1).rightAnswer(null);
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousRight() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000)).setScale(2, RoundingMode.HALF_UP);
    }

    public void previousWrong(){
        System.out.println("CALL: previousWrong() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            listOfPhrases.get(index -1).wrongAnswer(null);
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousWrong() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000)).setScale(2, RoundingMode.HALF_UP);
    }

    public void checkTheAnswer(){
        System.out.println("CALL: checkTheAnswer() from InterfaceBean");
        if(answer!=null){
            if (answer.equals("+")){
                rightAnswer(null);
            }else if (answer.equals("-")){
                wrongAnswer(null);
            }else if (answer.equals("++")){
                previousRight();
            }else if (answer.equals("--")){
                previousWrong();
            }else if(!(answer.equals("")||answer.equals("+")||answer.equals("-")||answer.equals("++")||answer.equals("--"))){
                boolean bool = logic.IntelliFind.match(listOfPhrases.get(listOfPhrases.size() - 1 - shift).forWord, answer, false);
                if(bool)
                    rightAnswer(answer);
                else
                    wrongAnswer(answer);
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
            question = currPhrase.natWord + " " + hint.getSlashHint(currPhrase.forWord);
        }else {
            index = listOfPhrases.size() - 1 - --shift;
            currPhrase = listOfPhrases.get(index);
            question = currPhrase.natWord + " " + hint.getSlashHint(currPhrase.forWord);
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

    public void delete(){
        System.out.println("CALL: delete() from InterfaceBean");
        currPhrase.delete();
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



    //>>Setters and getters
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

    public String getCurrPhrPercentOfAppearance() {
        return currPhrPercentOfAppearance;
    }
    public void setCurrPhrPercentOfAppearance(String pDpercentOfAppearance) {
        this.currPhrPercentOfAppearance = pDpercentOfAppearance;
    }

    public String getCurrPhrProb() {
        return currPhrProb;
    }
    public void setCurrPhrProb(String pDprob) {
        this.currPhrProb = pDprob;
    }

    public String getCurrPhrAbsLastAccsDate() {
        return currPhrAbsLastAccsDate;
    }
    public void setCurrPhrAbsLastAccsDate(String pdLastAccs) {
        this.currPhrAbsLastAccsDate = pdLastAccs;
    }

    public DAO getDao(){
        return dao;
    }

    public String getPdCreateDate() {
        return currPhrAbsCreateDate;
    }
    public void setPdCreateDate(String pdCreateDate) {
        this.currPhrAbsCreateDate = pdCreateDate;
    }

    /*public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }*/

    public BigDecimal getTimeOfLastAccsToDb() {
        return timeOfLastAccsToDb;
    }
    public void setTimeOfLastAccsToDb(BigDecimal timeOfLastAccsToDb) {
        this.timeOfLastAccsToDb = timeOfLastAccsToDb;
    }

    /*public String getStrLastAccs() {
        return strLastAccs;
    }
    public void setStrLastAccs(String strLastAccs) {
        this.strLastAccs = strLastAccs;
    }*/

    public String getCurrPhrAbsCreateDate() {
        return currPhrAbsCreateDate;
    }
    public void setCurrPhrAbsCreateDate(String strCreateDate) {
        this.currPhrAbsCreateDate = strCreateDate;
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
        return currPhrForWord;
    }
    public void setCurrPhrForWord(String currPhrForWord) {

        this.currPhrForWord = currPhrForWord;
        currPhrase.forWord = this.currPhrForWord;
        currPhrase.isModified = true;
        System.out.println("--- inside setCurrPhrForWord(String currPhrForWord) currPhrForWord is " + this.currPhrForWord);
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

    public int getId() {
        return currPhrId;
    }
    public void setId(int id) {
        this.currPhrId = id;
    }

    public int getLearnedWords() {
        return learnedWords;
    }
    public void setLearnedWords(int learnedWords) {
        this.learnedWords = learnedWords;
    }

    public int getNonLearnedWords() {
        return nonLearnedWords;
    }
    public void setNonLearnedWords(int nonLearnedWords) {
        this.nonLearnedWords = nonLearnedWords;
    }

    public int getTotalNumberOfPhrases() {
        return totalNumberOfPhrases;
    }
    public void setTotalNumberOfPhrases(int totalNumberOfPhrases) {
        this.totalNumberOfPhrases = totalNumberOfPhrases;
    }

    public int getNumberOfLearnedPhrasePerSession() {
        return numberOfLearnedPhrasePerSession;
    }
    public void setNumberOfLearnedPhrasePerSession(int numberOfLearnedPhrasePerSession) {
        this.numberOfLearnedPhrasePerSession = numberOfLearnedPhrasePerSession;
    }

    public Phrase getCurrPhrase(){
        return currPhrase;
    }

    public String getCurrPhrRelLastAccsDate() {
        return currPhrRelLastAccsDate;
    }

    public void setCurrPhrRelLastAccsDate(String currPhrRelLastAccsDate) {
        this.currPhrRelLastAccsDate = currPhrRelLastAccsDate;
    }

    public String getCurrPhrRelCreateDate() {
        return currPhrRelCreateDate;
    }

    public void setCurrPhrRelCreateDate(String currPhrRelCreateDate) {
        this.currPhrRelCreateDate = currPhrRelCreateDate;
    }

    public BigDecimal getTimeOfReturningPhraseFromCollection() {
        return timeOfReturningPhraseFromCollection;
    }

    public void setTimeOfReturningPhraseFromCollection(BigDecimal timeOfReturningPhraseFromCollection) {
        this.timeOfReturningPhraseFromCollection = timeOfReturningPhraseFromCollection;
    }
}




