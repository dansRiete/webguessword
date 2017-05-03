package beans;

import dao.UserDao;
import datamodel.Phrase;
import datamodel.Question;
import datamodel.User;
import dao.DatabaseHelper;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Component;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "login")
@SessionScoped
//@Component
public class LoginBean implements Serializable {

    private String userTextField;
    private String passwordTextField;
    private User loggedUser;
    private DatabaseHelper databaseHelper;
    private Connection mainDbConn;
    private List<User> usersList = new ArrayList<>();
    public String activeRemoteHost;
    public String activeUser;
    public String activePassword;
    private final static String ORIGINAL_REMOTE_HOST = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public final static boolean USE_LOCAL_DB = true;
    private SessionFactory sessionFactory;

    public LoginBean() {
        System.out.println("LoginBean's constructor");
        determineAliveDbAndConnectTo();
        buildSessionFactory();
        UserDao userDao = new UserDao(sessionFactory);
        userDao.openCurrentSession();
        this.usersList = userDao.findAll();
        userDao.closeCurrentSession();
    }

    private void determineAliveDbAndConnectTo() {

        System.out.println("CALL: determineAliveDbAndConnectTo() from LoginBean");
        String conectedDatabaseMessage = null;
        if (USE_LOCAL_DB) {
            this.activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
            this.activeUser = "root";
            this.activePassword = "root";
            try {
                mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                conectedDatabaseMessage = "Local virtual DB connected";
                System.out.println(conectedDatabaseMessage);
                return;
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        this.activeUser = "adminLtuHq9R";
        this.activePassword = "d-AUIKakd1Br";

        try {
            this.activeRemoteHost = ORIGINAL_REMOTE_HOST;

            this.mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
            conectedDatabaseMessage = "Remote DB was connected";
        } catch (SQLException e) {
            try {
                this.activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
                this.mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                conectedDatabaseMessage = "Remote DB was connected through the local port 3306 forwarding";
            } catch (SQLException e1) {
                try {
                    this.activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3307;
                    this.mainDbConn = DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                    conectedDatabaseMessage = "Remote DB was connected through the local port 3307 forwarding";
                } catch (SQLException e2) {
                    e2.printStackTrace();
                    System.out.println();
                    throw new RuntimeException("NoAliveDatabasesException");
                }
            }
        } finally {
            if (conectedDatabaseMessage != null) {
                System.out.println(conectedDatabaseMessage);
            }
        }
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

    public SessionFactory buildSessionFactory() {

        Configuration configuration = new Configuration().configure();
        configuration.addAnnotatedClass(Phrase.class);
        configuration.addAnnotatedClass(datamodel.User.class);
        configuration.addAnnotatedClass(Question.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        configuration.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        configuration.setProperty("hibernate.connection.username", this.activeUser);
        configuration.setProperty("hibernate.connection.password", this.activePassword);
        configuration.setProperty("hibernate.connection.url", this.activeRemoteHost);

        this.sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());
        return sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public Connection getConnection() {
        return mainDbConn;
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
