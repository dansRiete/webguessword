package logic;

import datamodel.Question;
import datamodel.QuestionLine;

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

    public Question getQuestion(int position){
        return allQuestions.get(position);
    }

    public void addQuestion(Question addedQuestion){
        allQuestions.add(0, addedQuestion);
        reloadLog();
    }

    public void reloadLog(){
        log = new StringBuilder();
        allQuestions.forEach(question -> log.append(new QuestionLine(question).getResultString()));
    }

    public List<Question> getTodayQuestions() {
        return todayQuestions;
    }

    public void setTodayQuestions(List<Question> todayQuestions) {
        this.todayQuestions = todayQuestions;
        allQuestions.addAll(todayQuestions);
        reloadLog();
    }

    public List<Question> getAllQuestions() {
        return allQuestions;
    }

    @Override
    public String toString() {
        return log.toString();
    }

    public int size(){
        return allQuestions.size();
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
        rightAnswersPercentage = ((new BigDecimal(numOfRightAnswForSession)).divide(new BigDecimal(answersForSessionNumber ==0?1: answersForSessionNumber),2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))).setScale(0, RoundingMode.HALF_UP)+"%";
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
