package logic;

import beans.LoginBean;
import dao.PhraseDao;
import dao.QuestionDao;
import dao.UserDao;
import datamodel.Phrase;
import datamodel.Question;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

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
//@SuppressWarnings("SqlResolve")
public class DatabaseHelper {

    private static final String TIMEZONE = "Europe/Kiev";
    private static final double CHANCE_OF_APPEARING_LEARNT_WORDS = 1d / 15d;
    private HashSet<String> activeLabels;
    private ArrayList<String> allAvailableLabels = new ArrayList<>();
    private ArrayList<Phrase> activePhrases = new ArrayList<>();
    private List<Phrase> allAvailablePhrases = new ArrayList<>();
    private int learntWordsAmount;
    private int nonLearntWordsAmount;
    private int totalActiveWordsAmount;
    private int learntWordsProbSumm; //unused since 25/03/2017
    private int theGreatestPhrasesIndex;
    private int totalTrainingAnswers; //Number of replies to 6 am of the current day
    private int totalTrainingHoursSpent; // Number of hours spent from the very begining till 6am of the current day
    private int lastSevenStackIndex;
    private Phrase[] lastSevenPhrasesStack;
    private Random random = new Random();
    private Connection mainDbConn;
    private LoginBean loginBean;
    private SessionFactory sessionFactory;
    private QuestionDao questionDao;
    private UserDao userDao;
    private PhraseDao phraseDao;


    public DatabaseHelper(LoginBean loginBean, SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.loginBean = loginBean;
        mainDbConn = loginBean.getConnection();
        questionDao = new QuestionDao(sessionFactory);
        userDao = new UserDao(sessionFactory);
        phraseDao = new PhraseDao(sessionFactory);
        reloadPhrasesCollection();
        retievePossibleLabels();
        initThisDayStatistics();
    }

    public void setStatistics(Phrase givenPhrase){
        String dateTime = givenPhrase.phraseAppearingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        int mlseconds = Integer.parseInt(ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("SSS")));

        String mode;
        if(givenPhrase.hasBeenAnsweredCorrectly)
            mode = "r_answ";
        else
            mode = "w_answ";

        int learnt = 0;
        if(givenPhrase.isTrained() && !givenPhrase.wasTrainedBeforeAnswer())
            learnt = 1;
        else if(!givenPhrase.isTrained() && givenPhrase.wasTrainedBeforeAnswer())
            learnt = -1;

        try (Statement statement = mainDbConn.createStatement()) {
            String sql = "INSERT INTO " + "statistics" + " VALUES ('" + dateTime + "', " + mlseconds +
                    ", '" + mode + "', " + givenPhrase.id + ", " + learnt + ")";

//            System.out.println(sql);
            statement.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in setStatistics() from DatabaseHelper");
        }
    }

    public void updateStatistics(Phrase phr){
        String dateTime = phr.phraseAppearingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        String mode;
        if(phr.hasBeenAnsweredCorrectly){
            mode = "r_answ";
        }else{
            mode = "w_answ";
        }

        int learnt = 0;
        if(phr.isTrained() && !phr.wasTrainedBeforeAnswer())
            learnt = 1;
        else if(!phr.isTrained() && phr.wasTrainedBeforeAnswer())
            learnt = -1;

        try (Statement statement = mainDbConn.createStatement()) {
            String sql = "UPDATE " + "statistics" + " SET event='" + mode +", learnt=" + learnt +  "' WHERE date='" + dateTime +"' AND id=" + phr.id;
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in updateStatistics from DatabaseHelper ");
        }
    }

    public ArrayList<Question> retrieveTodayQuestions(){
        ArrayList<Question> list = new ArrayList<>();

        /*try (Statement statement = mainDbConn.createStatement();
             ResultSet rs = statement.executeQuery
                     ("SELECT * FROM " + "statistics" + " WHERE " +
                             "date > DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR) ORDER BY DATE , ms")) {

            while (rs.next()){
                Phrase currentPhrase = null;

                    currentPhrase = getPhraseById(rs.getInt("id"));
                    currentPhrase.hasBeenAnswered = true;
                    if(rs.getString("event").equalsIgnoreCase("r_answ")) {
                        currentPhrase.hasBeenAnsweredCorrectly = true;
                    } else {
                        currentPhrase.hasBeenAnsweredCorrectly = false;
                    }
                    currentPhrase.phraseAppearingTime = rs.getTimestamp("date").toLocalDateTime().atZone(ZoneId.of("Europe/Helsinki"));
                    list.add(currentPhrase);


            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in setStatistics() from DatabaseHelper");
        }*/

        return list;
    }

    public List<String> retievePossibleLabels() {
        //Returns list of possible labels
        System.out.println("CALL: retievePossibleLabels() from DatabaseHelper");
        allAvailableLabels.clear();
        String temp;

        try (Statement st = mainDbConn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + "words" + " ORDER BY LABEL")) {
            while (rs.next()) {
                temp = rs.getString("LABEL");
                allAvailableLabels.add(temp == null ? "null" : temp);
            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION: in retievePossibleLabels() from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }

        return allAvailableLabels;

    }

    public int totalWordsNumber(){
        return allAvailablePhrases.size();
    }

    public int activePhrasesNumber(){
        return activePhrases.size();
    }

    public void reloadPhrasesCollection() {

        activePhrases.clear();
        allAvailablePhrases.clear();

        Session session = sessionFactory.openSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Phrase> criteriaQuery = builder.createQuery(Phrase.class);
        Root<Phrase> phraseRoot = criteriaQuery.from(Phrase.class);
        criteriaQuery.select(phraseRoot);
        System.out.println("user=" + loginBean.getUser());
        criteriaQuery.where(builder.equal(phraseRoot.get("owner"), 1), builder.equal(phraseRoot.get("isDeleted"), false));
//        criteriaQuery.where(builder.equal(phraseRoot.get("isDeleted"), false));
        Query<Phrase> allPhrasesQuery = session.createQuery(criteriaQuery);
        allAvailablePhrases = allPhrasesQuery.list();
        session.close();
        System.out.println("LIST SIZE:" + allAvailablePhrases.size());
        for(Phrase currentPhrase : allAvailablePhrases){
            currentPhrase.setDatabaseHelper(this);
            if(currentPhrase.isInList(activeLabels)){
                activePhrases.add(currentPhrase);
            }
        }
        reloadIndices();
    }

    public Phrase retrieveRandomPhrase() {

        Phrase retrievedPhrase;

        do {
            retrievedPhrase = getPhraseByIndex(random.nextInt(theGreatestPhrasesIndex));
        } while (lastPhrasesStackContains(retrievedPhrase));

        pushToLastPhrasesStack(retrievedPhrase);
        retrievedPhrase.resetPreviousValues();
        return retrievedPhrase;
    }

    public static Timestamp toTimestamp(ZonedDateTime dateTime) {
        if(dateTime == null){
            return null;
        }
        return new Timestamp(dateTime.toInstant().getEpochSecond() * 1000L);
    }

    public void insertPhrase(Phrase phrase) {
        System.out.println("CALL: insertPhrase(Phrase phrase) from DatabaseHelper");
        String insertSql = "INSERT INTO " + "words" + " (for_word, nat_word, transcr, prob_factor, create_date," +
                " label, last_accs_date, rate, user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = mainDbConn.prepareStatement(insertSql)) {
            ps.setString(1, phrase.foreignWord);
            ps.setString(2, phrase.nativeWord);
            ps.setString(3, phrase.transcription);
            ps.setDouble(4, phrase.probabilityFactor);
            ps.setTimestamp(5, toTimestamp(phrase.collectionAddingDateTime));
            ps.setString(6, phrase.label);
            ps.setTimestamp(7, toTimestamp(phrase.lastAccessDateTime));
            ps.setDouble(8, phrase.multiplier);
            ps.setString(9, loginBean.getUser());
            ps.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION inside: in insertPhrase(Phrase phrase) from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }
        activePhrases.add(phrase);
//        reloadPhrasesCollection();

    }

    public void deletePhrase(Phrase phr) {
        System.out.println("CALL: deletePhrase(int id) from DatabaseHelper");
        String deleteSql = "DELETE FROM " + loginBean.getUser() + " WHERE ID=" + phr.id;
        try (Statement st = mainDbConn.createStatement()) {
            st.execute(deleteSql);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in deletePhrase(int id) from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }
        activePhrases.remove(phr);

    }

    public void updatePhrase(Phrase givenPhrase) {
        System.out.println("CALL: update(Phrase givenPhrase) from DatabaseHelper with id=" + givenPhrase.id);
        String dateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String updateSql = "UPDATE " + "words" + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                "label=?, prob_factor=?, rate=?  WHERE id =" + givenPhrase.id;

        Phrase phraseInTheCollection = getPhraseById(givenPhrase.id);

        phraseInTheCollection.foreignWord = givenPhrase.foreignWord;
        phraseInTheCollection.nativeWord = givenPhrase.nativeWord;
        phraseInTheCollection.transcription = givenPhrase.transcription;
        phraseInTheCollection.lastAccessDateTime = givenPhrase.lastAccessDateTime;
        phraseInTheCollection.label = givenPhrase.label;
        phraseInTheCollection.probabilityFactor = givenPhrase.probabilityFactor;
        phraseInTheCollection.multiplier = givenPhrase.multiplier;

        try (PreparedStatement mainDbPrepStat = mainDbConn.prepareStatement(updateSql)) {

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

//        reloadPhrasesCollection();

    }

    public void updateProb(Phrase phrase) {
        System.out.println("CALL: updateProb(Phrase phrase) with id=" + phrase.id + " from DatabaseHelper");
        String dateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        getPhraseById(phrase.id).probabilityFactor = phrase.probabilityFactor;
        getPhraseById(phrase.id).multiplier = phrase.multiplier;

        try (Statement st = mainDbConn.createStatement()) {
            st.execute("UPDATE " + "words" + " SET prob_factor=" + phrase.probabilityFactor + ", last_accs_date='" + dateTime + "', rate=" + phrase.multiplier +
                    " WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DatabaseHelper");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public ArrayList<Phrase> getActivePhrases() {

        return new ArrayList<>(activePhrases);

    }

    private void initThisDayStatistics(){
        try(
                ResultSet rs1 = mainDbConn.createStatement().executeQuery
                        ("SELECT COUNT(*) FROM statistics WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)");
                ResultSet rs2 = mainDbConn.createStatement().executeQuery(
                        "SELECT TIMESTAMPDIFF(HOUR, (SELECT MIN(date) FROM statistics WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)), DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR))")
        ){
            rs1.next();
            totalTrainingAnswers = rs1.getInt(1);
            rs2.next();
            totalTrainingHoursSpent = rs2.getInt(1);

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void reloadIndices() {

        if(activePhrases.isEmpty()){
            throw new RuntimeException("Active Phrases list was empty. Reload indices impossible");
        }

        final long RANGE = 1_000_000_000;
        long start = System.currentTimeMillis();
        double temp = 0;
        double indexOfLearnt;     //Index of appearing learnt words
        double rangeOfNonlearnt;  //Ranhe indices non learnt words
        double scaleOfOneProb;
        int countOfModIndices = 0;
        int nonLearntWordsProbSumm = 0;
        totalActiveWordsAmount = activePhrases.size();
        nonLearntWordsAmount = 0;
        learntWordsProbSumm = 0;

        for (Phrase phr : activePhrases) {
            phr.indexStart = phr.indexEnd = 0;
            if (phr.probabilityFactor > 3) {
                nonLearntWordsAmount++;
                nonLearntWordsProbSumm += phr.probabilityFactor;
            }else {
                learntWordsProbSumm += phr.probabilityFactor;
            }
        }

        learntWordsAmount = totalActiveWordsAmount - nonLearntWordsAmount;
        indexOfLearnt = CHANCE_OF_APPEARING_LEARNT_WORDS / learntWordsAmount;
        rangeOfNonlearnt = learntWordsAmount > 0 ? 1 - CHANCE_OF_APPEARING_LEARNT_WORDS : 1;
        scaleOfOneProb = rangeOfNonlearnt / nonLearntWordsProbSumm;


        for (Phrase currentPhrase : activePhrases) { //Sets indices for nonlearnt words
            int indexStart;
            int indexEnd;
            double prob;
            prob = currentPhrase.probabilityFactor;

            //If nonLearntWordsAmount == 0 then all words have been learnt, setting equal for all indices
            if (nonLearntWordsAmount == 0) {

                indexStart = (int) (temp * RANGE);
                currentPhrase.indexStart = indexStart;
                temp += CHANCE_OF_APPEARING_LEARNT_WORDS / learntWordsAmount;
                indexEnd = (int) ((temp * RANGE) - 1);
                currentPhrase.indexEnd = indexEnd;

            } else { //Otherwise, set indices by algorithm

                if (prob > 3) {

                    indexStart = (int) (temp * RANGE);
                    currentPhrase.indexStart = indexStart;
                    temp += scaleOfOneProb * prob;
                    indexEnd = (int) ((temp * RANGE) - 1);
                    currentPhrase.indexEnd = indexEnd;

                } else {

                    indexStart = (int) (temp * RANGE);
                    currentPhrase.indexStart = indexStart;
                    temp += indexOfLearnt;
                    indexEnd = (int) ((temp * RANGE) - 1);
                    currentPhrase.indexEnd = indexEnd;
                }
            }

            countOfModIndices++;
            if(countOfModIndices== activePhrases.size()){
                theGreatestPhrasesIndex = currentPhrase.indexEnd;
            }
        }

        System.out.println("CALL: reloadIndices() from DatabaseHelper" + "Indexes changed=" + countOfModIndices + " Time taken " + (System.currentTimeMillis() - start) + "ms");
    }

    private Phrase getPhraseById(long id){
        for (Phrase phrase : allAvailablePhrases) {
            if (phrase.id == id)
                return phrase;
        }
        throw new RuntimeException("PhraseNotFoundException");
    }

    private Phrase getPhraseByIndex(int index) {
        for (Phrase phrase : activePhrases) {
            if (index >= phrase.indexStart && index <= phrase.indexEnd) {
                return phrase;
            }
        }
        throw new RuntimeException("There was no phrase by given index " + index);
    }

    /**
     * Corrects phrases stack size. Phrases stack size can not be
     * greater than a total active number of phrases curently trained
     */
    private void adjustLastPhrasesStackSize(){
        if (lastSevenPhrasesStack == null || lastSevenPhrasesStack.length > totalActiveWordsAmount) {
            if (totalActiveWordsAmount >= 7) {
                lastSevenPhrasesStack = new Phrase[7];
                lastSevenStackIndex = 0;
            }else {
                lastSevenPhrasesStack = new Phrase[0];
            }
        }
    }

    private int lastPhrasesStackPosition(){
        lastSevenStackIndex = lastSevenStackIndex > lastSevenPhrasesStack.length - 1 ? lastSevenStackIndex = 0 : lastSevenStackIndex;
        return lastSevenStackIndex++;
    }

    private void pushToLastPhrasesStack(Phrase pushedPhrase) {

        adjustLastPhrasesStackSize();

        if (!lastPhrasesStackContains(pushedPhrase) && lastSevenPhrasesStack != null && lastSevenPhrasesStack.length != 0) {
            lastSevenPhrasesStack[lastPhrasesStackPosition()] = pushedPhrase;
        }
    }

    private boolean lastPhrasesStackContains(Phrase checkedPhrase){
        if(checkedPhrase == null){
            throw new IllegalArgumentException("Given phrase was null");
        }
        adjustLastPhrasesStackSize();
        for (Phrase currentPhraseFromStack : lastSevenPhrasesStack) {
            if (currentPhraseFromStack != null && currentPhraseFromStack.id == checkedPhrase.id) {
                return true;
            }
        }
        return false;
    }

    //Setters and Getters

    public void setActiveLabels(HashSet<String> activeLabels) {
        this.activeLabels = activeLabels;
    }

    public int getLearntWordsAmount() {
        return learntWordsAmount;
    }

    public int getNonLearntWordsAmount() {
        return nonLearntWordsAmount;
    }

    public ArrayList<String> getAllAvailableLabels() {
        return allAvailableLabels;
    }

    public int getTotalTrainingHoursSpent() {
        return totalTrainingHoursSpent;
    }

    public int getTotalTrainingAnswers() {
        return totalTrainingAnswers;
    }
}

