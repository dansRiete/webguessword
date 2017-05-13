package aleks.kuzko.dao;

import aleks.kuzko.datamodel.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Component;
import aleks.kuzko.utils.DataSource;


import java.util.List;

/**
 * Created by Aleks on 28.03.2017.
 */
@Component
public class UserDao implements DaoInterface<User, Long> {

    private SessionFactory currentSessionFactory= DataSource.getHibernateSessionFactory();
    private Session currentSession;
    private Transaction currentTransaction;

    public UserDao(){
        System.out.println("UserDAO constructor");
    }

    public Session openCurrentSession(){
        currentSession = currentSessionFactory.openSession();
        return currentSession;
    }

    public Session openCurrentSessionWithTransaction(){
        currentSession = currentSessionFactory.openSession();
        currentTransaction = currentSession.beginTransaction();
        return currentSession;
    }

    public void closeCurrentSession() {
        currentSession.close();
    }

    public void closeCurrentSessionwithTransaction() {
        currentTransaction.commit();
        currentSession.close();
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public Transaction getCurrentTransaction() {
        return currentTransaction;
    }

    @Override
    public void persist(User entity) {
        getCurrentSession().save(entity);
    }

    @Override
    public void update(User entity) {
        getCurrentSession().update(entity);
    }

    @Override
    public User findById(Long aLong) {
        return getCurrentSession().get(User.class, aLong);
    }

    @Override
    public void delete(User entity) {
        getCurrentSession().delete(entity);
    }

    @Override
    public List<User> fetchAll() {
        return getCurrentSession().createQuery("from User").list();
    }

    @Override
    public void deleteAll() {
        fetchAll().forEach(this::delete);
    }
}
