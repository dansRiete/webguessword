package aleks.kuzko.beans;

import aleks.kuzko.utils.DataSource;
import aleks.kuzko.utils.PhrasesRepository;
import aleks.kuzko.dao.UserDao;
import aleks.kuzko.datamodel.User;
import org.hibernate.SessionFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
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
    private PhrasesRepository phrasesRepository;
    private List<User> usersList = new ArrayList<>();
    private SessionFactory sessionFactory = DataSource.getHibernateSessionFactory();

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
        System.out.println("usersList: " + usersList);
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
                this.phrasesRepository = new PhrasesRepository(loggedUser);
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

    public PhrasesRepository getPhrasesRepository() {
        return this.phrasesRepository;
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
