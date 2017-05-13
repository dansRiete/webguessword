package beans;

import utils.PhrasesRepository;
import datamodel.Question;
import utils.TrainingLog;

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
public class InterfaceBean implements Serializable {

    @ManagedProperty(value = "#{login}")
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
    private String currentPhraseLastAccessDate;
    private String currPhrAbsCreateDate;
    private String currentPhraseLastAccessDateTimeDifference;
    private String currentPhraseCreateDateTimeDifference;
    private long currentPhraseId;
    private String currentPhraseRate;
    private PhrasesRepository phrasesRepository;
    private String questionField = "";
    private String answerField = "";
    private TrainingLog trainingLog;
    private List<String> availableLabels;
    private String chosenLabel;
    private String resultChosenLabel;
    private String previousResultChosenLabel = "";
    private HashSet<String> chosenLabelsForLearningWords = new HashSet<>();


    public InterfaceBean() {

        System.out.println("CALL: InterfaceBean constructor");
        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        this.loginBean = (LoginBean) elContext.getELResolver().getValue(elContext, null, "login");
        this.phrasesRepository = loginBean.getPhrasesRepository();
        this.trainingLog = new TrainingLog(phrasesRepository);
        this.availableLabels = phrasesRepository.getAvailableLabels();
        List<Question> todayQuestions = phrasesRepository.retrieveTodayAnsweredQuestions();
        trainingLog.setTodayQuestions(todayQuestions);
        nextButtonAction();
    }

    public void reloadLabelsList() {
        System.out.println("CALL: setChosenLabels() from InterfaceBean");

        if (chosenLabel != null && !chosenLabel.equalsIgnoreCase("")) {
            if (chosenLabel.equalsIgnoreCase("all")) {
                chosenLabelsForLearningWords.clear();
            } else {
                chosenLabelsForLearningWords.add(chosenLabel);
            }
        }
        this.resultChosenLabel = "";

        boolean firstLoop = true;
        for (String currentLabel : chosenLabelsForLearningWords) {   //Makes a "WHERE LABEL IN" clause
            if (firstLoop) {
                this.resultChosenLabel += "'" + currentLabel + "'";
                firstLoop = false;
            } else {
                this.resultChosenLabel += ",'" + currentLabel + "'";
            }
        }

        if (!resultChosenLabel.equals(previousResultChosenLabel)) { //If clause was changed
            phrasesRepository.setSelectedLabels(chosenLabelsForLearningWords);
            phrasesRepository.reloadIndices();
            this.previousResultChosenLabel = resultChosenLabel;
        }
    }

    public void answerButtonAction() {

        System.out.println("CALL: answerButtonAction() from InterfaceBean");

        if (answerField == null) {
            return;
        } else if (answerField.equals("+")) {
            iKnowItButtonAction();
        } else if (answerField.equals("-")) {
            iDoNotKnowItButtonAction();
        } else if (answerField.equals("++")) {
            previousRightButtonAction();
        } else if (answerField.equals("--")) {
            previousWrongButtonAction();
        } else if (answerField.equals("")) {
            nextButtonAction();
        } else {
            trainingLog.retrieveSelected().answer(answerField);
            nextButtonAction();
        }
        this.answerField = "";
    }

    public void nextButtonAction() {
        System.out.println();
        System.out.println("CALL: nextButtonAction() from InterfaceBean");
        trainingLog.nextQuestion();
        Question question = trainingLog.retrieveSelected();
        if (question != null) {
            this.questionField = question.string();
        } else {
            this.questionField = "";
        }
    }

    public void previousButtonAction() {
        System.out.println("CALL: previousButtonAction() from InterfaceBean");
        trainingLog.selectPrevious();
        this.questionField = trainingLog.retrieveSelected().string();
    }

    public void iKnowItButtonAction() {
        System.out.println("CALL: iKnowItButtonAction() from InterfaceBean");
        Question question = trainingLog.retrieveSelected();
        if (question != null) {
            question.rightAnswer();
        }
        nextButtonAction();
    }

    public void iDoNotKnowItButtonAction() {
        System.out.println("CALL: iDoNotKnowItButtonAction() from InterfaceBean");
        Question question = trainingLog.retrieveSelected();
        if (question != null) {
            question.wrongAnswer();
        }
        nextButtonAction();
    }

    public void previousRightButtonAction() {
        System.out.println("CALL: previousRightButtonAction() from InterfaceBean");
        Question question = trainingLog.retrievePrevious();
        if (question != null) {
            question.rightAnswer();
        }
        this.trainingLog.reload();
    }

    public void previousWrongButtonAction() {

        System.out.println("CALL: previousWrongButtonAction() from InterfaceBean");
        Question question = trainingLog.retrievePrevious();
        if (question != null) {
            question.wrongAnswer();
        }
        trainingLog.reload();
    }

    public void deleteButtonAction() {
        System.out.println("CALL: delete() from InterfaceBean");
        trainingLog.deleteSelectedPhrase();
    }

    public void exitButtonAction() {
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

    public PhrasesRepository getPhrasesRepository() {
        return phrasesRepository;
    }

    public void setPhrasesRepository(PhrasesRepository phrasesRepository) {
        this.phrasesRepository = phrasesRepository;
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

    public String getCurrentPhraseLastAccessDate() {
        return currentPhraseLastAccessDate;
    }

    public void setCurrentPhraseLastAccessDate(String pdLastAccs) {
        this.currentPhraseLastAccessDate = pdLastAccs;
    }

    public String getCurrPhrAbsCreateDate() {
        return currPhrAbsCreateDate;
    }

    public void setCurrPhrAbsCreateDate(String strCreateDate) {
        this.currPhrAbsCreateDate = strCreateDate;
    }

    public String getChosenLabel() {
        return chosenLabel;
    }

    public void setChosenLabel(String chosenLabel) {
        this.chosenLabel = chosenLabel;
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

    public long getId() {
        return currentPhraseId;
    }

    public void setId(long id) {
        this.currentPhraseId = id;
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

    public String getCurrentPhraseLastAccessDateTimeDifference() {
        return currentPhraseLastAccessDateTimeDifference;
    }

    public void setCurrentPhraseLastAccessDateTimeDifference(String currentPhraseLastAccessDateTimeDifference) {
        this.currentPhraseLastAccessDateTimeDifference = currentPhraseLastAccessDateTimeDifference;
    }

    public String getCurrentPhraseCreateDateTimeDifference() {
        return currentPhraseCreateDateTimeDifference;
    }

    public void setCurrentPhraseCreateDateTimeDifference(String currentPhraseCreateDateTimeDifference) {
        this.currentPhraseCreateDateTimeDifference = currentPhraseCreateDateTimeDifference;
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




