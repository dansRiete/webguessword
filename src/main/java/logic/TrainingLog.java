package logic;

import datamodel.Phrase;
import datamodel.Question;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Aleks on 27.03.2017.
 */
public class TrainingLog {

    private int position;
    private int todayTrainedPhrasesNumber;
    private int totalTrainedPhrasesNumber;
    private int activeTrainedPhrasesNumber;
    private int totalUntrainedPhrasesNumber;
    private int activeUntrainedPhrasesNumber;
    private String todayRightAnswersPercentage;
    private String totalAndActivePhrasesNumber;
    private String totalAndActiveUntrainedPhrasesNumber;
    private String totalAndActiveTrainedPhrasesNumber;
    private String trainingCompletionPercentage;
    private BigDecimal averageAnswersPerDayNumber;
    private List<Question> allQuestions = new LinkedList<>();
    private StringBuilder log = new StringBuilder();
    private DatabaseHelper databaseHelper;
    private class TrainingLogQuestionLine {

        private final static String RIGHT_MESSAGE_COLOR = "green";
        private final static String WRONG_MESSAGE_COLOR = "#FF0000";
        private final static String NON_ANSWERED_MESSAGE_COLOR = "#BBBBB9";
        private final static String WRONG_MESSAGE = "wrong";
        private final static String RIGHT_MESSAGE = "right";

        public String getResultString() {
            return resultString;
        }

        private final String resultString;

        public TrainingLogQuestionLine(Question question){

            String timeAndRightWrongMessage = "[" + formatTime(question.getAskDate()) + " " + makeRightWrongMsg(question) + "] ";
            String phrase = question.getAskedPhrase().getNativeWord();
            String result;
            Phrase askedPhrase = question.getAskedPhrase();

            if (question.isAnswered()){
                phrase += " - " + askedPhrase.getForeignWord();
                if(askedPhrase.getTranscription() != null && !askedPhrase.getTranscription().equals("")){
                    phrase += " [" + askedPhrase.getTranscription() + "]";
                }
            }

            if(askedPhrase.isTrained()){
                phrase = applyColor(phrase, RIGHT_MESSAGE_COLOR);
                if(question.trainedAfterAnswer() == 1){
                    phrase = makeStrong(phrase);
                }
            }else if(question.trainedAfterAnswer() == -1){
                phrase = applyColor(phrase, WRONG_MESSAGE_COLOR);
                phrase = makeStrong(phrase);
            }

            result = timeAndRightWrongMessage + phrase;
            result = makeNewLine(result);
            resultString = result;
        }

        private String formatTime(ZonedDateTime givenTime){
            return givenTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        private String makeStrong(String givenString){
            return "<strong>" + givenString + "</strong>";
        }

        private String applyColor(String givenString, String color){
            return "<font color=\"" + color + "\">" + givenString + "</font>";
        }

        private String makeNewLine(String givenString){
            return givenString + "</br>";
        }

        private String makeRightWrongMsg(Question givenQuestion){
            String result;
            if(!givenQuestion.isAnswered()){
                result = applyColor(RIGHT_MESSAGE, NON_ANSWERED_MESSAGE_COLOR) + "/" + applyColor(WRONG_MESSAGE, NON_ANSWERED_MESSAGE_COLOR);
            }else if (givenQuestion.answerIsCorrect()){
                result = applyColor(RIGHT_MESSAGE, RIGHT_MESSAGE_COLOR) + "/" + applyColor(WRONG_MESSAGE, NON_ANSWERED_MESSAGE_COLOR);
            }else {
                result = applyColor(RIGHT_MESSAGE, NON_ANSWERED_MESSAGE_COLOR) + "/" + applyColor(WRONG_MESSAGE, WRONG_MESSAGE_COLOR);
            }
            return makeStrong(result);
        }
    }

    public TrainingLog(DatabaseHelper databaseHelper) {
        System.out.println("CALL: TrainingLog(DatabaseHelper databaseHelper) from TrainingLog");
        this.databaseHelper = databaseHelper;
    }

    public Question retrieveSelected(){
        Question question = null;
        if(position >= 0 && position < allQuestions.size()){
            question = allQuestions.get(position);
        }
        return question;
    }

    public Question retrievePrevious(){
        System.out.println("CALL: retrievePrevious() from TrainingLog");
        Question question = null;
        if(position - 1 < allQuestions.size()){
            question = allQuestions.get(position + 1);
        }
        return question;
    }

    public void select(int position){
        System.out.println("CALL: select(int position) from TrainingLog");
        this.position = position;
        reload();
    }

    public void selectPrevious(){
        System.out.println("CALL: selectPrevious() from TrainingLog");
        if(position + 1 < allQuestions.size()){
            select(++position);
        }
    }

    public void nextQuestion(){
        System.out.println("CALL: nextQuestion() from TrainingLog");
        if(position == 0){
            appendToLog(Question.compose(databaseHelper.retrieveRandomPhrase(), databaseHelper));
        }else {
            select(--position);
        }
    }

    public void updateSelectedQuestionsPhrase(){
        databaseHelper.updatePhrase(retrieveSelected().getAskedPhrase());
    }

    public void deleteSelectedPhrase(){
        System.out.println("CALL: deleteSelectedPhrase() from TrainingLog");
        databaseHelper.deletePhrase(retrieveSelected().getAskedPhrase());
        reload();
    }

    public void appendToLog(Question addedQuestion){
        System.out.println("CALL: appendToLog(Question addedQuestion) from TrainingLog");
        allQuestions.add(0, addedQuestion);
        select(0);
    }

    public void reload(){
        System.out.println("CALL: reload() from TrainingLog");
        log = new StringBuilder();
        int rightAnswersNumber = 0;
        int wrongAnswersNumber = 0;
        todayTrainedPhrasesNumber = 0;
        totalTrainedPhrasesNumber = databaseHelper.getTotalTrainedPhrasesNumber();
        totalUntrainedPhrasesNumber = databaseHelper.getTotalUntrainedPhrasesNumber();
        averageAnswersPerDayNumber = new BigDecimal((double) (databaseHelper.getUntilTodayAnswersNumber() + allQuestions.size()) /
                ((double) (databaseHelper.getUntilTodayTrainingHoursSpent() + LocalTime.now().getHour() - 6) / 24D)).setScale(1, BigDecimal.ROUND_HALF_UP);
        trainingCompletionPercentage = new BigDecimal((double) (totalTrainedPhrasesNumber + todayTrainedPhrasesNumber) / (double) (totalTrainedPhrasesNumber + totalUntrainedPhrasesNumber) * 100D).setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
        totalAndActivePhrasesNumber = databaseHelper.getActivePhrasesNumber() == databaseHelper.calculateTotalPhrasesNumber() ? String.valueOf(databaseHelper.calculateTotalPhrasesNumber()) : databaseHelper.getActivePhrasesNumber() + "/" + databaseHelper.calculateTotalPhrasesNumber();
        activeTrainedPhrasesNumber = databaseHelper.getActiveTrainedPhrasesNumber();
        activeUntrainedPhrasesNumber = databaseHelper.getActiveUntrainedPhrasesNumber();
        totalAndActiveUntrainedPhrasesNumber = totalUntrainedPhrasesNumber == activeUntrainedPhrasesNumber ? String.valueOf(totalUntrainedPhrasesNumber) : activeUntrainedPhrasesNumber + "/" + totalUntrainedPhrasesNumber;
        totalAndActiveTrainedPhrasesNumber = totalTrainedPhrasesNumber == activeTrainedPhrasesNumber ? String.valueOf(totalTrainedPhrasesNumber) : activeTrainedPhrasesNumber + "/" + totalTrainedPhrasesNumber;
        for(int i = 0; i < allQuestions.size(); i++){
            Question currentQuestion = allQuestions.get(i);
            if(currentQuestion.isAnswered()){
                todayTrainedPhrasesNumber += currentQuestion.trainedAfterAnswer();
                if(currentQuestion.isAnswerCorrect()){
                    rightAnswersNumber++;
                }else {
                    wrongAnswersNumber++;
                }
            }

            StringBuilder str = new StringBuilder(new TrainingLogQuestionLine(currentQuestion).getResultString());
            if(i == position){
                str.insert(0, "<strong>").append("</strong>");
            }
            log.append(str.toString());
        }
        todayAnswersNumber = rightAnswersNumber + wrongAnswersNumber;
        todayRightAnswersPercentage = String.valueOf((int) ((double) rightAnswersNumber / (double) todayAnswersNumber * 100)) + "%";
    }



    @Override
    public String toString() {
        return log.toString();
    }

    public void setTodayQuestions(List<Question> todayQuestions) {
        allQuestions.addAll(todayQuestions);
        reload();
    }

    public int getTodayAnswersNumber() {
        return todayAnswersNumber;
    }

    private int todayAnswersNumber;

    public String getTodayRightAnswersPercentage() {
        return todayRightAnswersPercentage;
    }

    public int getTodayTrainedPhrasesNumber() {
        return todayTrainedPhrasesNumber;
    }

    public String getTrainingCompletionPercentage() {
        return trainingCompletionPercentage;
    }

    public BigDecimal getAverageAnswersPerDayNumber() {
        return averageAnswersPerDayNumber;
    }

    public String getTotalAndActivePhrasesNumber() {
        return totalAndActivePhrasesNumber;
    }

    public String getTotalAndActiveUntrainedPhrasesNumber() {
        return totalAndActiveUntrainedPhrasesNumber;
    }

    public String getTotalAndActiveTrainedPhrasesNumber() {
        return totalAndActiveTrainedPhrasesNumber;
    }
}
