package beans;

import datamodel.User;
import Exceptions.NoAliveDatabasesException;
import logic.DAO;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;

@ManagedBean(name="login")
@SessionScoped
public class LoginBean implements Serializable {

    private String user;
    private String password;
    private User currentUser;
    private DAO dao;
    private Connection mainDbConn;
    private ArrayList<User> usersList = new ArrayList<>();
    public String activeRemoteHost;
    public String activeUser;
    public String activePassword;
    private final static String ORIGINAL_REMOTE_HOST = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public final static boolean USE_LOCAL_DB = false;

    public LoginBean(){
        determineAndConnectToDB();
        ResultSet rs = null;

        //>>Create list of users
        try {
            Statement st = mainDbConn.createStatement();
            rs = st.executeQuery("SELECT * FROM users");
            while (rs.next()){
                usersList.add(new User(rs.getInt("id"), rs.getString("login"), rs.getString("name"),
                        rs.getString("password"), rs.getString("email")));
            }
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in LoginBean constructor");
            e.printStackTrace();
        }
        //<<
    }

    private void determineAndConnectToDB() {

        System.out.println("CALL: determineAndConnectToDB() from LoginBean");
        String dbConnected = "EXCEPTION: in determineAndConnectToDB() from LoginBean";
        if(USE_LOCAL_DB){
            activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
            activeUser = "root";
            activePassword = "root";
            try {
                mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnected = "Local virtual DB connected";
            System.out.println(dbConnected);
            return;
        }
        try {
            activeRemoteHost = ORIGINAL_REMOTE_HOST;
            activeUser = "adminLtuHq9R";
            activePassword = "d-AUIKakd1Br";
            mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
            dbConnected = "Remote DB was connected";
        } catch (SQLException e) {
            try {
                activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
                activeUser = "adminLtuHq9R";
                activePassword = "d-AUIKakd1Br";
                mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                dbConnected = "Remote DB was connected through the local port 3306 forwarding";
            } catch (SQLException e1) {
                try {
                    activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3307;
                    activeUser = "adminLtuHq9R";
                    activePassword = "d-AUIKakd1Br";
                    mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                    dbConnected = "Remote DB was connected through the local port 3307 forwarding";
                } catch (SQLException e2) {
                    e2.printStackTrace();
                    System.out.println();
                    throw new NoAliveDatabasesException();
                }
            }
        } finally {
            System.out.println(dbConnected);
        }
    }

    public void checkAndRedirectUser(){
        System.out.println("CALL: checkAndRedirectUser() from LoginBean");
        boolean userExist = false;

        //Check if there is such user
        for(User user : usersList){
            if(this.user.equalsIgnoreCase(user.login)){
                userExist = true;
                currentUser = user;
                break;
            }
        }
        //If user exists and password is correct then dispatch to "learn.xhtml" otherwise sendRedirect("error.xhtml")
        if ((userExist)&&(password.equals(currentUser.password))) {
            dao = new DAO(this);
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            try {
                response.sendRedirect("learn.xhtml");
            } catch (IOException e) {
                System.out.println("EXCEPTION#2: in checkAndRedirectUser() from LoginBean");
                e.printStackTrace();
            }
        } else {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            try {
                response.sendRedirect("error.xhtml");
            } catch (IOException e) {
                System.out.println("EXCEPTION#2: in checkAndRedirectUser() from LoginBean");
                e.printStackTrace();
            }
        }

    }

    public Connection getConnection(){
        return mainDbConn;
    }

    public DAO getDao(){
        return this.dao;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user){
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }
}
