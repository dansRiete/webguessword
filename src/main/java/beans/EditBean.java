package beans;

import logic.DAO;
import logic.PhraseDb;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private ArrayList<PhraseDb> myList;
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

    public boolean filterByLabel(Object value, Object filter, Locale locale) {
        String filterText = (filter == null) ? null : filter.toString().trim();
        System.out.println("--- filterByLabel() value is \"" + (String) value.toString() + "\" filter is \"" + filterText+'\"');
        if(filterText == null||filterText.equals("")||filterText.equals("null")) {
            return true;
        }

        if(value == null) {
            return false;
        }

        return filter.equals(value);
    }

    public String editAction(PhraseDb order) {
        order.setEditable(true);
        return null;
    }

    public int rowNumbers(){
        if(myList.size()/100>10)
            return myList.size()/10+5;
        else
            return 100;
    }
    public void saveAction(){
        for(PhraseDb phr : myList){
            phr.setEditable(false);
        }
    }
    public void deleteUsers(){

    }

    public ArrayList<PhraseDb> getMyList() {
        return myList;
    }

    public void setMyList(ArrayList<PhraseDb> myList) {
        this.myList = myList;
    }

    public List<String> getLabelsList() {
        return labelsList;
    }

    public void setLabelsList(List<String> labelsList) {
        this.labelsList = labelsList;
    }
}
