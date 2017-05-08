package beans;

import dao.UserDao;
import datamodel.User;
import dao.DatabaseHelper;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import utils.DatabaseUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "login")
@SessionScoped
public class LoginBean implements Serializable {

    private String userTextField;
    private String passwordTextField;
    private User loggedUser;
    private DatabaseHelper databaseHelper;
    private List<User> usersList = new ArrayList<>();
    private SessionFactory sessionFactory = DatabaseUtils.getHibernateSessionFactory();

    @Autowired
    private UserDao userDao;

    public LoginBean() {
        System.out.println("LoginBean's constructor");
        UserDao userDao = new UserDao();
        userDao.openCurrentSession();
        this.usersList = userDao.fetchAll();
        userDao.closeCurrentSession();
    }

    public void checkCredentialsAndRedirectUser() {
        System.out.println("CALL: checkCredentialsAndRedirectUser() from LoginBean");
        boolean userExist = false;
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        User loggedUser = null;

        //Check if there is such user
        for (User user : usersList) {
            if (this.userTextField.equals(user.login)) {
                userExist = true;
                loggedUser = user;
                break;
            }
        }

        //If user exists and passwordTextField is correct then dispatch to "learn.xhtml" otherwise sendRedirect("error.xhtml")
        try {
            if (userExist && passwordTextField.equals(loggedUser.password)) {
                this.loggedUser = loggedUser;
                this.databaseHelper = new DatabaseHelper(this, sessionFactory);
                response.sendRedirect("learn.xhtml");
            } else {
                response.sendRedirect("error.xhtml");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public DatabaseHelper getDatabaseHelper() {
        return this.databaseHelper;
    }

    public String getUserTextField() {
        return userTextField;
    }

    public void setUserTextField(String userTextField) {
        this.userTextField = userTextField;
    }

    public String getPasswordTextField() {
        return passwordTextField;
    }

    public void setPasswordTextField(String passwordTextField) {
        this.passwordTextField = passwordTextField;
    }

    public User getLoggedUser() {
        return loggedUser;
    }
}
