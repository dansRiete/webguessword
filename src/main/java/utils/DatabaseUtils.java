package utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import datamodel.Phrase;
import datamodel.Question;
import datamodel.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.beans.PropertyVetoException;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Aleks on 08.02.2017.
 */
public class DatabaseUtils {

    private final static String ORIGINAL_REMOTE_HOST = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String FORWARDED_REMOTE_HOST_PORT3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private final static String DRIVER = "com.mysql.jdbc.Driver";
    public static String activeRemoteHost;
    public static String activeUser;
    public static String activePassword;
    private static SessionFactory hibernateSessionFactory = buildHibernateSessionFactory();
    private static ComboPooledDataSource connectionPool = initConnectionPool();
    public final static boolean USE_LOCAL_DB = true;

    private static ComboPooledDataSource initConnectionPool(){
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass(DRIVER); //loads the jdbc driver
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
        cpds.setJdbcUrl(activeRemoteHost);
        cpds.setUser(activeUser);
        cpds.setPassword(activePassword);
        return cpds;
    }

    private static SessionFactory buildHibernateSessionFactory() {
        determineAliveDatabase();

        try {
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(Phrase.class);
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(Question.class);
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            configuration.setProperty("hibernate.connection.driver_class", DRIVER);
            configuration.setProperty("hibernate.connection.username", activeUser);
            configuration.setProperty("hibernate.connection.password", activePassword);
            configuration.setProperty("hibernate.connection.url", activeRemoteHost);
            configuration.setProperty(Environment.SHOW_SQL, "true");
            configuration.setProperty("hibernate.id.new_generator_mappings", "false");
            configuration.configure("hibernate.cfg.xml");

            hibernateSessionFactory = configuration.buildSessionFactory(
                    new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("There was an error during Hibernate buildHibernateSessionFactory()" + e.getLocalizedMessage());
        }

        return hibernateSessionFactory;
    }

    //TO-DO This method should not be used in production, for developing only
    private static void determineAliveDatabase() {
        System.out.println("CALL: determineAliveDatabase() from DatabaseUtils");
        String conectedDatabaseMessage = null;

        if (USE_LOCAL_DB) {
            activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
            activeUser = "root";
            activePassword = "root";
            try {
                DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                conectedDatabaseMessage = "Local virtual DB connected";
                System.out.println(conectedDatabaseMessage);
                return;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        activeUser = "adminLtuHq9R";
        activePassword = "d-AUIKakd1Br";

        try {
            activeRemoteHost = ORIGINAL_REMOTE_HOST;

            DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
            conectedDatabaseMessage = "Remote DB was connected";
        } catch (SQLException e) {
            try {
                activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
                DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                conectedDatabaseMessage = "Remote DB was connected through the local port 3306 forwarding";
            } catch (SQLException e1) {
                try {
                    activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3307;
                    DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
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

    public static SessionFactory getHibernateSessionFactory() {
        if(hibernateSessionFactory != null){
            return hibernateSessionFactory;
        }else {
            return hibernateSessionFactory = buildHibernateSessionFactory();
        }
    }

    public static ComboPooledDataSource getConnectionPool() {
        return connectionPool;
    }
}
