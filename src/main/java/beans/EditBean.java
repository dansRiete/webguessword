package beans;

import datamodel.Phrase;
import logic.DatabaseHelper;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Aleks on 31.05.2016.
 */

@ManagedBean
@RequestScoped
public class EditBean implements Serializable{

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;

    private DatabaseHelper databaseHelper;
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
            databaseHelper = loginBean.getDatabaseHelper();
        if(databaseHelper != null){
            myList = databaseHelper.getActivePhrases();
            Collections.sort(myList, ((phrase1, phrase2) -> {
                if(phrase1.collectionAddingDateTime.isAfter(phrase2.collectionAddingDateTime)){
                    return -1;
                }else if(phrase2.collectionAddingDateTime.isAfter(phrase1.collectionAddingDateTime)){
                    return 1;
                }else {
                    if(phrase1.getId() > phrase2.getId()){
                        return -1;
                    }else {
                        return 1;
                    }
                }
            }));
            labelsList = databaseHelper.retievePossibleLabels();
        }
    }

    public void addAction(){

        System.out.println("EDITBEAN CALL START addAction() from editBean mylist.size=" + myList.size());

        if( this.foreignWord != null && this.nativeWord != null && !this.foreignWord.equalsIgnoreCase("") && !this.nativeWord.equalsIgnoreCase("")){

            Phrase phrase = new Phrase(0, this.foreignWord, this.nativeWord, this.transcription, 30,
                    ZonedDateTime.now(ZoneId.of("UTC")), this.label, null, 1, databaseHelper);
            this.foreignWord = this.nativeWord = this.transcription = this.label = "";
            probabilityFactor = null;
            myList.add(0, phrase);
            databaseHelper.insertPhrase(phrase);
            databaseHelper.reloadPhrasesAndIndices();

        }

        System.out.println("EDITBEAN CALL END addAction() from editBean mylist.size=" + myList.size());
    }

    public void deleteById(Phrase phr){

        System.out.println("EDITBEAN CALL START deleteById(Phrase phr)  mylist.size=" + myList.size() + " deleted phrase is " + phr.foreignWord);
        databaseHelper.deletePhrase(phr);
        myList = databaseHelper.getActivePhrases();
        labelsList = databaseHelper.retievePossibleLabels();
        databaseHelper.reloadPhrasesAndIndices();
        System.out.println("EDITBEAN CALL END deleteById(Phrase phr)  mylist.size=" + myList.size());

    }

    public void updateAll(){
//        myList.forEach(databaseHelper::updatePhrase);
    }

    public void updatePhrase(Phrase phrase){
        databaseHelper.updatePhrase(phrase);
    }

    public int rowNumbers(){

        /*if(myList.size() / 100 > 10){
            return myList.size() / 10 + 5;
        }else{
            return 100;
        }*/
        return 50;

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
