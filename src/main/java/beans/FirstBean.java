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

    private DAO dao;

    public FirstBean() throws SQLException{
        System.out.println("--- Bean was created");
        dao = new DAO();
    }

    public void exit(){
        dao.backupDB();
    }

    }




