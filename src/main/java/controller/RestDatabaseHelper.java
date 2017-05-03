package controller;

import dao.PhraseDao;
import dao.QuestionDao;
import dao.UserDao;
import datamodel.Phrase;
import datamodel.Question;
import datamodel.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Aleks on 02.05.2017.
 */
@Component
public class RestDatabaseHelper {

    private Connection mainDbConn;
    public String activeRemoteHost;
    public String activeUser;
    public String activePassword;
    private final static String ORIGINAL_REMOTE_HOST = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static boolean USE_LOCAL_DB = true;
    private SessionFactory sessionFactory;
    private QuestionDao questionDao;
    private UserDao userDao;
    private PhraseDao phraseDao;

    RestDatabaseHelper(){
        determineAliveDbAndConnectTo();
        this.sessionFactory = buildSessionFactory();
        questionDao = new QuestionDao(sessionFactory);
        userDao = new UserDao(sessionFactory);
        phraseDao = new PhraseDao(sessionFactory);
    }

    @SuppressWarnings({"unchecked", "JpaQlInspection"})
    public List<Phrase> fetchAllPhrases(long userId){
        Session session = phraseDao.openCurrentSession();
        List<User> users = session.createQuery("from User").list();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Phrase> criteriaQuery = builder.createQuery(Phrase.class);
        Root<Phrase> phraseRoot = criteriaQuery.from(Phrase.class);
        criteriaQuery.select(phraseRoot);
        criteriaQuery.where(builder.equal(phraseRoot.get("user").get("id"), userId), builder.equal(phraseRoot.get("isDeleted"), false));
        Query<Phrase> allPhrasesQuery = session.createQuery(criteriaQuery);
        List<Phrase> phrases = allPhrasesQuery.list();
        phraseDao.closeCurrentSession();
        return phrases;
    }

    @SuppressWarnings("Duplicates")
    private SessionFactory buildSessionFactory() {

        System.out.println("CALL: buildSessionFactory() from RestDatabaseHelper");
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
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties())
                        .build()
            );

        return sessionFactory;
    }

    @SuppressWarnings("Duplicates")
    private void determineAliveDbAndConnectTo() {

        System.out.println("CALL: determineAliveDbAndConnectTo() from RestDatabaseHelper");
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
}
