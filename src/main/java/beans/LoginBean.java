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

@ManagedBean(name="login")
@SessionScoped
public class LoginBean implements Serializable {
    private String user;
    private String password;
    User currentUser;
    private DAO dao;
//    final static Logger logger = Logger.getLogger(LoginBean.class);

    private ArrayList<User> usersList = new ArrayList<>();
    private String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private String localHost = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";

    public LoginBean(){
        Connection conn;
        ResultSet rs = null;
        String dbConnected = null;

//        dao = new DAO(this);
        try{
            conn = DriverManager.getConnection(remoteHost, "adminLtuHq9R", "d-AUIKakd1Br");
            dbConnected = "- Remote DB was connected";
        }catch (SQLException e){
            try{
                conn = DriverManager.getConnection(localHost, "adminLtuHq9R", "d-AUIKakd1Br");
                dbConnected = "- Local DB was connected" ;
            }catch (SQLException e1){
                e1.printStackTrace();
                System.out.println("EXCEPTION: in DAO constructor");
                throw new DataBaseConnectionException();
            }
        }

        System.out.println("CALL: LoginBean() constructor " + dbConnected);

        //>>Создаём список юзеров ArrayList<User> usersList
        try {
            Statement st = conn.createStatement();
            rs = st.executeQuery("SELECT * FROM users");
            while (rs.next()){
                usersList.add(new User(rs.getInt("id"), rs.getString("login"), rs.getString("name"),
                        rs.getString("password"), rs.getString("email")));
            }
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in LoginBean constructor");
            e.printStackTrace();
        }finally {
            try {
                conn.close();
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
//            dao.setLoginBean(this);
            /*try {
                FacesContext.getCurrentInstance().getExternalContext().dispatch("learn.xhtml");
            } catch (IOException e) {
                System.out.println("EXCEPTION#1: in checkUserAndPassword() from LoginBean");
                e.printStackTrace();
            }*/
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
