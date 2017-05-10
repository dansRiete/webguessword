package dao;

import beans.LoginBean;
import datamodel.Phrase;
import datamodel.Question;
import datamodel.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import utils.DatabaseUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Created by Aleks on 11.05.2016.
 */

public class DatabaseHelper {

    public static final String TIMEZONE = "Europe/Kiev";
    private static final double CHANCE_OF_APPEARING_TRAINED_PHRASES = 1D / 35D;
    private int activeTrainedPhrasesNumber;
    private int activeUntrainedPhrasesNumber;
    private int totalTrainedPhrasesNumber;
    private int totalUntrainedPhrasesNumber;
    private int activePhrasesNumber;
    private int greatestPhrasesIndex;
    private HashSet<String> selectedLabels;
    private List<String> availableLabels = new ArrayList<>();
    private List<Phrase> availablePhrases = new ArrayList<>();
    private Random random = new Random();
//    private LoginBean loginBean;
    private SessionFactory sessionFactory = DatabaseUtils.getHibernateSessionFactory();
    //    private QuestionDao questionDao;
//    private UserDao userDao;
    private PhraseDao phraseDao = new PhraseDao();
    private User loggedUser;
    /**
     * Number of replies to 6 am of the current day
     */
    private final int untilTodayAnswersNumber;
    /**
     * Number of hours spent from the very begining till 6am of the current day
     */
    private final int untilTodayTrainingHoursSpent;


    public DatabaseHelper(User loggedUser) {
        this.loggedUser = loggedUser;
        untilTodayAnswersNumber = calculateUntilTodayAnswersNumber();
        untilTodayTrainingHoursSpent = calculateUntilTodayTrainingHoursSpent();
        if(loggedUser != null){
            availableLabels = retrievePossibleLabels();
            reloadPhrasesAndIndices();
        }
    }

    /*public void peristQuestion(Question question){

        System.out.println("CALL: peristQuestion(Question question) from DatabaseHelper");
        questionDao.persist(question);
    }

    public void updateQuestion(Question question){

        System.out.println("CALL: updateQuestion(Question question) from DatabaseHelper");
        questionDao.update(question);
    }*/

    public List<Question> loadTodayAnsweredQuestions(){

        Session session = sessionFactory.openSession();
        ZonedDateTime todays6amDateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).withHour(6).withMinute(0).withSecond(0).withNano(0);
        Timestamp todays6amTimestamp = new Timestamp(todays6amDateTime.toEpochSecond() * 1000);
        @SuppressWarnings("JpaQlInspection")
        String queryString = "FROM Question WHERE date > :time AND user_id = :user ORDER BY date DESC";
        Query query = session.createQuery(queryString);
        query.setParameter("time", todays6amTimestamp);
        query.setParameter("user", loggedUser.getId());
        @SuppressWarnings("unchecked")
        List<Question> list = query.list();
        list.forEach(question -> question.setAnswered(true));
        return list;
    }

    public List<String> retrievePossibleLabels() {

        System.out.println("CALL: retrievePossibleLabels() from DatabaseHelper");
        List<String> availableLabels = new ArrayList<>();
        availableLabels.clear();
        String temp;
        availableLabels.add("ALL");

        try (Statement st = DatabaseUtils.getConnectionPool().getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + "(SELECT * FROM words WHERE user_id=" +
                     loggedUser.getId() + ") AS THIS_USER" + " ORDER BY LABEL")) {
            while (rs.next()) {
                temp = rs.getString("LABEL");
                if(temp != null && !temp.equals(""))
                    availableLabels.add(temp);
            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION: in retrievePossibleLabels() from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }

        return availableLabels;
    }

    public int calculateTotalPhrasesNumber(){
        return availablePhrases.size();
    }

    public void reloadPhrasesAndIndices() {

        System.out.println("CALL: reloadPhrasesAndIndices() from DatabaseHelper");

        availablePhrases.clear();
        totalTrainedPhrasesNumber = totalUntrainedPhrasesNumber = 0;

        Session session = sessionFactory.openSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Phrase> criteriaQuery = builder.createQuery(Phrase.class);
        Root<Phrase> phraseRoot = criteriaQuery.from(Phrase.class);
        criteriaQuery.select(phraseRoot);
        criteriaQuery.where(builder.equal(phraseRoot.get("user"), loggedUser),
                builder.equal(phraseRoot.get("isDeleted"), false));
        Query<Phrase> allPhrasesQuery = session.createQuery(criteriaQuery);
        availablePhrases = allPhrasesQuery.list();
        session.close();

        for(Phrase currentPhrase : availablePhrases){
            if(currentPhrase.probabilityFactor <= Phrase.TRAINED_PROBABILITY_FACTOR){
                totalTrainedPhrasesNumber++;
            }else {
                totalUntrainedPhrasesNumber++;
            }
        }
        System.out.println("availablePhrases size=" + availablePhrases.size());
        reloadIndices();
    }

    private void reloadIndices() {

        if(availablePhrases.isEmpty()){
            throw new RuntimeException("Active Phrases list was empty. Reload indices impossible");
        }

        final long RANGE = 1_000_000_000;
        long startTime = System.currentTimeMillis();
        double temp = 0;
        double indexOfTrained;     //Index of appearing learnt words
        double rangeOfUnTrained;  //Ranhe indices non learnt words
        double scaleOfOneProb;
        int modificatePhrasesIndicesNumber = 0;
        int untrainedPhrasesProbabilityFactorsSumm = 0;
        this.activePhrasesNumber = 0;
        this.activeUntrainedPhrasesNumber = 0;

        for (Phrase phr : availablePhrases) {
            phr.indexStart = phr.indexEnd = 0;
            if(phr.isInList(selectedLabels)){
                this.activePhrasesNumber++;
                if(phr.probabilityFactor > 3){
                    this.activeUntrainedPhrasesNumber++;
                    untrainedPhrasesProbabilityFactorsSumm += phr.probabilityFactor;
                }
            }
        }

        this.activeTrainedPhrasesNumber = activePhrasesNumber - activeUntrainedPhrasesNumber;
        indexOfTrained = CHANCE_OF_APPEARING_TRAINED_PHRASES / activeTrainedPhrasesNumber;
        rangeOfUnTrained = activeTrainedPhrasesNumber > 0 ? 1 - CHANCE_OF_APPEARING_TRAINED_PHRASES : 1;
        scaleOfOneProb = rangeOfUnTrained / untrainedPhrasesProbabilityFactorsSumm;

        for (Phrase currentPhrase : availablePhrases) { //Sets indices for nonlearnt words
            if(currentPhrase.isInList(selectedLabels)){
                int indexStart;
                int indexEnd;
                double prob;
                prob = currentPhrase.probabilityFactor;

                //If activeUntrainedPhrasesNumber == 0 then all words have been learnt, setting equal for all indices
                if (activeUntrainedPhrasesNumber == 0) {

                    indexStart = (int) (temp * RANGE);
                    currentPhrase.indexStart = indexStart;
                    temp += CHANCE_OF_APPEARING_TRAINED_PHRASES / activeTrainedPhrasesNumber;
                    indexEnd = (int) ((temp * RANGE) - 1);
                    currentPhrase.indexEnd = indexEnd;

                } else { //Otherwise, set indices by algorithm

                    if (prob > 3) {

                        indexStart = (int) (temp * RANGE);
                        currentPhrase.indexStart = indexStart;
                        temp += scaleOfOneProb * prob;
                        indexEnd = (int) ((temp * RANGE) - 1);
                        currentPhrase.indexEnd = indexEnd;
//                        System.out.println("Index Start = " + indexStart + ", Index End = " + indexEnd);

                    } else {

                        indexStart = (int) (temp * RANGE);
                        currentPhrase.indexStart = indexStart;
                        temp += indexOfTrained;
                        indexEnd = (int) ((temp * RANGE) - 1);
                        currentPhrase.indexEnd = indexEnd;
//                        System.out.println("Index Start = " + indexStart + ", Index End = " + indexEnd);
                    }
                }

                modificatePhrasesIndicesNumber++;
                if(modificatePhrasesIndicesNumber == activePhrasesNumber){
                    this.greatestPhrasesIndex = currentPhrase.indexEnd;
                }
            }
        }
        System.out.println("CALL: reloadIndices() from DatabaseHelper," + " Indexes changed=" + modificatePhrasesIndicesNumber + ", Time taken " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public Phrase retrieveRandomPhrase() {

        System.out.println("CALL: retrieveRandomPhrase() from DatabaseHelper");
        return retrievePhraseByIndex(random.nextInt(greatestPhrasesIndex));
    }

    //    @SuppressWarnings({"unchecked", "JpaQlInspection"})
    public long retrieveMaxPhraseId(){

        System.out.println("CALL: retrieveMaxPhraseId() from DatabaseHelper");
        long maxId = 0;
        @SuppressWarnings({"unchecked", "JpaQlInspection"})
        List<Phrase> list = sessionFactory.openSession().createQuery("from Phrase").list();
        for(Phrase currentPhrase : list){
            if(currentPhrase.getId() > maxId){
                maxId = currentPhrase.getId();
            }
        }
        return maxId;
    }

    public void insertPhrase(Phrase insertedPhrase) {

        System.out.println("CALL: insertPhrase(Phrase insertedPhrase) from DatabaseHelper");
        phraseDao.openCurrentSessionWithTransaction();
        phraseDao.persist(insertedPhrase);
        phraseDao.closeCurrentSessionwithTransaction();
        availablePhrases.add(insertedPhrase);
    }

    public void deletePhrase(Phrase deletedPhrase) {

        System.out.println("CALL: deleteButtonAction(int id) from DatabaseHelper");
        String deleteSql = "DELETE FROM words WHERE ID=" + deletedPhrase.id;
        try (Statement st = DatabaseUtils.getConnectionPool().getConnection().createStatement()) {
            st.execute(deleteSql);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in deleteButtonAction(int id) from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }
        reloadPhrasesAndIndices();

    }

    public void updatePhrase(Phrase givenPhrase) {

        System.out.println("CALL: updatePhrase(Phrase givenPhrase) from DatabaseHelper with id=" + givenPhrase.id);
        String dateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String updateSql = "UPDATE " + "words" + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                "label=?, prob_factor=?, rate=?  WHERE id =" + givenPhrase.id;

        Phrase phraseInTheCollection = retrievePhraseById(givenPhrase.id);
        phraseInTheCollection.foreignWord = givenPhrase.foreignWord;
        phraseInTheCollection.nativeWord = givenPhrase.nativeWord;
        phraseInTheCollection.transcription = givenPhrase.transcription;
        phraseInTheCollection.lastAccessDateTime = givenPhrase.lastAccessDateTime;
        phraseInTheCollection.label = givenPhrase.label;
        phraseInTheCollection.probabilityFactor = givenPhrase.probabilityFactor;
        phraseInTheCollection.multiplier = givenPhrase.multiplier;

        try (PreparedStatement mainDbPrepStat = DatabaseUtils.getConnectionPool().getConnection().prepareStatement(updateSql)) {

            mainDbPrepStat.setString(1, givenPhrase.foreignWord);
            mainDbPrepStat.setString(2, givenPhrase.nativeWord);

            if (givenPhrase.transcription == null || givenPhrase.transcription.equalsIgnoreCase(""))
                mainDbPrepStat.setString(3, null);
            else
                mainDbPrepStat.setString(3, givenPhrase.transcription);

            mainDbPrepStat.setString(4, dateTime);

            if (givenPhrase.label == null || givenPhrase.label.equalsIgnoreCase(""))
                mainDbPrepStat.setString(5, null);
            else
                mainDbPrepStat.setString(5, givenPhrase.label);

            mainDbPrepStat.setDouble(6, givenPhrase.probabilityFactor);
            mainDbPrepStat.setDouble(7, givenPhrase.multiplier);
            mainDbPrepStat.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase givenPhrase) from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }

        reloadIndices();

    }

    public void updateProb(Phrase phrase) {

        System.out.println("CALL: updateProb(Phrase insertedPhrase) with id=" + phrase.id + " from DatabaseHelper");
        String dateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        retrievePhraseById(phrase.id).probabilityFactor = phrase.probabilityFactor;
        retrievePhraseById(phrase.id).multiplier = phrase.multiplier;

        try (Statement statement = DatabaseUtils.getConnectionPool().getConnection().createStatement()) {
            statement.execute("UPDATE " + "words" + " SET prob_factor=" + phrase.probabilityFactor + ", last_accs_date='" +
                    dateTime + "', rate=" + phrase.multiplier +
                    " WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("SQLException in updateProb(Phrase insertedPhrase) from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException("SQLException in updateProb(Phrase insertedPhrase) from DatabaseHelper");
        }
        reloadIndices();
    }

    @SuppressWarnings("Duplicates")
    private int calculateUntilTodayAnswersNumber(){

        System.out.println("CALL: calculateUntilTodayAnswersNumber() from DatabaseHelper");
        int untilTodayAnswersNumber = 0;
        try {
            Statement statement = DatabaseUtils.getConnectionPool().getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM questions WHERE date < " +
                    "DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)");
            resultSet.next();
            untilTodayAnswersNumber = resultSet.getInt(1);
            statement.close();
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return untilTodayAnswersNumber;
    }

    @SuppressWarnings("Duplicates")
    private int calculateUntilTodayTrainingHoursSpent(){

        System.out.println("CALL: calculateUntilTodayTrainingHoursSpent() from DatabaseHelper");
        int untilTodayTrainingHoursSpent = 0;
        try{
            Statement statement = DatabaseUtils.getConnectionPool().getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT TIMESTAMPDIFF(HOUR, (SELECT MIN(date) FROM questions " +
                    "WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)), DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR))");
            resultSet.next();
            untilTodayTrainingHoursSpent = resultSet.getInt(1);
            statement.close();
            resultSet.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
        return untilTodayTrainingHoursSpent;
    }

    private Phrase retrievePhraseById(long id){

        System.out.println("CALL: retrievePhraseById(long id) from DatabaseHelper");
        for (Phrase phrase : availablePhrases) {
            if (phrase.id == id)
                return phrase;
        }
        throw new RuntimeException("PhraseNotFoundException");
    }

    private Phrase retrievePhraseByIndex(int index) {

        System.out.println("CALL: retrievePhraseByIndex(int index) from DatabaseHelper");
        for (Phrase phrase : availablePhrases) {
            if (phrase.isInList(selectedLabels) && index >= phrase.indexStart && index <= phrase.indexEnd) {
                return phrase;
            }
        }
        throw new RuntimeException("There was no insertedPhrase by given index " + index);
    }

    public List<Phrase> retrieveActivePhrases() {
        List<Phrase> activePhrasesList = new ArrayList<>();
        availablePhrases.forEach(phrase -> {
            if (phrase.isInList(selectedLabels)){
                activePhrasesList.add(phrase);
            }
        });
        return activePhrasesList;
    }

    //Setters and Getters

    public void setSelectedLabels(HashSet<String> selectedLabels) {
        this.selectedLabels = selectedLabels;
    }

    public int getActiveTrainedPhrasesNumber() {
        return activeTrainedPhrasesNumber;
    }

    public int getActiveUntrainedPhrasesNumber() {
        return activeUntrainedPhrasesNumber;
    }

    public List<String> getAvailableLabels() {
        return availableLabels;
    }

    public int getUntilTodayTrainingHoursSpent() {
        return untilTodayTrainingHoursSpent;
    }

    public int getUntilTodayAnswersNumber() {
        return untilTodayAnswersNumber;
    }

    public int getGreatestPhrasesIndex() {
        return greatestPhrasesIndex;
    }

    public int getTotalUntrainedPhrasesNumber() {
        return totalUntrainedPhrasesNumber;
    }

    public int getTotalTrainedPhrasesNumber() {
        return totalTrainedPhrasesNumber;
    }

    public int getActivePhrasesNumber() {
        return activePhrasesNumber;
    }
}

