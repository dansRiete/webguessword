package beans;

import logic.DAO;
import logic.Phrase;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 31.05.2016.
 */

@ManagedBean
@RequestScoped
public class EditBean {

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    private DAO dao;
    private ArrayList<Phrase> myList;
//    private ArrayList<Phrase> currList;
    private List<String> labelsList;
    private String forWord;
    private String natWord;
    private String transcr;
    private String label;
    public EditBean(){
        init();
    }
    @PostConstruct
    private void init(){
        System.out.println("EDITBEAN CALL init()");
        if(loginBean!=null)
            dao = loginBean.getDao();
        if(dao!=null){
//            currList = dao.getCurrList();
            myList = dao.returnPhrasesList();
            labelsList = dao.reloadLabelsList();
        }
    }

    public void addAction(){
        System.out.println("EDITBEAN CALL START addAction() from editBean mylist.size=" + myList.size());

        if((this.forWord!=null && this.natWord!=null) && (!this.forWord.equalsIgnoreCase("") && !this.natWord.equalsIgnoreCase(""))){
            String forWord = this.forWord;
            String natWord = this.natWord;
            String transcr = this.transcr;
            String label = this.label;
            this.forWord = this.natWord = this.transcr = "";
            Phrase phrase = new Phrase(forWord, natWord, transcr, label);
            System.out.println("EDITBEAN added phrase is " + phrase.forWord);
            myList.add(0, phrase);
            dao.insertPhrase(phrase);
        }

        System.out.println("EDITBEAN CALL END addAction() from editBean mylist.size=" + myList.size());

    }

    public void deleteById(Phrase phr){
        System.out.println("EDITBEAN CALL START deleteById(Phrase phr)  mylist.size=" + myList.size());
        dao.deletePhrase(phr);
        System.out.println("EDITBEAN CALL deleted phrase is " + phr.forWord);
        myList = dao.returnPhrasesList();
        labelsList = dao.reloadLabelsList();
        System.out.println("EDITBEAN CALL END deleteById(Phrase phr)  mylist.size=" + myList.size());
    }

    public int rowNumbers(){
        if(myList.size()/100>10)
            return myList.size()/10+5;
        else
            return 100;
    }

    public ArrayList<Phrase> getMyList() {
        System.out.println("EDITBEAN CALL getMyList() myList.size() = " + myList.size());
//        dao.reloadCollectionOfPhrases();
//        myList = dao.returnPhrasesList();
        return myList;
    }

    public void setMyList(ArrayList<Phrase> myList) {
        this.myList = myList;
    }

    public List<String> getLabelsList() {
        return labelsList;
    }

    public void setLabelsList(List<String> labelsList) {
        this.labelsList = labelsList;
    }

    public String getForWord() {
        return forWord;
    }

    public void setForWord(String forWord) {
        this.forWord = forWord;
    }

    public String getNatWord() {
        return natWord;
    }

    public void setNatWord(String natWord) {
        this.natWord = natWord;
    }

    public String getTranscr() {
        return transcr;
    }

    public void setTranscr(String transcr) {
        this.transcr = transcr;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /*public ArrayList<Phrase> getCurrList() {
        return currList;
    }

    public void setCurrList(ArrayList currList) {
        this.currList = currList;
    }*/
}
