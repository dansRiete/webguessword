package beans;

import logic.DAO;
import logic.Phrase;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleks on 31.05.2016.
 */
@ManagedBean
@SessionScoped
public class EditBean {

    @ManagedProperty(value="#{login}")
    private LoginBean loginBean;
    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    private DAO dao;
    private ArrayList<Phrase> myList;
    private List<String> labelsList;
    public EditBean(){
        init();

    }
    @PostConstruct
    private void init(){
        if(loginBean!=null)
            dao = loginBean.returnDAO();
        if(dao!=null){
            myList = dao.returnPhrasesList();
            labelsList = dao.returnLabelsList();
        }
    }

    public void deleteById(Phrase phr){
        dao.deletePhrase(phr);
        myList = dao.returnPhrasesList();
        labelsList = dao.returnLabelsList();
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
}
