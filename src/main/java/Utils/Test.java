package Utils;

import logic.Phrase;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Created by Aleks on 08.02.2017.
 */
public class Test {
    public static void main(String[] args) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Query<Phrase> queryAllPhrases = session.createQuery("from Phrase");
        List<Phrase> allPhrases = queryAllPhrases.list();
        for(Phrase currentPhrase : allPhrases){
            System.out.println(currentPhrase);
        }
        session.close();
        HibernateUtils.getSessionFactory().close();

    }
}
