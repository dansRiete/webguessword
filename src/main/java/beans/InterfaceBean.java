package beans;

import datamodel.Phrase;
import logic.Answer;
import logic.DAO;
import logic.Hints;
import logic.RetDiff;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class InterfaceBean implements Serializable{

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;

    //>>Current session data
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
    private String currentPhraseRate;
    //<<

    private RetDiff retDiff = new RetDiff();
    private DAO dao;
    private Phrase selectedPhrase;
    private String question ="";
    private String answerField = "";
    private StringBuilder trainingLog = new StringBuilder("");
    private final static String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong>";
    private final static String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private final static String NON_ANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private ArrayList<Phrase> answeredPhrases = new ArrayList<>();
    private ArrayList<Phrase> todayAnsweredPhrases = new ArrayList<>();
    private ArrayList<String> availableLabels;
    private String choosedLabel;
    private String resultChoosedLabel;
    private String previousResultChoosedLabel = "";
    private HashSet<String> choosedLabelsForLearningWords = new HashSet<>();
    private int currentlySelectedPhraseIndex;
    private Hints hint = new Hints();


    public InterfaceBean(){
        System.out.println("CALL: InterfaceBean constructor");
        init();
    }

    @PostConstruct
    private void init(){
        System.out.println("CALL: init() from InterfaceBean");
        if(loginBean != null)
            dao = loginBean.getDao();
        if(dao != null){
            availableLabels = dao.availableLabels;
            todayAnsweredPhrases = dao.retrieveTodayAnsweredPhrases();
            answeredPhrases.addAll(todayAnsweredPhrases);
            currentlySelectedPhraseIndex = answeredPhrases.size() - 1;
            nextQuestion();
        }
    }

    public void setTable() {
        System.out.println("CALL: setTable() from InterfaceBean");

        if (choosedLabel != null && !choosedLabel.equalsIgnoreCase("")){
            if(choosedLabel.equalsIgnoreCase("all")){
                choosedLabelsForLearningWords.clear();
            }else{
                choosedLabelsForLearningWords.add(choosedLabel);
            }
        }
        resultChoosedLabel = "";

        boolean firstLoop = true;
        for(String currentLabel : choosedLabelsForLearningWords){   //Makes a "WHERE LABEL IN" clause
            if(firstLoop){
                resultChoosedLabel += "'" + currentLabel + "'";
                firstLoop = false;
            }else {
                resultChoosedLabel += ",'" + currentLabel + "'";
            }
        }

        if(!resultChoosedLabel.equals(previousResultChoosedLabel)){ //If clause was changed
            dao.chosedLabels = choosedLabelsForLearningWords;
            dao.reloadPhrasesCollection();
            previousResultChoosedLabel = resultChoosedLabel;
            reloadStatTableData();
        }
    }



     private void reloadStatTableData(){
        System.out.println("CALL: reloadStatTableData() from InterfaceBean");

        //After the answerField creates String like this - "40.2 ➩ 37.3"
        if(!selectedPhrase.hasBeenAnswered){
            BigDecimal previous = selectedPhrase.probabilityFactor.setScale(1, RoundingMode.HALF_UP);
            currPhrProb = previous.toString();
        }else{
            BigDecimal previous = selectedPhrase.previousProbabilityFactor.setScale(1, RoundingMode.HALF_UP);
            BigDecimal present = selectedPhrase.probabilityFactor.setScale(1, RoundingMode.HALF_UP);
            currPhrProb = previous + "➩" + present + "(" + present.subtract(previous) + ")";
        }

        //After the answerField creates String like this - "0.06116% ➩ 0.07294%"
         currPhrPercentOfAppearance = /*selectedPhrase.getPercentChanceView();*/ "NOT YET IMPLEMENTED";


        if(selectedPhrase.lastAccessDateTime != null){

            currPhrAbsLastAccsDate = LocalDateTime.ofInstant(selectedPhrase.lastAccessDateTime.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelLastAccsDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedPhrase.lastAccessDateTime.toEpochSecond());
        }

        if(selectedPhrase.collectionAddingDateTime != null){

            currPhrAbsCreateDate = LocalDateTime.ofInstant(selectedPhrase.collectionAddingDateTime.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedPhrase.collectionAddingDateTime.toEpochSecond());
        }

        currPhrLabel = selectedPhrase.label;

        //>>Calculate session statistics
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        int numOfPhrForSession = answeredPhrases.size();
        currPhrId = selectedPhrase.id;
         if(!selectedPhrase.hasBeenAnswered){
            currentPhraseRate = new BigDecimal(selectedPhrase.multiplier).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
         }else {
             currentPhraseRate = (new BigDecimal(selectedPhrase.previousMultiplier).setScale(2, BigDecimal.ROUND_HALF_UP) + " ➩ " + new BigDecimal(selectedPhrase.multiplier).setScale(2, BigDecimal.ROUND_HALF_UP));
         }

        for(Phrase phrs : answeredPhrases){
            if(!phrs.hasBeenAnswered)
                numOfNonAnswForSession++;
            else if(phrs.hasBeenAnsweredCorrectly)
                numOfRightAnswForSession++;
        }

        answersForSessionNumber = numOfPhrForSession - numOfNonAnswForSession;
        //Generates a string with the percentage of correct answers to the total number of answers
        rightAnswersPercentage = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(answersForSessionNumber ==0?1: answersForSessionNumber),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        trainedPhrasesNumber = (int) dao.learntWords;
        nonLearnedWordsNumber = (int) dao.nonLearnedWords;
        totalPhrasesNumber = trainedPhrasesNumber + nonLearnedWordsNumber;

         try{
             averageAnswersPerDayNumber = (int) ( (float) (dao.answUntil6amAmount + answersForSessionNumber) / (float) (dao.totalHoursUntil6am + ZonedDateTime.now(ZoneId.of("Europe/Kiev")).getHour() - 6) * 24);
         }catch (ArithmeticException e){
             averageAnswersPerDayNumber = 0;
         }
        //<<

    }

    private void reloadTrainingLog(){

        System.out.println("CALL: reloadTrainingLog() from InterfaceBean");

        trainingLog = new StringBuilder();
        trainedPhrasesPerSessionNumber = 0;

        for (int i = answeredPhrases.size() - 1; i >= 0; i--) {
            Phrase currentPhrase = answeredPhrases.get(i);

            if (currentPhrase.isTrained() && !currentPhrase.wasTrainedBeforeAnswer()) {
                trainedPhrasesPerSessionNumber++;
            } else if (!currentPhrase.isTrained() && currentPhrase.wasTrainedBeforeAnswer()) {
                trainedPhrasesPerSessionNumber--;
            }

            if (!currentPhrase.hasBeenAnswered){
                trainingLog.append(i == currentlySelectedPhraseIndex ? "<strong>" : "")
                        .append("[").append(currentPhrase.phraseAppearingTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append(NON_ANSWERED_MESSAGE).append("] ")
                        .append(currentPhrase.isTrained() ? "<font color=\"green\">" : "")
                        .append(currentPhrase.nativeWord).append(currentPhrase.isTrained() ? "</font>" : "")
                        .append((i == currentlySelectedPhraseIndex ? "</strong>" : "")).append("</br>");
            } else if (currentPhrase.hasBeenAnsweredCorrectly){
                trainingLog.append(i == currentlySelectedPhraseIndex ? "<strong>" : "").append("[").append(currentPhrase.phraseAppearingTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(RIGHT_MESSAGE).append("] ").append(currentPhrase.isTrained() ? "<font color=\"green\">" : "")
                        .append(currentPhrase.nativeWord).append(" - ")
                        .append(currentPhrase.getForWordAndTranscription())
                        .append(currentPhrase.isTrained() ? "</font>" : "").append((i == currentlySelectedPhraseIndex ? "</strong>" : "")).append("</br>");
            } else {
                trainingLog.append(i == currentlySelectedPhraseIndex ? "<strong>" : "").append("[").append(currentPhrase.phraseAppearingTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .append(WRONG_MESSAGE).append("] ").append(currentPhrase.isTrained() ? "<font color=\"green\">" : "")
                        .append(currentPhrase.nativeWord).append(" - ")
                        .append(currentPhrase.getForWordAndTranscription())
                        .append(currentPhrase.isTrained() ? "</font>" : "").append((i == currentlySelectedPhraseIndex ? "</strong>" : "")).append("</br>");
            }
        }

        currPhrForWord = selectedPhrase.foreignWord;
        currPhrNatWord = selectedPhrase.nativeWord;
        currPhrTransc = selectedPhrase.transcription;
        currPhrLabel = selectedPhrase.label;
//        currentPhraseRate = selectedPhrase.multiplier;
        if (selectedPhrase.isModified){
            selectedPhrase.updatePhraseInDb();
        }

    }


    public void rightAnswer(){

        System.out.println("CALL: rightAnswer() from InterfaceBean");
        selectedPhrase = answeredPhrases.get(currentlySelectedPhraseIndex);
        selectedPhrase.rightAnswer();
        nextQuestion();
        reloadStatTableData();
        reloadTrainingLog();

    }

    public void wrongAnswer(){

        System.out.println("CALL: wrongAnswer() from InterfaceBean");
        selectedPhrase = answeredPhrases.get(currentlySelectedPhraseIndex);
        selectedPhrase.wrongAnswer();
        nextQuestion();
        reloadStatTableData();
        reloadTrainingLog();
    }

    public void previousRight(){

        System.out.println("CALL: previousRight() from InterfaceBean");
        // previousRight() method is not alowed in the middle of the phrases list and at first question per session
        if(currentlySelectedPhraseIndex == answeredPhrases.size() - 1 && answeredPhrases.size() - todayAnsweredPhrases.size() > 1){
            answeredPhrases.get(currentlySelectedPhraseIndex -1).rightAnswer();
            reloadStatTableData();
            reloadTrainingLog();
        }
    }

    public void previousWrong(){

        System.out.println("CALL: previousWrong() from InterfaceBean");
        // previousWrong() method is not alowed in the middle of the phrases list and at first question per session
        if(currentlySelectedPhraseIndex == answeredPhrases.size() - 1 && answeredPhrases.size() - todayAnsweredPhrases.size() > 1) {
            answeredPhrases.get(currentlySelectedPhraseIndex - 1).wrongAnswer();
            reloadStatTableData();
            reloadTrainingLog();
        }
    }

    public void checkTheAnswer(){

        System.out.println("CALL: checkTheAnswer() from InterfaceBean");

        if(answerField == null) {
            return;
        }

        if (answerField.equals("+")){
            rightAnswer();
        }else if (answerField.equals("-")){
            wrongAnswer();
        }else if (answerField.equals("++")){
            previousRight();
        }else if (answerField.equals("--")){
            previousWrong();
        }else {
            Answer givenAnswer = Answer.compose(selectedPhrase, answerField);
            if(givenAnswer.isCorrect()){
                rightAnswer();
            } else {
                wrongAnswer();
            }
        }
        answerField = "";
    }


    public void nextQuestion(){
        System.out.print("\nCALL: nextQuestion() from InterfaceBean");
        if(currentlySelectedPhraseIndex == answeredPhrases.size() - 1) {
            Phrase newPhrase = new Phrase(dao.obtainRandomPhrase());
            answeredPhrases.add(newPhrase);
            selectedPhrase = newPhrase;
            currentlySelectedPhraseIndex = answeredPhrases.size() - 1;
            selectedPhrase = answeredPhrases.get(currentlySelectedPhraseIndex);
            question = selectedPhrase.nativeWord + " " + hint.shortHint(selectedPhrase.foreignWord);
        }else {
            currentlySelectedPhraseIndex++;
            selectedPhrase = answeredPhrases.get(currentlySelectedPhraseIndex);
            question = selectedPhrase.nativeWord + " " + hint.shortHint(selectedPhrase.foreignWord);
        }
        System.out.println(" " + question);
        reloadStatTableData();
        reloadTrainingLog();

    }

    public void previousQuestion() {
        System.out.println("CALL: previousQuestion() from InterfaceBean");

        //Prevents selecting today answered phrases and negative index
        if(currentlySelectedPhraseIndex > todayAnsweredPhrases.size()){
            selectedPhrase = answeredPhrases.get(--currentlySelectedPhraseIndex);
            question = selectedPhrase.nativeWord;
            reloadStatTableData();
            reloadTrainingLog();
        }
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

    public DAO getDao() {
        return dao;
    }
    public void setDao(DAO dao) {
        this.dao = dao;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

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

    public StringBuilder getTrainingLog() {
        return trainingLog;
    }
    public void setTrainingLog(StringBuilder res) {
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

    /*public BigDecimal getCurrentPhraseLastAccessDate() {
        return currentPhraseLastAccessDate;
    }
    public void setCurrentPhraseLastAccessDate(BigDecimal currentPhraseLastAccessDate) {
        this.currentPhraseLastAccessDate = currentPhraseLastAccessDate;
    }*/

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

    public ArrayList<String> getAvailableLabels() {
        return availableLabels;
    }
    public void setAvailableLabels(ArrayList<String> availableLabels) {
        this.availableLabels = availableLabels;
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

    public String getCurrentPhraseRate() {
        return currentPhraseRate;
    }
    public void setCurrentPhraseRate(String currentPhraseRate) {
        this.currentPhraseRate = currentPhraseRate;
    }
}




