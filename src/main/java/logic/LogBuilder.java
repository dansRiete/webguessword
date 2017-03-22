package logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 12.02.2017.
 */
public class LogBuilder {
    private List<Question> previousQuestions;
    private List<Question> currentQuestions = new ArrayList<>();

    public LogBuilder(List<Question> previousQuestions){
        this.previousQuestions = previousQuestions == null ? new ArrayList<Question>() : previousQuestions;
    }

    public void appendAnswer(Question appendedQuestion){
        currentQuestions.add(appendedQuestion);
        reloadLog();
    }

    private void reloadLog(){

    }
}
