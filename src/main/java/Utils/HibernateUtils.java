package utils;

import beans.LoginBean;
import datamodel.Phrase;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import javax.el.ELContext;
import javax.faces.context.FacesContext;

/**
 * Created by Aleks on 08.02.2017.
 */
public class HibernateUtils {

    SessionFactory sessionFactory;

    public SessionFactory buildSessionFactory() {

        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        LoginBean loginBean = (LoginBean) elContext.getELResolver().getValue(elContext, null, "login");

        if (loginBean != null) {
            try {

                Configuration configuration = new Configuration();
                configuration.addAnnotatedClass(Phrase.class);
                configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
                configuration.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
//                configuration.setProperty("hibernate.connection.username", loginBean.activeUser);
                configuration.setProperty("hibernate.connection.password", loginBean.activePassword);
                configuration.setProperty("hibernate.connection.url", loginBean.activeRemoteHost);
                configuration.setProperty(Environment.SHOW_SQL, "true");
                configuration.setProperty("hibernate.id.new_generator_mappings", "false");
                configuration.configure("hibernate.cfg.xml");

                sessionFactory = configuration.buildSessionFactory(
                        new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(
                        "There was an error during Hibernate buildSessionFactory()");
            }
        }

        return sessionFactory;
    }


}
