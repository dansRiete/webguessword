package aleks.kuzko.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import aleks.kuzko.datamodel.Phrase;
import aleks.kuzko.datamodel.Question;
import aleks.kuzko.datamodel.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.beans.PropertyVetoException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by Aleks on 08.02.2017.
 */
public class DataSource {

    private final static String FORWARDED_REMOTE_HOST_PORT3306 = "jdbc:mysql://localhost:3306/webguessword?autoReconnect=true&useSSL=false";//?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false
    private final static String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static String activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
    private static String activeUser = "root";
    private static String activePassword = "root";
    private static ComboPooledDataSource connectionPool = initConnectionPool();
    private static SessionFactory hibernateSessionFactory = buildHibernateSessionFactory();
    private final static boolean USE_LOCAL_DB = true;

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
//        determineAliveDatabase();

//        try {
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(Phrase.class);
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(Question.class);
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            configuration.setProperty("hibernate.connection.driver_class", DRIVER);
            configuration.setProperty("hibernate.connection.username", activeUser);
            configuration.setProperty("hibernate.connection.password", activePassword);
            configuration.setProperty("hibernate.connection.url", activeRemoteHost);
//            aleks.kuzko.configuration.setProperty(Environment.SHOW_SQL, "true");
            configuration.setProperty("hibernate.id.new_generator_mappings", "false");
            configuration.configure("hibernate.cfg.xml");
            System.out.println("1 " + activeUser + " " + activePassword + " " + activeRemoteHost);

            hibernateSessionFactory = configuration.buildSessionFactory(
                    new StandardServiceRegistryBuilder()
                            .applySettings(configuration.getProperties())
                            .build());

//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("There was an error during Hibernate buildHibernateSessionFactory()" + e.getLocalizedMessage());
//        }

        return hibernateSessionFactory;
    }

    //TO-DO This method should not be used in production, for developing only
    /*private static void determineAliveDatabase() {
        System.out.println("CALL: determineAliveDatabase() from DataSource");
        String conectedDatabaseMessage = null;
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (USE_LOCAL_DB) {
            activeRemoteHost = FORWARDED_REMOTE_HOST_PORT3306;
            activeUser = "root";
            activePassword = "vlenaf13";
            try {
                DriverManager.getConnection(activeRemoteHost, activeUser, activePassword);
                conectedDatabaseMessage = "Local virtual DB connected";
                System.out.println(conectedDatabaseMessage);
                return;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }*/

    public static SessionFactory getHibernateSessionFactory() {
//        if(hibernateSessionFactory != null){
            return hibernateSessionFactory;
//        }else {
//            return hibernateSessionFactory = buildHibernateSessionFactory();
//        }
    }

    public static ComboPooledDataSource getConnectionPool() {
        return connectionPool;
    }
}
