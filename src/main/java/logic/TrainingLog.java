package logic;

import datamodel.Question;
import datamodel.QuestionLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 27.03.2017.
 */
public class TrainingLog {

    private List<Question> questions = new ArrayList<>();
    private StringBuilder log = new StringBuilder();

    public Question getQuestion(int position){
        return questions.get(position);
    }

    public void addQuestion(Question addedQuestion){
        questions.add(0, addedQuestion);
    }

    public void reloadLog(){
        log = new StringBuilder();
        questions.forEach(question -> log.append(new QuestionLine(question).getResultString()));
    }

    public String getLogString(){
        return log.toString();
    }
}
