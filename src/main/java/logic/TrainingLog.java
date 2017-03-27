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

    public List<Question> getTodayQuestions() {
        return todayQuestions;
    }

    public void setTodayQuestions(List<Question> todayQuestions) {
        this.todayQuestions = todayQuestions;
    }

    private List<Question> todayQuestions = new ArrayList<>();
    private List<Question> sessionQuestions = new LinkedList<>();
    private StringBuilder log = new StringBuilder();

    public Question getQuestion(int position){
        return sessionQuestions.get(position);
    }

    public void addQuestion(Question addedQuestion){
        sessionQuestions.add(0, addedQuestion);
        reloadLog();
    }

    public void reloadLog(){
        log = new StringBuilder();
        sessionQuestions.forEach(question -> log.append(new QuestionLine(question).getResultString()));
    }

    public String getLogString(){
        return log.toString();
    }
}
