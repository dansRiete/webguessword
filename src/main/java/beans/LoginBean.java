package beans;

import logic.DAO;
import logic.User;
import org.apache.log4j.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

@ManagedBean(name="login")
@SessionScoped
public class LoginBean implements Serializable {
    private String user;
    private String password;
    User currentUser;
    private DAO dao;
    final static Logger logger = Logger.getLogger(LoginBean.class);
    Connection conn;
    private ArrayList<User> usersList = new ArrayList<>();
    public LoginBean(){
        dao = new DAO(user);
        conn = dao.getConnection();

        //>>Создаём список юзеров ArrayList<User> usersList
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM users");
            while (rs.next()){
                usersList.add(new User(rs.getInt("id"), rs.getString("login"), rs.getString("name"),
                        rs.getString("password"), rs.getString("email")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //<<
    }
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public void checkUser(){
        //!!! EXC !!!
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
            if ((userExist)&&(password.equalsIgnoreCase(currentUser.password))) {
                dao.setLoginBean(this);
                try {
                    FacesContext.getCurrentInstance().getExternalContext().dispatch("learn.xhtml");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /*FacesContext context = FacesContext.getCurrentInstance();
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                try {
                    request.getRequestDispatcher("learn.xhtml").forward(request, response);
                } catch (ServletException e) {
                    System.out.println("--- Servlet exception during request.getRequestDispatcher(\"learn.xhtml\").forward(request, response);");
                    logger.error("Log4G error!", e);
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("--- IOException during request.getRequestDispatcher(\"learn.xhtml\").forward(request, response);");
                    e.printStackTrace();
                }*/
            } else {
                /*try { //>>STACK OVERFLOW! WHY?
                    FacesContext.getCurrentInstance().getExternalContext().dispatch("error.xhtml");
                } catch (IOException e) {
                    e.printStackTrace();
                }*/   //<<
                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                try {
                    response.sendRedirect("error.xhtml");
                } catch (IOException e) {
                    System.out.println("--- IOException during response.sendRedirect(\"error.xhtml\");");
                    e.printStackTrace();
                }
            }
    }


    public DAO returnDAO(){
        return this.dao;
    }
}
