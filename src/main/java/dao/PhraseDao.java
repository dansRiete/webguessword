package dao;

import datamodel.Phrase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Created by Aleks on 28.03.2017.
 */
public class PhraseDao implements DaoInterface<Phrase, Long> {

    private SessionFactory currentSessionFactory;
    private Session currentSession;
    private Transaction currentTransaction;

    public PhraseDao(SessionFactory sessionFactory){
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
    public void persist(Phrase entity) {
        getCurrentSession().save(entity);
    }

    @Override
    public void update(Phrase entity) {
        getCurrentSession().update(entity);
    }

    @Override
    public Phrase findById(Long aLong) {
        return getCurrentSession().get(Phrase.class, aLong);

    }

    @Override
    public void delete(Phrase entity) {
        getCurrentSession().delete(entity);

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Phrase> findAll() {
        return getCurrentSession().createQuery("from Question").list();
    }

    @Override
    public void deleteAll() {
        findAll().forEach(this::delete);
    }
}
