package aleks.kuzko.dao;

import aleks.kuzko.datamodel.Phrase;
import aleks.kuzko.datamodel.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Component;
import aleks.kuzko.utils.DataSource;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Created by Aleks on 28.03.2017.
 */
@Component
public class PhraseDao implements DaoInterface<Phrase, Long> {

    private SessionFactory currentSessionFactory = DataSource.getHibernateSessionFactory();
    private Session currentSession;
    private Transaction currentTransaction;

    public PhraseDao(){
        System.out.println("PhraseDao constructor");
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

    public void update(Phrase entity, String user) {
        getCurrentSession().update(entity);
    }

    @Override
    public Phrase findById(Long id) {
        return getCurrentSession().get(Phrase.class, id);

    }

    @Override
    public void delete(Phrase entity) {
        getCurrentSession().delete(entity);
    }


    public List<Phrase> fetchAll(String userId) {
        openCurrentSession();
        System.out.println("currentSession = " + currentSession);
        List<User> users = currentSession.createQuery("from User").list();
        CriteriaBuilder builder = currentSession.getCriteriaBuilder();
        CriteriaQuery<Phrase> criteriaQuery = builder.createQuery(Phrase.class);
        Root<Phrase> phraseRoot = criteriaQuery.from(Phrase.class);
        criteriaQuery.select(phraseRoot);
        criteriaQuery.where(builder.equal(phraseRoot.get("user").get("login"), userId), builder.equal(phraseRoot.get("isDeleted"), false));
        Query<Phrase> allPhrasesQuery = currentSession.createQuery(criteriaQuery);
        List<Phrase> phrases = allPhrasesQuery.list();
        closeCurrentSession();
        return phrases;
    }


    @Override
    public List<Phrase> fetchAll() {
        List<Phrase> phrases = getCurrentSession().createQuery("from Phrase").list();
        return phrases;
    }


    @Override
    public void deleteAll() {
        fetchAll().forEach(this::delete);
    }
}
