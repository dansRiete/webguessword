package beans;

import datamodel.Phrase;
import logic.DAO;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 31.05.2016.
 */

@ManagedBean
@SessionScoped
public class EditBean implements Serializable{

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;

    private DAO dao;
    private ArrayList<Phrase> myList;
    private List<String> labelsList;
    private String foreignWord;
    private String nativeWord;
    private String transcription;
    private String label;
    private BigDecimal probabilityFactor;

    public EditBean(){
        init();
    }

    @PostConstruct
    private void init(){
        System.out.println("EDITBEAN CALL init()");
        if(loginBean != null)
            dao = loginBean.getDao();
        if(dao != null){
            myList = dao.getActivePhrases();
            labelsList = dao.retievePossibleLabels();
            labelsList.add("All");
        }
    }

    public void addAction(){

        System.out.println("EDITBEAN CALL START addAction() from editBean mylist.size=" + myList.size());

        if( this.foreignWord != null && this.nativeWord != null && !this.foreignWord.equalsIgnoreCase("") && !this.nativeWord.equalsIgnoreCase("")){

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            Phrase phrase = new Phrase(0, this.foreignWord, this.nativeWord, this.transcription, new BigDecimal(30), ZonedDateTime.now(ZoneId.of("UTC")), this.label, null, 0, 0, false, 1, dao);
            this.foreignWord = this.nativeWord = this.transcription = this.label = "";
            probabilityFactor = null;
            myList.add(0, phrase);
            dao.insertPhrase(phrase);

        }

        System.out.println("EDITBEAN CALL END addAction() from editBean mylist.size=" + myList.size());
    }

    public void deleteById(Phrase phr){

        System.out.println("EDITBEAN CALL START deleteById(Phrase phr)  mylist.size=" + myList.size() + " deleted phrase is " + phr.foreignWord);
        dao.deletePhrase(phr);
        myList = dao.getActivePhrases();
        labelsList = dao.retievePossibleLabels();
        System.out.println("EDITBEAN CALL END deleteById(Phrase phr)  mylist.size=" + myList.size());

    }

    public int rowNumbers(){

        if(myList.size() / 100 > 10){
            return myList.size() / 10 + 5;
        }else{
            return 100;
        }

    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
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

    public String getForeignWord() {
        return foreignWord;
    }

    public void setForeignWord(String foreignWord) {
        this.foreignWord = foreignWord;
    }

    public String getNativeWord() {
        return nativeWord;
    }

    public void setNativeWord(String nativeWord) {
        this.nativeWord = nativeWord;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getProbabilityFactor() {
        return probabilityFactor;
    }

    public void setProbabilityFactor(double probabilityFactor) {
        this.probabilityFactor = new BigDecimal(probabilityFactor);
    }
}
