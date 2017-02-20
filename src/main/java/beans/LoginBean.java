package beans;

import Exceptions.DataBaseConnectionException;
import logic.*;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.StringJoiner;

@ManagedBean(name="login")
@SessionScoped
public class LoginBean implements Serializable {
    private String user;
    private String password;
    User currentUser;
    private DAO dao;
    Connection mainDbConn;

    private ArrayList<User> usersList = new ArrayList<>();
    public static String activeRemoteHost;
    public static String activeUser;
    public static String activePassword;
    private String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private String localHost3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private String localHost3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public final static boolean USE_LOCAL_DB = false;

    private void connectToDatabase() {

        System.out.println("CALL: connectToDatabase() from LoginBean");
        String dbConnected = "EXCEPTION: in connectToDatabase() from LoginBean";
        if(USE_LOCAL_DB){
            activeRemoteHost = localHost3306;
            activeUser = "root";
            activePassword = "root";
            try {
                mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnected = "Local DB connected without port forwarding";
            System.out.println(dbConnected);
            return;
        }
        try {
            activeRemoteHost = remoteHost;
            activeUser = "adminLtuHq9R";
            activePassword = "d-AUIKakd1Br";
            mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
            dbConnected = "- Remote DB was connected";
        } catch (SQLException e) {
            try {
                activeRemoteHost = localHost3306;
                activeUser = "adminLtuHq9R";
                activePassword = "d-AUIKakd1Br";
                mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                dbConnected = "- Local DB 3306 was connected";
            } catch (SQLException e1) {
                try {
                    activeRemoteHost = localHost3307;
                    activeUser = "adminLtuHq9R";
                    activePassword = "d-AUIKakd1Br";
                    mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                    dbConnected = "- Local DB 3307 was connected";
                } catch (SQLException e2) {
                    e2.printStackTrace();
                    System.out.println("EXCEPTION: in connectToDatabase() from LoginBean");
                    throw new DataBaseConnectionException();
                }
            }
        } finally {
            System.out.println(dbConnected);
        }
    }



    public Connection getConnection(){
        return mainDbConn;
    }

    public LoginBean(){
        connectToDatabase();
        ResultSet rs = null;

        //>>Создаём список юзеров ArrayList<User> usersList
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

    public void checkUserAndPassword(){
        System.out.println("CALL: checkUserAndPassword() from LoginBean");
        boolean userExist = false;

        //Проверяем или введенный пользователь присутствует в базе данных
        for(User user : usersList){
            if(this.user.equalsIgnoreCase(user.login)){
                userExist = true;
                currentUser = user;
                break;
            }
        }
        //Если пользователь существует и пароль совпадает то dispatch("learn.xhtml")
        //в противном случае sendRedirect("error.xhtml")
        if ((userExist)&&(password.equals(currentUser.password))) {
            dao = new DAO(this);
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            try {
                response.sendRedirect("learn.xhtml");
            } catch (IOException e) {
                System.out.println("EXCEPTION#2: in checkUserAndPassword() from LoginBean");
                e.printStackTrace();
            }
        } else {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            try {
                response.sendRedirect("error.xhtml");
            } catch (IOException e) {
                System.out.println("EXCEPTION#2: in checkUserAndPassword() from LoginBean");
                e.printStackTrace();
            }
        }

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
