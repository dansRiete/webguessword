package Utils;

import logic.Phrase;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/**
 * Created by Aleks on 08.02.2017.
 */
public class HibernateUtils {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure();
            configuration.addAnnotatedClass(Phrase.class);
            configuration.setProperty("hibernate.connection.username", "root");
            configuration.setProperty("hibernate.connection.password", "root");
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            configuration.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
            configuration.setProperty("hibernate.connection.url", "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false");

            return configuration.buildSessionFactory(
                    new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "There was an error building the factory");
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
