package beans;

import datamodel.Phrase;
import datamodel.Question;
import logic.DAO;
import logic.Hints;
import logic.RetDiff;
import logic.TrainingLog;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
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
    private int nonTrainedPhrasesNumber;
    private int averageAnswersPerDay;
    private String trainingCompletionPercent;
    private int totalPhrasesNumber;
    private int trainedPhrasesPerSessionNumber;
    private String rightAnswersPercentage;
    private BigDecimal currentPhraseLastAccessDate;
    //<<

    //>>Current phrase data
//    private String currPhrNatWord;
//    private String currPhrForWord;
//    private String currPhrTransc;
//    private String currPhrLabel;
    private String currPhrPercentOfAppearance;
    private String currPhrasesProbabilityFactor;
    private String currentPhraseLastAccesssDate;
    private String currPhrAbsCreateDate;
    private String currPhrRelLastAccsDate;
    private String currPhrRelCreateDate;
    private long currPhrId;
    private String currentPhraseRate;
    //<<

    private RetDiff retDiff = new RetDiff();
    private DAO dao;

    public Question getSelectedQuestion() {
        return selectedQuestion;
    }

    public Question selectedQuestion;
    private String question ="";
    private String answerField = "";
    private TrainingLog trainingLog = new TrainingLog();
    private final static String WRONG_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#ff0000\">wrong</font></strong>";
    private final static String RIGHT_MESSAGE = " <strong><font color=\"green\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
    private final static String NON_ANSWERED_MESSAGE = " <strong><font color=\"#BBBBB9\">right</font>/<font color=\"#BBBBB9\">wrong</font></strong>";
//    private ArrayList<Phrase> askedPhrasesLog = new ArrayList<>();
//    private ArrayList<Phrase> todayAskedPhrases = new ArrayList<>();
    private ArrayList<String> availableLabels;
    private String choosedLabel;
    private String resultChosenLabel;
    private String previousResultChoosedLabel = "";
    private HashSet<String> chosenLabelsForLearningWords = new HashSet<>();
    private int currentlySelectedPhraseIndex;
    private Hints hint = new Hints();


    public InterfaceBean(){
        System.out.println("CALL: InterfaceBean constructor");
        init();
    }

    @PostConstruct
    private void init(){
        System.out.println("CALL: init() from InterfaceBean");
        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        loginBean = (LoginBean) elContext.getELResolver().getValue(elContext, null, "login");
        if(loginBean != null){
            dao = loginBean.getDao();
        }else {
            throw new RuntimeException("loginBean in InterfaceBean was null");
        }
        if(dao != null){
            availableLabels = dao.getAllAvailableLabels();
            todayAskedPhrases = dao.retrieveTodayAnsweredPhrases();
            askedPhrasesLog.addAll(todayAskedPhrases);
            currentlySelectedPhraseIndex = askedPhrasesLog.size() - 1;
            nextQuestion();
        }else {
            throw new RuntimeException("DAO was null");
        }
    }

    public void reloadLabelsList() {
        System.out.println("CALL: setChosenLabels() from InterfaceBean");

        if (choosedLabel != null && !choosedLabel.equalsIgnoreCase("")){
            if(choosedLabel.equalsIgnoreCase("all")){
                chosenLabelsForLearningWords.clear();
            }else{
                chosenLabelsForLearningWords.add(choosedLabel);
            }
        }
        resultChosenLabel = "";

        boolean firstLoop = true;
        for(String currentLabel : chosenLabelsForLearningWords){   //Makes a "WHERE LABEL IN" clause
            if(firstLoop){
                resultChosenLabel += "'" + currentLabel + "'";
                firstLoop = false;
            }else {
                resultChosenLabel += ",'" + currentLabel + "'";
            }
        }

        if(!resultChosenLabel.equals(previousResultChoosedLabel)){ //If clause was changed
            dao.setActiveLabels(chosenLabelsForLearningWords);
            dao.reloadPhrasesCollection();
            previousResultChoosedLabel = resultChosenLabel;
            reloadStatisticsTable();
        }
    }

     private void reloadStatisticsTable(){
        System.out.println("CALL: reloadStatisticsTable() from InterfaceBean");

        //After the answerField creates String like this - "40.2 ➩ 37.3"
        if(!selectedQuestion.hasBeenAnswered){
            BigDecimal previous = new BigDecimal(selectedQuestion.probabilityFactor).setScale(1, RoundingMode.HALF_UP);
            currPhrasesProbabilityFactor = previous.toString();
        }else{
            BigDecimal previous = new BigDecimal(selectedQuestion.previousProbabilityFactor).setScale(1, RoundingMode.HALF_UP);
            BigDecimal present = new BigDecimal(selectedQuestion.probabilityFactor).setScale(1, RoundingMode.HALF_UP);
            currPhrasesProbabilityFactor = previous + "➩" + present + "(" + present.subtract(previous) + ")";
        }

        //After the answerField creates String like this - "0.06116% ➩ 0.07294%"
         currPhrPercentOfAppearance = /*selectedQuestion.getPercentChanceView();*/ "NOT YET IMPLEMENTED";


        if(selectedQuestion.lastAccessDateTime != null){

            currentPhraseLastAccesssDate = selectedQuestion.lastAccessDateTime.format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelLastAccsDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedQuestion.lastAccessDateTime.toEpochSecond());
        }

        if(selectedQuestion.collectionAddingDateTime != null){

            currPhrAbsCreateDate = LocalDateTime.ofInstant(selectedQuestion.collectionAddingDateTime.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedQuestion.collectionAddingDateTime.toEpochSecond());
        }

//        currPhrLabel = selectedQuestion.label;

        //>>Calculate session statistics
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        int numOfPhrForSession = askedPhrasesLog.size();
        currPhrId = selectedQuestion.id;
         if(!selectedQuestion.hasBeenAnswered){
            currentPhraseRate = new BigDecimal(selectedQuestion.multiplier).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
         }else {
             currentPhraseRate = (new BigDecimal(selectedQuestion.previousMultiplier).setScale(2, BigDecimal.ROUND_HALF_UP) + " ➩ " + new BigDecimal(selectedQuestion.multiplier).setScale(2, BigDecimal.ROUND_HALF_UP));
         }

        for(Phrase phrs : askedPhrasesLog){
            if(!phrs.hasBeenAnswered)
                numOfNonAnswForSession++;
            else if(phrs.hasBeenAnsweredCorrectly)
                numOfRightAnswForSession++;
        }

        answersForSessionNumber = numOfPhrForSession - numOfNonAnswForSession;
        //Generates a string with the percentage of correct answers to the total number of answers
        rightAnswersPercentage = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(answersForSessionNumber ==0?1: answersForSessionNumber),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        trainedPhrasesNumber = dao.getLearntWordsAmount();
        nonTrainedPhrasesNumber = dao.getNonLearntWordsAmount();
        totalPhrasesNumber = trainedPhrasesNumber + nonTrainedPhrasesNumber;

         try{
             averageAnswersPerDay = (int) ((float) (dao.getTotalTrainingAnswers() + answersForSessionNumber) / (float) (dao.getTotalTrainingHoursSpent() + ZonedDateTime.now(ZoneId.of("Europe/Kiev")).getHour() - 6) * 24);
         }catch (ArithmeticException e){
             averageAnswersPerDay = 0;
         }
        //<<

    }

    private void reloadTrainingLog(){

        System.out.println("CALL: reloadTrainingLog() from InterfaceBean");

        trainingLog = new StringBuilder();
        trainedPhrasesPerSessionNumber = 0;

        for (int i = askedPhrasesLog.size() - 1; i >= 0; i--) {
            Phrase currentPhrase = askedPhrasesLog.get(i);

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

//        currPhrForWord = selectedQuestion.foreignWord;
//        currPhrNatWord = selectedQuestion.nativeWord;
//        currPhrTransc = selectedQuestion.transcription;
//        currPhrLabel = selectedQuestion.label;
        if (selectedQuestion.isModified){
            selectedQuestion.update();
        }

    }


    public void rightAnswer(){

        System.out.println("CALL: rightAnswer() from InterfaceBean");
        selectedQuestion = askedPhrasesLog.get(currentlySelectedPhraseIndex);
        selectedQuestion.rightAnswer();
        nextQuestion();
        reloadStatisticsTable();
        reloadTrainingLog();

    }

    public void wrongAnswer(){

        System.out.println("CALL: wrongAnswer() from InterfaceBean");
        selectedQuestion = askedPhrasesLog.get(currentlySelectedPhraseIndex);
        selectedQuestion.wrongAnswer();
        nextQuestion();
        reloadStatisticsTable();
        reloadTrainingLog();
    }

    public void previousQuestionRight(){

        System.out.println("CALL: previousQuestionRight() from InterfaceBean");
        // previousQuestionRight() method is not alowed in the middle of the phrases list and at first question per session
        if(currentlySelectedPhraseIndex == askedPhrasesLog.size() - 1 && askedPhrasesLog.size() - todayAskedPhrases.size() > 1){
            askedPhrasesLog.get(currentlySelectedPhraseIndex - 1).rightAnswer();
            reloadStatisticsTable();
            reloadTrainingLog();
        }
    }

    public void previousQuestionWrong(){

        System.out.println("CALL: previousQuestionWrong() from InterfaceBean");
        // previousQuestionWrong() method is not alowed in the middle of the phrases list and at first question per session
        if(currentlySelectedPhraseIndex == askedPhrasesLog.size() - 1 && askedPhrasesLog.size() - todayAskedPhrases.size() > 1) {
            askedPhrasesLog.get(currentlySelectedPhraseIndex - 1).wrongAnswer();
            reloadStatisticsTable();
            reloadTrainingLog();
        }
    }

    public void answerTheQuestion(){

        System.out.println("CALL: answerTheQuestion() from InterfaceBean");

        if(answerField == null) {
            return;
        }else if (answerField.equals("+")){
            rightAnswer();
        }else if (answerField.equals("-")){
            wrongAnswer();
        }else if (answerField.equals("++")){
            previousQuestionRight();
        }else if (answerField.equals("--")){
            previousQuestionWrong();
        }else if (answerField.equals("*")){
            nextQuestion();
        }else {
            Question givenQuestion = Question.compose(selectedQuestion);
            if(givenQuestion.answerIsCorrect()){
                rightAnswer();
            } else {
                wrongAnswer();
            }
        }
        answerField = "";
    }


    public void nextQuestion(){
        System.out.println();
        System.out.println("CALL: nextQuestion() from InterfaceBean");
        if(currentlySelectedPhraseIndex == askedPhrasesLog.size() - 1) {
            selectedQuestion = new Phrase(dao.retrieveRandomPhrase());
            askedPhrasesLog.add(selectedQuestion);
            currentlySelectedPhraseIndex = askedPhrasesLog.size() - 1;
            question = selectedQuestion.nativeWord + " " + hint.shortHint(selectedQuestion.foreignWord);
        }else {
            currentlySelectedPhraseIndex++;
            selectedQuestion = askedPhrasesLog.get(currentlySelectedPhraseIndex);
            question = selectedQuestion.nativeWord + " " + hint.shortHint(selectedQuestion.foreignWord);
        }
        reloadStatisticsTable();
        reloadTrainingLog();
    }

    public void previousQuestion() {
        System.out.println("CALL: previousQuestion() from InterfaceBean");

        //Prevents selecting today answered phrases and negative index
        if(currentlySelectedPhraseIndex > todayAskedPhrases.size()){
            selectedQuestion = askedPhrasesLog.get(--currentlySelectedPhraseIndex);
            question = selectedQuestion.nativeWord;
            reloadStatisticsTable();
            reloadTrainingLog();
        }
    }

    public void deletePhrase(){
        System.out.println("CALL: delete() from InterfaceBean");
        selectedQuestion.delete();
    }

    public void exitSession(){
        System.out.println("CALL: exitSession() from InterfaceBean");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpSession session = request.getSession();
        session.invalidate();
        try {
            context.getExternalContext().redirect("index.xhtml");
        } catch (IOException e) {
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

    public TrainingLog getTrainingLog() {
        return trainingLog;
    }
    public void setTrainingLog(TrainingLog res) {
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

    public String getCurrPhrasesProbabilityFactor() {
        return currPhrasesProbabilityFactor;
    }
    public void setCurrPhrasesProbabilityFactor(String pDprob) {
        this.currPhrasesProbabilityFactor = pDprob;
    }

    public String getCurrentPhraseLastAccesssDate() {
        return currentPhraseLastAccesssDate;
    }
    public void setCurrentPhraseLastAccesssDate(String pdLastAccs) {
        this.currentPhraseLastAccesssDate = pdLastAccs;
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

    public String getResultChosenLabel() {
        return resultChosenLabel;
    }
    public void setResultChosenLabel(String resultChosenLabel) {
        this.resultChosenLabel = resultChosenLabel;
    }

    /*public String getCurrPhrNatWord() {
        return currPhrNatWord;
    }
    public void setCurrPhrNatWord(String currPhrNatWord) {
        this.currPhrNatWord = currPhrNatWord;
        selectedQuestion.nativeWord = this.currPhrNatWord;
        selectedQuestion.isModified = true;
    }

    public String getCurrPhrForWord() {
        return currPhrForWord;
    }
    public void setCurrPhrForWord(String currPhrForWord) {

        this.currPhrForWord = currPhrForWord;
        selectedQuestion.foreignWord = this.currPhrForWord;
        selectedQuestion.isModified = true;
        System.out.println("--- inside setCurrPhrForWord(String currPhrForWord) currPhrForWord is " + this.currPhrForWord);
    }

    public String getCurrPhrTransc() {
        return currPhrTransc;
    }
    public void setCurrPhrTransc(String currPhrTransc) {
        this.currPhrTransc = currPhrTransc;
        selectedQuestion.transcription = this.currPhrTransc;
        selectedQuestion.isModified = true;
    }

    public String getCurrPhrLabel() {
        return currPhrLabel;
    }
    public void setCurrPhrLabel(String currPhrLabel) {
        this.currPhrLabel = currPhrLabel;
        selectedQuestion.label = this.currPhrLabel;
        selectedQuestion.isModified = true;
    }*/

    public long getId() {
        return currPhrId;
    }
    public void setId(long id) {
        this.currPhrId = id;
    }

    public int getTrainedPhrasesNumber() {
        return trainedPhrasesNumber;
    }
    public void setTrainedPhrasesNumber(int trainedPhrasesNumber) {
        this.trainedPhrasesNumber = trainedPhrasesNumber;
    }

    public int getNonTrainedPhrasesNumber() {
        return nonTrainedPhrasesNumber;
    }
    public void setNonTrainedPhrasesNumber(int nonTrainedPhrasesNumber) {
        this.nonTrainedPhrasesNumber = nonTrainedPhrasesNumber;
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

    public int getAverageAnswersPerDay() {
        return averageAnswersPerDay;
    }
    public void setAverageAnswersPerDay(int averageAnswersPerDay) {
        this.averageAnswersPerDay = averageAnswersPerDay;
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




