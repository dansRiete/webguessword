package beans;

import logic.DAO;
import logic.PhraseDb;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

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
    public EditBean(){
        init();

    }
    @PostConstruct
    private void init(){
        if(loginBean!=null)
            dao = loginBean.returnDAO();
        if(dao!=null)
            myList = dao.returnPhrasesList();
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
}
