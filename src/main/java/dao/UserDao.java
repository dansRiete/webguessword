package dao;

import datamodel.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Created by Aleks on 28.03.2017.
 */
public class UserDao implements DaoInterface<User, Long> {

    private SessionFactory currentSessionFactory;
    private Session currentSession;
    private Transaction currentTransaction;

    public UserDao(SessionFactory sessionFactory){
        this.currentSessionFactory = sessionFactory;
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

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findAll() {
        return getCurrentSession().createQuery("from User").list();
    }

    @Override
    public void deleteAll() {
        findAll().forEach(this::delete);
    }
}
