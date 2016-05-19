package beans;

import logic.DAO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name="stat")
@SessionScoped
public class StatBean/* implements Serializable*/{
//    DAO dao = DAO.getInstance();

    public StatBean(){}

    private int dayCounter = 0;
    private int totalNumberOfWords;
    private int totalNumberOfLearnedWords;
    public void dayCounterIncrement(){
        dayCounter++;
    }

    public int getDayCounter() {
        return dayCounter;
    }

    public void setDayCounter(int dayCounter) {
        this.dayCounter = dayCounter;
    }

    public int getTotalNumberOfWords() {
        return totalNumberOfWords;
    }

    public void setTotalNumberOfWords(int totalNumberOfWords) {
        this.totalNumberOfWords = totalNumberOfWords;
    }

    public int getTotalNumberOfLearnedWords() {
        return totalNumberOfLearnedWords;
    }

    public void setTotalNumberOfLearnedWords(int totalNumberOfLearnedWords) {
        this.totalNumberOfLearnedWords = totalNumberOfLearnedWords;
    }
}