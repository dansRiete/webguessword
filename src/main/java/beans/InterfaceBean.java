package beans;

import datamodel.Question;
import dao.DatabaseHelper;
import utils.TrainingLog;
import utils.TimeDifference;

import javax.el.ELContext;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
    //<<

    private String currPhrPercentOfAppearance;
    private String currPhrasesProbabilityFactor;
    private String currentPhraseLastAccesssDate;
    private String currPhrAbsCreateDate;
    private String currPhrRelLastAccsDate;
    private String currPhrRelCreateDate;
    private long currPhrId;
    private String currentPhraseRate;
    private DatabaseHelper databaseHelper;
    private String questionField ="";
    private String answerField = "";
    private TrainingLog trainingLog;
    private List<String> availableLabels;
    private String choosedLabel;
    private String resultChosenLabel;
    private String previousResultChoosedLabel = "";
    private HashSet<String> chosenLabelsForLearningWords = new HashSet<>();


    public InterfaceBean(){

        System.out.println("CALL: InterfaceBean constructor");
        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        this.loginBean = (LoginBean) elContext.getELResolver().getValue(elContext, null, "login");

        if(loginBean != null){
            this.databaseHelper = loginBean.getDatabaseHelper();
        }else {
            throw new RuntimeException("loginBean in InterfaceBean was null");
        }

        if(databaseHelper != null){
            this.trainingLog = new TrainingLog(databaseHelper);
            this.availableLabels = databaseHelper.getAvailableLabels();
            List<Question> todayQuestions = databaseHelper.loadTodayAnsweredQuestions();
            trainingLog.setTodayQuestions(todayQuestions);
            nextButtonAction();
        }else {
            throw new RuntimeException("DatabaseHelper was null");
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
        this.resultChosenLabel = "";

        boolean firstLoop = true;
        for(String currentLabel : chosenLabelsForLearningWords){   //Makes a "WHERE LABEL IN" clause
            if(firstLoop){
                this.resultChosenLabel += "'" + currentLabel + "'";
                firstLoop = false;
            }else {
                this.resultChosenLabel += ",'" + currentLabel + "'";
            }
        }

        if(!resultChosenLabel.equals(previousResultChoosedLabel)){ //If clause was changed
            databaseHelper.setSelectedLabels(chosenLabelsForLearningWords);
            databaseHelper.reloadPhrasesAndIndices();
            this.previousResultChoosedLabel = resultChosenLabel;
        }
    }

    public void answerButtonAction(){

        System.out.println("CALL: answerButtonAction() from InterfaceBean");

        if(answerField == null) {
            return;
        }else if (answerField.equals("+")){
            iKnowItButtonAction();
        }else if (answerField.equals("-")){
            iDontKnowItButtonAction();
        }else if (answerField.equals("++")){
            previousRightButtonAction();
        }else if (answerField.equals("--")){
            previousWrongButtonAction();
        }else if (answerField.equals("")){
            nextButtonAction();
        }else {
            trainingLog.retrieveSelected().answerTheQuestion(answerField);
            nextButtonAction();
        }
        this.answerField = "";
    }

    public void nextButtonAction(){
        System.out.println();
        System.out.println("CALL: nextButtonAction() from InterfaceBean");
        trainingLog.nextQuestion();
        Question question = trainingLog.retrieveSelected();
        if(question != null){
            this.questionField = question.string();
        }else {
            this.questionField = "";
        }
    }

    public void previousButtonAction() {
        System.out.println("CALL: previousButtonAction() from InterfaceBean");
        trainingLog.selectPrevious();
        this.questionField = trainingLog.retrieveSelected().string();
    }

    public void iKnowItButtonAction(){
        System.out.println("CALL: iKnowItButtonAction() from InterfaceBean");
        Question question = trainingLog.retrieveSelected();
        if(question != null) {
            question.rightAnswer();
        }
        nextButtonAction();
    }

    public void iDontKnowItButtonAction(){
        System.out.println("CALL: iDontKnowItButtonAction() from InterfaceBean");
        Question question = trainingLog.retrieveSelected();
        if(question != null){
            question.wrongAnswer();
        }
        nextButtonAction();
    }

    public void previousRightButtonAction(){
        System.out.println("CALL: previousRightButtonAction() from InterfaceBean");
        Question question = trainingLog.retrievePrevious();
        if(question != null){
            question.rightAnswer();
        }
        this.trainingLog.reload();
    }

    public void previousWrongButtonAction(){

        System.out.println("CALL: previousWrongButtonAction() from InterfaceBean");
        Question question = trainingLog.retrievePrevious();
        if(question != null){
            question.wrongAnswer();
        }
        trainingLog.reload();
    }

    public void deleteButtonAction(){
        System.out.println("CALL: delete() from InterfaceBean");
        trainingLog.deleteSelectedPhrase();
    }

    public void exitButtonAction(){
        System.out.println("CALL: exitButtonAction() from InterfaceBean");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpSession session = request.getSession();
//        loginBean.getSessionFactory().close();
        session.invalidate();
        try {
            context.getExternalContext().redirect("index.xhtml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //>>>>>>>>>>>>    Setters and getters     >>>>>>>>>>>>>

    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }
    public void setDatabaseHelper(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public String getQuestionField() {
        return questionField;
    }
    public void setQuestionField(String questionField) {
        this.questionField = questionField;
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
//    public void setTrainingLog(utils.TrainingLog res) {
//        this.trainingLog = res;
//    }

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

    public List<String> getAvailableLabels() {
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




