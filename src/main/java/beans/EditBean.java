package beans;

import logic.DAO;
import logic.Phrase;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 31.05.2016.
 */

@ManagedBean
@ViewScoped
public class EditBean {

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    private DAO dao;
    private ArrayList<Phrase> myList;
    private ArrayList<Phrase> currList;
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
        if(loginBean!=null)
            dao = loginBean.returnDAO();
        if(dao!=null){
            currList = dao.getCurrList();
            myList = dao.returnPhrasesList();
            labelsList = dao.reloadLabelsList();
        }
    }

    public void addAction(){
        System.out.println("CALL addAction() from editBean mylist.size=" + myList.size());
        String forWord = this.forWord;
        String natWord = this.natWord;
        String transcr = this.transcr;
        String label = this.label;
        this.forWord = this.natWord = this.transcr = "";
        Phrase phrase = new Phrase(forWord, natWord, transcr, label);
        myList.add(0, phrase);
        if(forWord!=null&&natWord!=null)
            if(forWord.equalsIgnoreCase("")&&natWord.equalsIgnoreCase("")){
                new Thread(){
                    public void run(){
                        dao.insertPhrase(phrase);
                    }
                }.start();
            }

    }

    public void deleteById(Phrase phr){
        dao.deletePhrase(phr);
        myList = dao.returnPhrasesList();
        labelsList = dao.reloadLabelsList();
    }

    public int rowNumbers(){
        if(myList.size()/100>10)
            return myList.size()/10+5;
        else
            return 100;
    }

    public ArrayList<Phrase> getMyList() {
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

    public ArrayList<Phrase> getCurrList() {
        return currList;
    }

    public void setCurrList(ArrayList currList) {
        this.currList = currList;
    }
}
