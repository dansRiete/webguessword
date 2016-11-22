package beans;

import logic.Phrase;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

@ManagedBean(name="stat")
@SessionScoped
public class StatBean/* implements Serializable*/{
//    DAO dao = DAO.getInstance();

    public StatBean(){}

    private int numOfPhrForSession = 0;
    private int numOfAnswForSession = 0;
    private int numOfRightAnswForSession = 0;
    private int totalNumberOfWords;
    private int totalNumberOfLearnedWords;
    private String percentOfRightAnswers;

    /*@ManagedProperty(value="#{interfaceBean}")
    private InterfaceBean interfaceBean;*/

    /*@PostConstruct
    public void calculateResults(){
        int numOfNonAnswForSession = 0;
        int numOfRightAnswForSession = 0;
        numOfPhrForSession = interfaceBean.returnListOfPhrases().size();
        for(Phrase phrs : interfaceBean.returnListOfPhrases()){
            if(phrs.thisPhraseHadBeenAnsweredCorrectly==null)
                numOfNonAnswForSession++;
            else if(phrs.thisPhraseHadBeenAnsweredCorrectly)
                numOfRightAnswForSession++;
        }
        numOfAnswForSession = numOfPhrForSession-numOfNonAnswForSession;
        this.numOfRightAnswForSession = numOfRightAnswForSession;

    }*/

    public int getNumOfPhrForSession() {
        return numOfPhrForSession;
    }
    public void setNumOfPhrForSession(int numOfPhrForSession) {
        this.numOfPhrForSession = numOfPhrForSession;
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

    public String getPercentOfRightAnswers() {
        return percentOfRightAnswers;
    }
    public void setPercentOfRightAnswers(String percentOfRightAnswers) {
        this.percentOfRightAnswers = percentOfRightAnswers;
    }

    public int getNumOfAnswForSession() {
        return numOfAnswForSession;
    }
    public void setNumOfAnswForSession(int numOfAnswForSession) {
        this.numOfAnswForSession = numOfAnswForSession;
    }

    public int getNumOfRightAnswForSession() {
        return numOfRightAnswForSession;
    }
    public void setNumOfRightAnswForSession(int numOfRightAnswForSession) {
        this.numOfRightAnswForSession = numOfRightAnswForSession;
    }

    /*public void setInterfaceBean(InterfaceBean interfaceBean) {
        this.interfaceBean = interfaceBean;
    }*/
}