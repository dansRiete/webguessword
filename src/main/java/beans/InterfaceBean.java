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

    /*@ManagedProperty(value="#{stat}")
    private StatBean statBean;
    public void setStatBean(StatBean statBean) {
        this.statBean = statBean;
    }*/

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    private int answersForSessionNumber;
    private int trainedPhrasesNumber;
    private int nonLearnedWordsNumber;
    private int averageAnswersPerDayNumber;
    private String trainingCompletionPercent;
    private int totalPhrasesNumber;
    private int trainedPhrasesPerSessionNumber;
    private String rightAnswersPercentage;
    private BigDecimal currentPhraseLastAccessDate;
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
    private Phrase selectedPhrase;
    private String question ="";
    private String answerField = "";
    private String trainingLog = "";
    private final static String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong>";
    private final static String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private final static String NONANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private ArrayList<Phrase> answeredPhrases = new ArrayList<>();
    private ArrayList<String> listOfChooses;
    private String choosedLabel;
    private String resultChoosedLabel;
    private String previousResultChoosedLabel = "";
    private HashSet<String> hshset = new HashSet<>();
    private int shift = 0;
    private int index;
    private int beforeCurrentSessionPhrasesNumber;
    private Hints hint = new Hints();


    public InterfaceBean(){
        System.out.println("CALL: InterfaceBean constructor");
        init();
    }

    @PostConstruct
    private void init(){

        if(loginBean != null)
            dao = loginBean.getDao();
        if(dao != null){
            listOfChooses = dao.possibleLabels;
            answeredPhrases = dao.getTodaysPhrasesCollection();
            beforeCurrentSessionPhrasesNumber = answeredPhrases.size();
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

        //After the answerField creates String like this - "40.2 ➩ 37.3"
        if(!selectedPhrase.hasBeenAnswered){
            currPhrProb = selectedPhrase.probabilityFactor.setScale(1, RoundingMode.HALF_UP).toString();
        }else{
            currPhrProb = selectedPhrase.beforeCurrentAnswerProbabilityFactor.setScale(1, RoundingMode.HALF_UP) + "➩"
                    + selectedPhrase.probabilityFactor.setScale(1, RoundingMode.HALF_UP);
        }

        //After the answerField creates String like this - "0.06116% ➩ 0.07294%"
         currPhrPercentOfAppearance = /*selectedPhrase.getPercentChanceView();*/ "NOT YET IMPLEMENTED";


        if(selectedPhrase.lastAccessDate != null){

            currPhrAbsLastAccsDate = LocalDateTime.ofInstant(selectedPhrase.lastAccessDate.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelLastAccsDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedPhrase.lastAccessDate.getTime());
        }

        if(selectedPhrase.addingToCollectionDate != null){

            currPhrAbsCreateDate = LocalDateTime.ofInstant(selectedPhrase.addingToCollectionDate.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedPhrase.addingToCollectionDate.getTime());
        }

        currPhrLabel = selectedPhrase.label;

        //>>Calculate session statistics
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        int numOfPhrForSession = answeredPhrases.size();
        currPhrId = selectedPhrase.id;
        currentPhraseRate = selectedPhrase.multiplier;

        for(Phrase phrs : answeredPhrases){
            if(!phrs.hasBeenAnswered)
                numOfNonAnswForSession++;
            else if(phrs.hasBeenAnsweredCorrectly)
                numOfRightAnswForSession++;
        }

        answersForSessionNumber = numOfPhrForSession - numOfNonAnswForSession;
        //Формирует строку с процентным соотношением правильных ответов к общему кол-ву ответов
        rightAnswersPercentage = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(answersForSessionNumber ==0?1: answersForSessionNumber),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        trainedPhrasesNumber = (int) dao.learnedWords;
        nonLearnedWordsNumber = (int) dao.nonLearnedWords;
        totalPhrasesNumber = trainedPhrasesNumber + nonLearnedWordsNumber;

         try{
             averageAnswersPerDayNumber = (int) ( (float) (dao.answUntil6amAmount + answersForSessionNumber)/ (float) (dao.totalHoursUntil6am + ZonedDateTime.now(ZoneId.of("Europe/Kiev")).getHour()-6) * 24);
         }catch (ArithmeticException e){
             averageAnswersPerDayNumber = 0;
         }
        //<<

    }

    private void reloadTrainingLog(){

        System.out.println("CALL: reloadTrainingLog() from InterfaceBean");
        reloadStatTableData();
        StringBuilder str = new StringBuilder();
        trainedPhrasesPerSessionNumber = 0;

        for (int i = answeredPhrases.size() - 1; i >= 0; i--) {
            Phrase currentPhrase = answeredPhrases.get(i);
            if (currentPhrase.isTrained() && !currentPhrase.wasTrainedBeforeAnswer()) {
                trainedPhrasesPerSessionNumber++;
            } else if (!currentPhrase.isTrained() && currentPhrase.wasTrainedBeforeAnswer()) {
                trainedPhrasesPerSessionNumber--;
            }
            if(i == beforeCurrentSessionPhrasesNumber - 1){
                str.append("—————————————————————————————————————</br>");
            }
            if (!currentPhrase.hasBeenAnswered){
                str.append(i == index ? "<strong>" : "").append("[").append(currentPhrase.creationDate.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append(NONANSWERED_MESSAGE).append("] ")
                        .append(currentPhrase.isTrained() ? "<font color=\"green\">" : "")
                        .append(currentPhrase.nativeWord).append(currentPhrase.isTrained() ? "</font>" : "")
                        .append((i == index ? "</strong>" : "")).append("</br>");
            } else if (currentPhrase.hasBeenAnsweredCorrectly){
                str.append(i == index ? "<strong>" : "").append("[").append(currentPhrase.creationDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(RIGHT_MESSAGE).append("] ").append(currentPhrase.isTrained() ? "<font color=\"green\">" : "")
                        .append(currentPhrase.nativeWord).append(" - ")
                        .append(currentPhrase.getForWordAndTranscription())
                        .append(currentPhrase.isTrained() ? "</font>" : "").append((i == index ? "</strong>" : "")).append("</br>");
            } else if (!currentPhrase.hasBeenAnsweredCorrectly){
                str.append(i == index ? "<strong>" : "").append("[").append(currentPhrase.creationDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(WRONG_MESSAGE).append("] ").append(currentPhrase.isTrained() ? "<font color=\"green\">" : "")
                        .append(currentPhrase.nativeWord).append(" - ")
                        .append(currentPhrase.getForWordAndTranscription())
                        .append(currentPhrase.isTrained() ? "</font>" : "").append((i == index ? "</strong>" : "")).append("</br>");
            }

        }

        trainingLog = str.toString();
//        Phrase currentPhrase = answeredPhrases.get(index);
        currPhrForWord = selectedPhrase.foreignWord;
        currPhrNatWord = selectedPhrase.nativeWord;
        currPhrTransc = selectedPhrase.transcription;
        currPhrLabel = selectedPhrase.label;
        currentPhraseRate = selectedPhrase.multiplier;
        if (selectedPhrase.isModified){
            selectedPhrase.updatePhraseInDb();
        }

    }


    public void rightAnswer(){
        System.out.println("CALL: rightAnswer() from InterfaceBean");
        long starTime = System.nanoTime();
        try {
            index = answeredPhrases.size() - 1 - shift;
            selectedPhrase = answeredPhrases.get(index);
            selectedPhrase.rightAnswer();
            nextQuestion();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in rightAnswer() from InterfaceBean");
            e.printStackTrace();
        }
        currentPhraseLastAccessDate = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void wrongAnswer(){
        System.out.println("CALL: wrongAnswer() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            index = answeredPhrases.size() - 1 - shift;
            selectedPhrase = answeredPhrases.get(index);
            selectedPhrase.wrongAnswer();
            nextQuestion();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in wrongAnswer() from InterfaceBean");
            e.printStackTrace();
        }
        currentPhraseLastAccessDate = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void previousRight(){
        System.out.println("CALL: previousRight() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            answeredPhrases.get(index-1).rightAnswer();
            reloadTrainingLog();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousRight() from InterfaceBean");
            e.printStackTrace();
        }
        currentPhraseLastAccessDate = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void previousWrong(){
        System.out.println("CALL: previousWrong() from InterfaceBean");
        long starTime = System.nanoTime();
        try{
            answeredPhrases.get(index -1).wrongAnswer();
            reloadTrainingLog();
        }catch (NullPointerException e){
            System.out.println("EXCEPTION: in previousWrong() from InterfaceBean");
            e.printStackTrace();
        }
        currentPhraseLastAccessDate = new BigDecimal(System.nanoTime()-starTime).divide(new BigDecimal(1000000), BigDecimal.ROUND_HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public void checkTheAnswer(){
        System.out.println("CALL: checkTheAnswer() from InterfaceBean");

        if(answerField != null){

            if (answerField.equals("+")){
                rightAnswer();
            }else if (answerField.equals("-") || answerField.equals("")){
                wrongAnswer();
            }else if (answerField.equals("++")){
                previousRight();
            }else if (answerField.equals("--")){
                previousWrong();
            }else if(!(answerField.equals("") || answerField.equals("+") || answerField.equals("-") ||
                    answerField.equals("++") || answerField.equals("--"))){

                Answer givenAnswer = new Answer(answerField, selectedPhrase/*answeredPhrases.get(answeredPhrases.size() - 1 - shift)*/);
                if(givenAnswer.isCorrect()){
                    rightAnswer();
                } else {
                    wrongAnswer();
                }

            }
            answerField = "";
        }
    }

    public void nextQuestion(){
        System.out.println("CALL: nextQuestion() from InterfaceBean");
        if(shift == 0) {
            newPhrase();
            index = answeredPhrases.size() - 1;
            selectedPhrase = answeredPhrases.get(index);
            question = selectedPhrase.nativeWord + " " + hint.getShortHint(selectedPhrase.foreignWord);
        }else {
            index = answeredPhrases.size() - 1 - --shift;
            selectedPhrase = answeredPhrases.get(index);
            question = selectedPhrase.nativeWord + " " + hint.getShortHint(selectedPhrase.foreignWord);
        }

        reloadTrainingLog();

    }

    private void newPhrase(){
        System.out.println("CALL: newPhrase() from InterfaceBean");
        Phrase newPhrase = new Phrase(dao.obtainRandomPhrase());
        answeredPhrases.add(newPhrase);
        selectedPhrase = newPhrase;
    }

    public void previousQuestion(){
        System.out.println("CALL: previousQuestion() from InterfaceBean");
        if(shift < (answeredPhrases.size() - 1)){
            shift++;
        }
        index = answeredPhrases.size() - 1 - shift;
        if(index < 0){
            index = 0;
        }
        selectedPhrase = answeredPhrases.get(index);
        question = selectedPhrase.nativeWord;
        reloadTrainingLog();
    }

    public void deletePhrase(){
        System.out.println("CALL: deleteThisPhrase() from InterfaceBean");
        selectedPhrase.deleteThisPhrase();
    }

    public void exitCurrentSession(){
        System.out.println("CALL: exitCurrentSession() from InterfaceBean");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpSession sess = request.getSession();
        sess.invalidate();
        try {
            context.getExternalContext().redirect("index.xhtml");
        } catch (IOException e) {
            System.out.println("EXCEPTION: in exitCurrentSession() from InterfaceBean");
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

    public String getAnswerField() {
        return answerField;
    }
    public void setAnswerField(String answerField) {
        this.answerField = answerField;
    }

    public String getTrainingLog() {
        return trainingLog;
    }
    public void setTrainingLog(String res) {
        this.trainingLog = res;
    }

    public String getRightAnswersPercentage() {
        return rightAnswersPercentage;
    }
    public void setRightAnswersPercentage(String rightAnswersPercentage) {
        this.rightAnswersPercentage = rightAnswersPercentage;
    }

    public int getAnswersForSessionNumber() {
        return answersForSessionNumber;
    }
    public void setAnswersForSessionNumber(int answersForSessionNumber) {
        this.answersForSessionNumber = answersForSessionNumber;
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

    public BigDecimal getCurrentPhraseLastAccessDate() {
        return currentPhraseLastAccessDate;
    }
    public void setCurrentPhraseLastAccessDate(BigDecimal currentPhraseLastAccessDate) {
        this.currentPhraseLastAccessDate = currentPhraseLastAccessDate;
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
        selectedPhrase.nativeWord = this.currPhrNatWord;
        selectedPhrase.isModified = true;
    }

    public String getCurrPhrForWord() {
        return currPhrForWord;
    }
    public void setCurrPhrForWord(String currPhrForWord) {

        this.currPhrForWord = currPhrForWord;
        selectedPhrase.foreignWord = this.currPhrForWord;
        selectedPhrase.isModified = true;
        System.out.println("--- inside setCurrPhrForWord(String currPhrForWord) currPhrForWord is " + this.currPhrForWord);
    }

    public String getCurrPhrTransc() {
        return currPhrTransc;
    }
    public void setCurrPhrTransc(String currPhrTransc) {
        this.currPhrTransc = currPhrTransc;
        selectedPhrase.transcription = this.currPhrTransc;
        selectedPhrase.isModified = true;
    }

    public String getCurrPhrLabel() {
        return currPhrLabel;
    }
    public void setCurrPhrLabel(String currPhrLabel) {
        this.currPhrLabel = currPhrLabel;
        selectedPhrase.label = this.currPhrLabel;
        selectedPhrase.isModified = true;
    }

    public int getId() {
        return currPhrId;
    }
    public void setId(int id) {
        this.currPhrId = id;
    }

    public int getTrainedPhrasesNumber() {
        return trainedPhrasesNumber;
    }
    public void setTrainedPhrasesNumber(int trainedPhrasesNumber) {
        this.trainedPhrasesNumber = trainedPhrasesNumber;
    }

    public int getNonLearnedWordsNumber() {
        return nonLearnedWordsNumber;
    }
    public void setNonLearnedWordsNumber(int nonLearnedWordsNumber) {
        this.nonLearnedWordsNumber = nonLearnedWordsNumber;
    }

    public int getTotalPhrasesNumber() {
        return totalPhrasesNumber;
    }
    public void setTotalPhrasesNumber(int totalPhrasesNumber) {
        this.totalPhrasesNumber = totalPhrasesNumber;
    }

    public int getTrainedPhrasesPerSessionNumber() {
        return trainedPhrasesPerSessionNumber;
    }
    public void setTrainedPhrasesPerSessionNumber(int trainedPhrasesPerSessionNumber) {
        this.trainedPhrasesPerSessionNumber = trainedPhrasesPerSessionNumber;
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

    public int getAverageAnswersPerDayNumber() {
        return averageAnswersPerDayNumber;
    }
    public void setAverageAnswersPerDayNumber(int averageAnswersPerDayNumber) {
        this.averageAnswersPerDayNumber = averageAnswersPerDayNumber;
    }

    public String getTrainingCompletionPercent() {
        return trainingCompletionPercent;
    }
    public void setTrainingCompletionPercent(String trainingCompletionPercent) {
        this.trainingCompletionPercent = trainingCompletionPercent;
    }

    public double getCurrentPhraseRate() {
        return currentPhraseRate;
    }
    public void setCurrentPhraseRate(double currentPhraseRate) {
        this.currentPhraseRate = currentPhraseRate;
    }
}




