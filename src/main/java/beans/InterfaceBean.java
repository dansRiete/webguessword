package beans;

import logic.*;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private int numOfAnswForSession;
    private int learnedWords;
    private int nonLearnedWords;
    private int avgAnswersPerDay;
    private String percentOfCompleteLearning;
    private int totalNumberOfPhrases;
    private int numberOfLearnedPhrasePerSession;
    private String percentOfRightAnswers;
    private BigDecimal timeOfLastAccsToDb;
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
    private double currentPhraseRate;
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
            listOfPhrases = dao.getTodaysPhrasesCollection();
            nextQuestion();
        }
    }

    public void setTable() {
        /*new ArrayList<>().

        System.out.println("CALL: setTable() from InterfaceBean");*/

        if (choosedLabel != null && (!choosedLabel.equalsIgnoreCase(""))){
            if(!choosedLabel.equalsIgnoreCase("all")){
                hshset.add(choosedLabel);
            }else{
                hshset.clear();
            }
        }

        resultChoosedLabel = "";
        boolean temp = true;

        for(String str : hshset){   //Makes a "WHERE LABEL IN" clause
            if(temp){
                resultChoosedLabel += "'" + str + "'";
                temp = false;
            }else {
                resultChoosedLabel += ",'" + str + "'";
            }
        }

        if(!resultChoosedLabel.equals(previousResultChoosedLabel)){ //If clause was changed
            dao.activeChosedLabels = hshset;
            dao.reloadPhrasesCollection();
            previousResultChoosedLabel = resultChoosedLabel;
            reloadStatTableData();
        }
    }



     private void reloadStatTableData(){
        System.out.println("CALL: reloadStatTableData() from InterfaceBean");

        //After the answer creates String like this - "40.2 ➩ 37.3"
        if(!currPhrase.thisPhraseHadBeenAnswered){
            currPhrProb = currPhrase.probabilityFactor.setScale(1, RoundingMode.HALF_UP).toString();
        }else{
            currPhrProb = currPhrase.beforeCurrentAnswerProbabilityFactor.setScale(1, RoundingMode.HALF_UP) + "➩"
                    + currPhrase.probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        }

        //After the answer creates String like this - "0.06116% ➩ 0.07294%"
         currPhrPercentOfAppearance = /*currPhrase.getPercentChanceView();*/ "NOT YET IMPLEMENTED";


        if(currPhrase.lastAccessDate !=null){

            currPhrAbsLastAccsDate = LocalDateTime.ofInstant(currPhrase.lastAccessDate.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelLastAccsDate = retDiff.retDiffInTime(System.currentTimeMillis() - currPhrase.lastAccessDate.getTime());
        }

        if(currPhrase.addingToCollectionDate !=null){

            currPhrAbsCreateDate = LocalDateTime.ofInstant(currPhrase.addingToCollectionDate.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - currPhrase.addingToCollectionDate.getTime());
        }

        currPhrLabel = currPhrase.label;

        //>>Calculate session statistics
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        int numOfPhrForSession = listOfPhrases.size();
        currPhrId = currPhrase.id;
        currentPhraseRate = currPhrase.multiplier;

        for(Phrase phrs : listOfPhrases){
            if(!phrs.thisPhraseHadBeenAnswered)
                numOfNonAnswForSession++;
            else if(phrs.thisPhraseHadBeenAnsweredCorrectly)
                numOfRightAnswForSession++;
        }

        numOfAnswForSession = numOfPhrForSession - numOfNonAnswForSession;
        //Формирует строку с процентным соотношением правильных ответов к общему кол-ву ответов
        percentOfRightAnswers = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(numOfAnswForSession==0?1:numOfAnswForSession),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        learnedWords = (int) dao.learnedWords;
        nonLearnedWords = (int) dao.nonLearnedWords;
        totalNumberOfPhrases = learnedWords + nonLearnedWords;

         try{
             avgAnswersPerDay = (int) ( (float) (dao.answUntil6amAmount +numOfAnswForSession)/ (float) (dao.totalHoursUntil6am + ZonedDateTime.now(ZoneId.of("Europe/Kiev")).getHour()-6) * 24);
         }catch (ArithmeticException e){
             avgAnswersPerDay = 0;
         }
        //<<

    }

    private void resultProcessing(){

        System.out.println("CALL: resultProcessing() from InterfaceBean");
        reloadStatTableData();
        StringBuilder str = new StringBuilder();
        int countOfLearnedPhrases = 0;

        for (int i = listOfPhrases.size() - 1; i >= 0; i--) {
            //If the phrase has been learnt and had not been learnt before then increase the counter of learnt phrases per current session
            if (listOfPhrases.get(i).hasThisPhraseBeenLearnt() && !listOfPhrases.get(i).hadThisPhraseBeenLearntBeforeCurrentAnswer()) {
                countOfLearnedPhrases++;
            }
            //If vice-versa --
            if (!listOfPhrases.get(i).hasThisPhraseBeenLearnt() && listOfPhrases.get(i).hadThisPhraseBeenLearntBeforeCurrentAnswer()) {
                countOfLearnedPhrases--;
            }
            if (!listOfPhrases.get(i).thisPhraseHadBeenAnswered)
                str.append(i == index ? "<strong>" : "").append("[").append(listOfPhrases.get(i).creationDate.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append(NONANSWERED_MESSAGE).append("] ")
                        .append(listOfPhrases.get(i).hasThisPhraseBeenLearnt() ? "<font color=\"green\">" : "")
                        .append(listOfPhrases.get(i).nativeWord).append(listOfPhrases.get(i).hasThisPhraseBeenLearnt() ? "</font>" : "")
                        .append((i == index ? "</strong>" : "")).append("</br>");
            else if (listOfPhrases.get(i).thisPhraseHadBeenAnsweredCorrectly)
                str.append(i == index ? "<strong>" : "").append("[").append(listOfPhrases.get(i).creationDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(RIGHT_MESSAGE).append("] ").append(listOfPhrases.get(i).hasThisPhraseBeenLearnt() ? "<font color=\"green\">" : "")
                        .append(listOfPhrases.get(i).nativeWord).append(" - ")
                        .append(listOfPhrases.get(i).getForWordAndTranscription())
                        .append(listOfPhrases.get(i).hasThisPhraseBeenLearnt() ? "</font>" : "").append((i == index ? "</strong>" : "")).append("</br>");
            else if (!listOfPhrases.get(i).thisPhraseHadBeenAnsweredCorrectly)
                str.append(i == index ? "<strong>" : "").append("[").append(listOfPhrases.get(i).creationDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(WRONG_MESSAGE).append("] ").append(listOfPhrases.get(i).hasThisPhraseBeenLearnt() ? "<font color=\"green\">" : "")
                        .append(listOfPhrases.get(i).nativeWord).append(" - ")
                        .append(listOfPhrases.get(i).getForWordAndTranscription())
                        .append(listOfPhrases.get(i).hasThisPhraseBeenLearnt() ? "</font>" : "").append((i == index ? "</strong>" : "")).append("</br>");

        }

        numberOfLearnedPhrasePerSession = countOfLearnedPhrases;
        result = str.toString();
        Phrase currentPhrase = listOfPhrases.get(index);
        currPhrForWord = currentPhrase.foreignWord;
        currPhrNatWord = currentPhrase.nativeWord;
        currPhrTransc = currentPhrase.transcription;
        currPhrLabel = currentPhrase.label;
        currentPhraseRate = currentPhrase.multiplier;
        if (currPhrase.isModified){
            currPhrase.updatePhraseInDb();
        }

    }

    private void newPhrase(){
        System.out.println("CALL: newPhrase() from InterfaceBean");
        Phrase newPhrase = new Phrase(dao.obtainRandomPhrase());
        listOfPhrases.add(newPhrase);
        currPhrase = newPhrase;
    }

    public void rightAnswer(String answer){
        System.out.println("CALL: rightAnswer() from InterfaceBean");
        long starTime = System.nanoTime();
        try {
            index = listOfPhrases.size() - 1 - shift;
            currPhrase = listOfPhrases.get(index);
            currPhrase.rightAnswer();
            nextQuestion();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in rightAnswer() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void wrongAnswer(String answer){
        System.out.println("CALL: wrongAnswer() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            index = listOfPhrases.size() - 1 - shift;
            currPhrase = listOfPhrases.get(index);
            currPhrase.wrongAnswer();
            nextQuestion();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in wrongAnswer() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void previousRight(){
        System.out.println("CALL: previousRight() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            listOfPhrases.get(index-1).rightAnswer();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousRight() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void previousWrong(){
        System.out.println("CALL: previousWrong() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            listOfPhrases.get(index -1).wrongAnswer();
            resultProcessing();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousWrong() from InterfaceBean");
            e.printStackTrace();
        }
        timeOfLastAccsToDb = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
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
//                boolean bool = intelliFind.match(listOfPhrases.get(listOfPhrases.size() - 1 - shift).foreignWord, answer, false);
                Answer currentAnswer = new Answer(answer, listOfPhrases.get(listOfPhrases.size() - 1 - shift));
                if(currentAnswer.isCorrect()){
                    rightAnswer(answer);
                } else {
                    wrongAnswer(answer);
                }

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
            question = currPhrase.nativeWord + " " + hint.getShortHint(currPhrase.foreignWord);
        }else {
            index = listOfPhrases.size() - 1 - --shift;
            currPhrase = listOfPhrases.get(index);
            question = currPhrase.nativeWord + " " + hint.getShortHint(currPhrase.foreignWord);
        }

        resultProcessing();

    }

    public void previousQuestion(){
        System.out.println("CALL: previousQuestion() from InterfaceBean");
        if(shift<(listOfPhrases.size()-1))
            shift++;
        index = listOfPhrases.size() - 1 - shift;
        if(index<0)
            index = 0;
        currPhrase = listOfPhrases.get(index);
        question = currPhrase.nativeWord;
        resultProcessing();
    }

    public void delete(){
        System.out.println("CALL: deleteThisPhrase() from InterfaceBean");
        currPhrase.deleteThisPhrase();
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

    }



    //>>>>>>>>>>>>    Setters and getters     >>>>>>>>>>>>>

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

    public BigDecimal getTimeOfLastAccsToDb() {
        return timeOfLastAccsToDb;
    }
    public void setTimeOfLastAccsToDb(BigDecimal timeOfLastAccsToDb) {
        this.timeOfLastAccsToDb = timeOfLastAccsToDb;
    }

    public String getCurrPhrAbsCreateDate() {
        return currPhrAbsCreateDate;
    }
    public void setCurrPhrAbsCreateDate(String strCreateDate) {
        this.currPhrAbsCreateDate = strCreateDate;
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
        currPhrase.nativeWord = this.currPhrNatWord;
        currPhrase.isModified = true;
    }

    public String getCurrPhrForWord() {
        return currPhrForWord;
    }
    public void setCurrPhrForWord(String currPhrForWord) {

        this.currPhrForWord = currPhrForWord;
        currPhrase.foreignWord = this.currPhrForWord;
        currPhrase.isModified = true;
        System.out.println("--- inside setCurrPhrForWord(String currPhrForWord) currPhrForWord is " + this.currPhrForWord);
    }

    public String getCurrPhrTransc() {
        return currPhrTransc;
    }
    public void setCurrPhrTransc(String currPhrTransc) {
        this.currPhrTransc = currPhrTransc;
        currPhrase.transcription = this.currPhrTransc;
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

    public int getAvgAnswersPerDay() {
        return avgAnswersPerDay;
    }
    public void setAvgAnswersPerDay(int avgAnswersPerDay) {
        this.avgAnswersPerDay = avgAnswersPerDay;
    }

    public String getPercentOfCompleteLearning() {
        return percentOfCompleteLearning;
    }
    public void setPercentOfCompleteLearning(String percentOfCompleteLearning) {
        this.percentOfCompleteLearning = percentOfCompleteLearning;
    }

    public double getCurrentPhraseRate() {
        return currentPhraseRate;
    }
    public void setCurrentPhraseRate(double currentPhraseRate) {
        this.currentPhraseRate = currentPhraseRate;
    }
}




