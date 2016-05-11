package beans;

import logic.DAO;

import java.io.Serializable;
import java.sql.*;
import java.util.Random;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class FirstBean implements Serializable{
    @ManagedProperty(value="#{loginBean}")
    private LoginBean logBean;
    private boolean flag = false;
    private DAO dao;


    private String outcomeForWord = "";
    private String outcomeNatWord = "";
    private String outcomeTransc = "";
    private String resultOutcomeText = "";
    static String host1 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static String host2 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static Statement st;

    Random random = new Random();

    public LoginBean getLogBean(){
        return logBean;
    }

    public void setLogBean(LoginBean logBean){
        this.logBean = logBean;
    }

    public FirstBean() throws SQLException{
        System.out.println("-------- Bean was created");
        dao = new DAO();
    }

    public String getOutcomeForWord(){
        return outcomeForWord;
    }

    public void setOutcomeForWord(String name){
        this.outcomeForWord = name;
    }

    public String getOutcomeNatWord(){
        return outcomeNatWord;
    }

    public void setOutcomeNatWord(String name){
        this.outcomeNatWord = name;
    }

    public String getOutcomeTransc(){
        return outcomeTransc;
    }

    public void setOutcomeTransc(String name){
        this.outcomeTransc = name;
    }

    public String getResultOutcomeText(){
        return resultOutcomeText;
    }

    public void setResultOutcomeText(String dispText){
        this.resultOutcomeText = dispText;
    }

    public void refresh() throws SQLException{
        System.out.println("-------- refresh() was called");
        System.out.println("-------- Password=" + logBean.getPassword());
        dao.backupDB();
        }

    }




