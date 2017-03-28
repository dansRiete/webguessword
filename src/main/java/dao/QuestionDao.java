package dao;

import datamodel.Question;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Created by Aleks on 28.03.2017.
 */
public class QuestionDao implements DaoInterface<Question, Long> {

    private SessionFactory currentSessionFactory;
    private Session currentSession;
    private Transaction currentTransaction;

    public QuestionDao(SessionFactory sessionFactory){

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
    public void persist(Question entity) {
        getCurrentSession().save(entity);
    }

    @Override
    public void update(Question entity) {
        getCurrentSession().update(entity);
    }

    @Override
    public Question findById(Long aLong) {
        return getCurrentSession().get(Question.class, aLong);

    }

    @Override
    public void delete(Question entity) {
        getCurrentSession().delete(entity);

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Question> findAll() {
        return getCurrentSession().createQuery("from Question").list();
    }

    @Override
    public void deleteAll() {
        findAll().forEach(this::delete);
    }
}
