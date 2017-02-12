package logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 12.02.2017.
 */
public class LogBuilder {
    private List<Answer> previousAnswers;
    private List<Answer> currentAnswers = new ArrayList<>();

    public LogBuilder(List<Answer> previousAnswers){
        this.previousAnswers = previousAnswers == null ? new ArrayList<Answer>() : previousAnswers;
    }

    public void appendAnswer(Answer appendedAnswer){
        currentAnswers.add(appendedAnswer);
        reloadLog();
    }

    private void reloadLog(){

    }
}
