package logic;

import datamodel.Phrase;
import datamodel.Question;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Aleks on 27.03.2017.
 */
public class TrainingLog {

    private List<Question> todayQuestions = new ArrayList<>();
    private List<Question> allQuestions = new LinkedList<>();
    private StringBuilder log = new StringBuilder();
    private DatabaseHelper databaseHelper;
    private int position;
    private String todayRightAnswersPercentage;
    private int todayTrainedPhrasesNumber;
    private int totalTrainedPhrasesNumber;
    private int totalUntrainedPhrasesNumber;
    private int totalPhrasesNumber;
    private String trainingCompletionPercentage = "NOT YET IMPLEMENTED";
    private int averageAnswersPerDayNumber;
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

            if (question.answered()){
                phrase += " - " + askedPhrase.getForeignWord();
                if(askedPhrase.getTranscription() != null && !askedPhrase.getTranscription().equals("")){
                    phrase += " [" + askedPhrase.getTranscription() + "]";
                }
            }

            if(askedPhrase.isTrained()){
                phrase = makeStrong(applyColor(phrase, RIGHT_MESSAGE_COLOR));
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
            if(!givenQuestion.answered()){
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

    public Question retrieveQuestion(int position){
        System.out.println("CALL: retrieveQuestion(int position) from TrainingLog");
        Question question = null;
        if(position > 0 && position < allQuestions.size()){
            question = allQuestions.get(position);
        }
        return question;
    }

    public Question retrieveSelectedQuestion(){
        System.out.println("CALL: retrieveSelectedQuestion() from TrainingLog");
        Question question = null;
        if(position >= 0 && position < allQuestions.size()){
            question = allQuestions.get(position);
        }
        return question;
    }

    public Question retrievePreviousQuestion(){
        System.out.println("CALL: retrievePreviousQuestion() from TrainingLog");
        Question question = null;
        if(position - 1 < allQuestions.size()){
            question = allQuestions.get(position + 1);
        }
        return question;
    }

    public void selectQuestion(int position){
        System.out.println("CALL: selectQuestion(int position) from TrainingLog");
        this.position = position;
        reload();
    }

    public void selectPreviousQuestion(){
        System.out.println("CALL: selectPreviousQuestion() from TrainingLog");
        if(position + 1 < allQuestions.size() - todayQuestions.size()){
            selectQuestion(++position);
        }
    }

    public void nextQuestion(){
        System.out.println("CALL: nextQuestion() from TrainingLog");
        if(position == 0){
            appendToLog(Question.compose(databaseHelper.retrieveRandomPhrase(), databaseHelper));
        }else {
            selectQuestion(--position);
        }
    }

    public void updateSelectedQuestionsPhrase(){
        databaseHelper.updatePhrase(retrieveSelectedQuestion().getAskedPhrase());
    }

    public void deleteSelectedPhrase(){
        System.out.println("CALL: deleteSelectedPhrase() from TrainingLog");
        databaseHelper.deletePhrase(retrieveSelectedQuestion().getAskedPhrase());
        reload();
    }

    public void appendToLog(Question addedQuestion){
        System.out.println("CALL: appendToLog(Question addedQuestion) from TrainingLog");
        allQuestions.add(0, addedQuestion);
        selectQuestion(0);
//        reload();
    }

    public void reload(){
        System.out.println("CALL: reload() from TrainingLog");
        log = new StringBuilder();
        int rightAnswersNumber = 0;
        int wrongAnswersNumber = 0;
        todayTrainedPhrasesNumber = 0;
        for(int i = 0; i < allQuestions.size(); i++){
            Question currentQuestion = allQuestions.get(i);
            if(currentQuestion.answered()){
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

    public List<Question> getTodayQuestions() {
        return todayQuestions;
    }

    public void setTodayQuestions(List<Question> todayQuestions) {
        this.todayQuestions = todayQuestions;
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

    public int getTotalPhrasesNumber() {
        return totalPhrasesNumber;
    }

    public int getTotalTrainedPhrasesNumber() {
        return totalTrainedPhrasesNumber;
    }

    public int getTotalUntrainedPhrasesNumber() {
        return totalUntrainedPhrasesNumber;
    }

    public String getTrainingCompletionPercentage() {
        return trainingCompletionPercentage;
    }

    public int getAverageAnswersPerDayNumber() {
        return averageAnswersPerDayNumber;
    }

    /*private void reloadStatisticsTable(){
        System.out.println("CALL: reloadStatisticsTable() from InterfaceBean");

        //After the answerField creates String like this - "40.2 ➩ 37.3"
        if(!selectedQuestion.answered()){
            BigDecimal previous = new BigDecimal(selectedQuestion.getAskedPhrase().probabilityFactor).setScale(1, RoundingMode.HALF_UP);
            currPhrasesProbabilityFactor = previous.toString();
        }else{
            BigDecimal previous = new BigDecimal(selectedQuestion.getAskedPhrase().previousProbabilityFactor).setScale(1, RoundingMode.HALF_UP);
            BigDecimal present = new BigDecimal(selectedQuestion.getAskedPhrase().probabilityFactor).setScale(1, RoundingMode.HALF_UP);
            currPhrasesProbabilityFactor = previous + "➩" + present + "(" + present.subtract(previous) + ")";
        }

        //After the answerField creates String like this - "0.06116% ➩ 0.07294%"
         currPhrPercentOfAppearance = *//*selectedQuestion.getPercentChanceView();*//* "NOT YET IMPLEMENTED";


        if(selectedQuestion.getAskedPhrase().lastAccessDateTime != null){

            currentPhraseLastAccesssDate = selectedQuestion.getAskedPhrase().lastAccessDateTime.format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelLastAccsDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedQuestion.getAskedPhrase().lastAccessDateTime.toEpochSecond());
        }

        if(selectedQuestion.getAskedPhrase().collectionAddingDateTime != null){

            currPhrAbsCreateDate = LocalDateTime.ofInstant(selectedQuestion.getAskedPhrase().collectionAddingDateTime.toInstant(),
                    ZoneId.of("EET")).format(DateTimeFormatter.ofPattern("d MMM y HH:mm", Locale.ENGLISH));

            currPhrRelCreateDate = retDiff.retDiffInTime(System.currentTimeMillis() - selectedQuestion.getAskedPhrase().collectionAddingDateTime.toEpochSecond());
        }

//        currPhrLabel = selectedQuestion.label;

        //>>Calculate session statistics
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        int numOfPhrForSession = trainingLog.getAllQuestions().size();
        currPhrId = selectedQuestion.getAskedPhrase().id;
         if(!selectedQuestion.getAskedPhrase().hasBeenAnswered){
            currentPhraseRate = new BigDecimal(selectedQuestion.getAskedPhrase().multiplier).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
         }else {
             currentPhraseRate = (new BigDecimal(selectedQuestion.getAskedPhrase().previousMultiplier).setScale(2, BigDecimal.ROUND_HALF_UP) + " ➩ " + new BigDecimal(selectedQuestion.getAskedPhrase().multiplier).setScale(2, BigDecimal.ROUND_HALF_UP));
         }

        for(Question phrs : trainingLog.getAllQuestions()){
            if(!phrs.getAskedPhrase().hasBeenAnswered)
                numOfNonAnswForSession++;
            else if(phrs.getAskedPhrase().hasBeenAnsweredCorrectly)
                numOfRightAnswForSession++;
        }

        answersForSessionNumber = numOfPhrForSession - numOfNonAnswForSession;
        //Generates a string with the percentage of correct answers to the total number of answers
        todayRightAnswersPercentage = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(answersForSessionNumber ==0?1: answersForSessionNumber),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
        trainedPhrasesNumber = databaseHelper.getLearntWordsAmount();
        nonTrainedPhrasesNumber = databaseHelper.getNonLearntWordsAmount();
        totalPhrasesNumber = trainedPhrasesNumber + nonTrainedPhrasesNumber;

         try{
             averageAnswersPerDay = (int) ((float) (databaseHelper.getTotalTrainingAnswers() + answersForSessionNumber) / (float) (databaseHelper.getTotalTrainingHoursSpent() + ZonedDateTime.now(ZoneId.of("Europe/Kiev")).getHour() - 6) * 24);
         }catch (ArithmeticException e){
             averageAnswersPerDay = 0;
         }
        //<<

    }*/
}
