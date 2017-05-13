package aleks.kuzko.dao;

import aleks.kuzko.datamodel.Question;
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
public class QuestionDao implements DaoInterface<Question, Long> {

    private SessionFactory currentSessionFactory= DataSource.getHibernateSessionFactory();
    private Session currentSession;
    private Transaction currentTransaction;

    public QuestionDao(){
        System.out.println("QuestionDao constructor");
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
        openCurrentSessionWithTransaction();
        getCurrentSession().save(entity);
        closeCurrentSessionwithTransaction();
    }

    @Override
    public void update(Question entity) {
        openCurrentSessionWithTransaction();
        getCurrentSession().update(entity);
        closeCurrentSessionwithTransaction();
    }

    @Override
    public Question findById(Long aLong) {
        return getCurrentSession().get(Question.class, aLong);

    }

    @Override
    public void delete(Question entity) {
        getCurrentSession().delete(entity);

    }

    @Override
    public List<Question> fetchAll() {
        return getCurrentSession().createQuery("from Question").list();
    }

    @Override
    public void deleteAll() {
        fetchAll().forEach(this::delete);
    }
}
