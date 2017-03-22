package logic;

import Utils.HibernateUtils;
import beans.LoginBean;
import datamodel.Phrase;
import Exceptions.PhraseNotFoundException;
import org.hibernate.query.Query;

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
@SuppressWarnings("SqlResolve")
public class DAO {

    private final static String TIMEZONE = "Europe/Kiev";
    public static final double CHANCE_OF_APPEARING_LEARNT_WORDS = 1d / 15d;

    public HashSet<String> chosedLabels;
    public ArrayList<String> availableLabels = new ArrayList<>();
    public double learntWords;
    public double nonLearnedWords;
    public double totalActiveWordsAmount;
    public int nonLearnedWordsProbSumm;
    public int learnedWordsProbSumm;
    private int maxPossibleAppearingIndex;
    private Random random = new Random();
    private Phrase[] lastSevenPhrasesStack;
    private int lastSevenStackCurrentPosition;
    public Connection mainDbConn;
    private LoginBean loginBean;
    private ArrayList<Phrase> activePhrases = new ArrayList<>();
    private List<Phrase> allPhrases = new ArrayList<>();
    public int answUntil6amAmount; //Number of replies to 6 am of the current day
    public int totalHoursUntil6am; // Number of hours spent from the very begining till 6am of the current day

    public DAO(LoginBean loginBean) {

        this.loginBean = loginBean;
        mainDbConn = loginBean.getConnection();
        checkDbForExistingAllTables();
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
            System.out.println("EXCEPTION: SQLException in setStatistics() from DAO");
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
            System.out.println("EXCEPTION: SQLException in updateStatistics from DAO ");
        }
    }

    public ArrayList<Phrase> retrieveTodayAnsweredPhrases(){
        ArrayList<Phrase> list = new ArrayList<>();

        try (Statement statement = mainDbConn.createStatement();
             ResultSet rs = statement.executeQuery
                     ("SELECT * FROM " + "statistics" + " WHERE " +
                             "date > DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR) ORDER BY DATE , ms")) {

            while (rs.next()){
                Phrase currentPhrase = null;
                try {
                    currentPhrase = getPhraseById(rs.getInt("id"));
                    currentPhrase.hasBeenAnswered = true;
                    if(rs.getString("event").equalsIgnoreCase("r_answ")) {
                        currentPhrase.hasBeenAnsweredCorrectly = true;
                    } else {
                        currentPhrase.hasBeenAnsweredCorrectly = false;
                    }
                    currentPhrase.phraseAppearingTime = rs.getTimestamp("date").toLocalDateTime().atZone(ZoneId.of("Europe/Helsinki"));
                    list.add(currentPhrase);

                } catch (PhraseNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in setStatistics() from DAO");
        }

        return list;
    }

    public List<String> retievePossibleLabels() {
        //Returns list of possible labels
        System.out.println("CALL: retievePossibleLabels() from DAO");
        availableLabels.clear();
        String temp;

        try (Statement st = mainDbConn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + "words" + " ORDER BY LABEL")) {
            while (rs.next()) {
                temp = rs.getString("LABEL");
                availableLabels.add(temp == null ? "null" : temp);
            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION: in retievePossibleLabels() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        return availableLabels;

    }

    public int totalWordsNumber(){
        return allPhrases.size();
    }

    public int activePhrasesNumber(){
        return activePhrases.size();
    }

    public void reloadPhrasesCollection() {

        HibernateUtils hibernateUtils = new HibernateUtils();

        activePhrases.clear();
        allPhrases.clear();

        Query<Phrase> allPhrasesQuery = hibernateUtils.buildSessionFactory().openSession().createQuery("from Phrase");
        allPhrases = allPhrasesQuery.list();
        for(Phrase currentPhrase : allPhrases){
            currentPhrase.setDao(this);
            if(currentPhrase.isThisPhraseInList(chosedLabels)){
                activePhrases.add(currentPhrase);
            }
        }
        reloadIndices(1);
    }

    private long[] reloadIndices(int id) {

        final long RANGE = 1000000000;
        long start = System.currentTimeMillis();
        double temp = 0;
        double indexOfLearnt;     //Index of appearing learnt words
        double rangeOfNonlearnt;  //Ranhe indices non learnt words
        double scaleOfOneProb;
        int countOfModIndices = 0;
        long[] indexes = new long[2];
        totalActiveWordsAmount = activePhrases.size();
        //Count nonlearnt words, nonLearnedWordsProbSumm and clear indices
        nonLearnedWords = 0;
        nonLearnedWordsProbSumm = 0;
        learnedWordsProbSumm = 0;

        for (Phrase phr : activePhrases) {
            phr.indexStart = phr.indexEnd = 0;
            if (phr.probabilityFactor.doubleValue() > 3) {
                nonLearnedWords++;
                nonLearnedWordsProbSumm += phr.probabilityFactor.doubleValue();
            }else {
                learnedWordsProbSumm += phr.probabilityFactor.doubleValue();
            }
        }

        //Count learntWords
        learntWords = totalActiveWordsAmount - nonLearnedWords;
        indexOfLearnt = CHANCE_OF_APPEARING_LEARNT_WORDS / learntWords;
        rangeOfNonlearnt = learntWords > 0 ? 1 - CHANCE_OF_APPEARING_LEARNT_WORDS : 1;
        scaleOfOneProb = rangeOfNonlearnt / nonLearnedWordsProbSumm;

        for (Phrase currentPhrase : activePhrases) { //Sets indices for nonlearnt words
            long indexStart;
            long indexEnd;
            double prob;
            prob = currentPhrase.probabilityFactor.doubleValue();

            //If nonLearnedWords == 0 then all words have been learnt, setting equal for all indices
            if (nonLearnedWords == 0) {

                indexStart = Math.round(temp * RANGE);
                currentPhrase.indexStart = indexStart;
                temp += CHANCE_OF_APPEARING_LEARNT_WORDS / learntWords;
                indexEnd = Math.round((temp * RANGE) - 1);
                currentPhrase.indexEnd = indexEnd;

            } else { //Otherwise, set indices by algorithm

                if (prob > 3) {

                    indexStart = Math.round(temp * RANGE);
                    currentPhrase.indexStart = indexStart;
                    temp += scaleOfOneProb * prob;
                    indexEnd = Math.round((temp * RANGE) - 1);
                    currentPhrase.indexEnd = indexEnd;

                } else {

                    indexStart = Math.round(temp * RANGE);
                    currentPhrase.indexStart = indexStart;
                    temp += indexOfLearnt;
                    indexEnd = Math.round((temp * RANGE) - 1);
                    currentPhrase.indexEnd = indexEnd;

                }
            }

            countOfModIndices++;
            if(countOfModIndices== activePhrases.size()){
                maxPossibleAppearingIndex = (int) currentPhrase.indexEnd;
            }
            if (currentPhrase.id == id) {
                indexes[0] = indexStart;
                indexes[1] = indexEnd;
            }
        }

        System.out.println("CALL: reloadIndices() from DAO" + "Indexes changed=" + countOfModIndices + " Time taken " + (System.currentTimeMillis() - start) + "ms");
        return indexes;
    }

    public Phrase retrieveRandomPhrase() {

        Phrase createdPhrase;

        do {
            createdPhrase = getPhraseByIndex(random.nextInt(maxPossibleAppearingIndex));
        } while (doesStackContainPhrase(createdPhrase));

        pushToLastSevenPhrasesStack(createdPhrase);
        createdPhrase.resetPreviousValues();
        return createdPhrase;
    }

    public static Timestamp toTimestamp(ZonedDateTime dateTime) {
        if(dateTime == null){
            return null;
        }
        return new Timestamp(dateTime.toInstant().getEpochSecond() * 1000L);
    }

    public void insertPhrase(Phrase phrase) {
        System.out.println("CALL: insertPhrase(Phrase phrase) from DAO");
        String insertSql = "INSERT INTO " + "words" + " (for_word, nat_word, transcr, prob_factor, create_date," +
                " label, last_accs_date, exactmatch, rate, user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = mainDbConn.prepareStatement(insertSql)) {
            ps.setString(1, phrase.foreignWord);
            ps.setString(2, phrase.nativeWord);
            ps.setString(3, phrase.transcription);
            ps.setDouble(4, phrase.probabilityFactor.doubleValue());
            ps.setTimestamp(5, toTimestamp(phrase.collectionAddingDateTime));
            ps.setString(6, phrase.label);
            ps.setTimestamp(7, toTimestamp(phrase.lastAccessDateTime));
            ps.setBoolean(8, phrase.exactMatch);
            ps.setDouble(9, phrase.multiplier);
            ps.setString(10, loginBean.getUser());
            ps.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION inside: in insertPhrase(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        activePhrases.add(phrase);
//        reloadPhrasesCollection();

    }

    public void deletePhrase(Phrase phr) {
        System.out.println("CALL: deletePhrase(int id) from DAO");
        String deleteSql = "DELETE FROM " + loginBean.getUser() + " WHERE ID=" + phr.id;
        try (Statement st = mainDbConn.createStatement()) {
            st.execute(deleteSql);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in deletePhrase(int id) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        activePhrases.remove(phr);

    }

    public void updatePhrase(Phrase givenPhrase) {
        System.out.println("CALL: updatePhraseInDb(Phrase givenPhrase) from DAO with id=" + givenPhrase.id);
        String dateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String updateSql = "UPDATE " + "words" + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                "exactmatch=?, label=?, prob_factor=?, rate=?  WHERE id =" + givenPhrase.id;
        Phrase phraseInTheCollection = null;

        try {
            phraseInTheCollection = getPhraseById(givenPhrase.id);
        } catch (PhraseNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        phraseInTheCollection.foreignWord = givenPhrase.foreignWord;
        phraseInTheCollection.nativeWord = givenPhrase.nativeWord;
        phraseInTheCollection.transcription = givenPhrase.transcription;
        phraseInTheCollection.lastAccessDateTime = givenPhrase.lastAccessDateTime;
        phraseInTheCollection.exactMatch = givenPhrase.exactMatch;
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

            if (givenPhrase.label == null || givenPhrase.label.equalsIgnoreCase(""))
                mainDbPrepStat.setString(6, null);
            else
                mainDbPrepStat.setString(6, givenPhrase.label);

            mainDbPrepStat.setString(4, dateTime);
            mainDbPrepStat.setBoolean(5, givenPhrase.exactMatch);
            mainDbPrepStat.setDouble(7, givenPhrase.probabilityFactor.doubleValue());
            mainDbPrepStat.setDouble(8, givenPhrase.multiplier);
            mainDbPrepStat.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase givenPhrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

//        reloadPhrasesCollection();

    }

    public long[] updateProb(Phrase phrase) {
        System.out.println("CALL: updateProb(Phrase phrase) with id=" + phrase.id + " from DAO");
        String dateTime = ZonedDateTime.now(ZoneId.of(TIMEZONE)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        try {
            getPhraseById(phrase.id).probabilityFactor = phrase.probabilityFactor;
            getPhraseById(phrase.id).multiplier = phrase.multiplier;
        } catch (PhraseNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        try (Statement st = mainDbConn.createStatement()) {
            st.execute("UPDATE " + "words" + " SET prob_factor=" + phrase.probabilityFactor + ", last_accs_date='" + dateTime + "', rate=" + phrase.multiplier +
                    " WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }


        return reloadIndices(phrase.id);
    }

    public ArrayList<Phrase> getActivePhrases() {

        return new ArrayList<>(activePhrases);

    }

    private String getSql_CreateMainTable() {
        return "CREATE TABLE " + "words" + "\n" +
                "    (id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "    for_word VARCHAR(250) NOT NULL,\n" +
                "    nat_word VARCHAR(250) NOT NULL,\n" +
                "    transcr VARCHAR(100),\n" +
                "    prob_factor DOUBLE NOT NULL,\n" +
                "    label VARCHAR(50),\n" +
                "    create_date DATETIME,\n" +
                "    last_accs_date DATETIME,\n" +
                "    exactmatch BOOLEAN DEFAULT FALSE  NOT NULL,\n" +
                "    index_start DOUBLE,\n" +
                "    index_end DOUBLE,\n" +
                "    rate DOUBLE DEFAULT 1 NOT NULL" +
                "    user VARCHAR(50) NOT NULL,\n" +
                ")";
    }

    private String getSql_CreateStatTable() {

        return "CREATE TABLE " + "words" + "_stat (date DATETIME NOT NULL, ms INT NOT NULL, event VARCHAR(30) NOT NULL," +
                " id INT NOT NULL, learnt INT)";

    }

    private void initThisDayStatistics(){
        try(
                ResultSet rs1 = mainDbConn.createStatement().executeQuery
                        ("SELECT COUNT(*) FROM " + "statistics" + " WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)");
                ResultSet rs2 = mainDbConn.createStatement().executeQuery
                        ("SELECT TIMESTAMPDIFF(HOUR, (SELECT MIN(date) FROM " + "statistics" + " WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)), " +
                                "DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR))")
        ){
            rs1.next();
            answUntil6amAmount = rs1.getInt(1);
            rs2.next();
            totalHoursUntil6am = rs2.getInt(1);

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void createMainDb(){
        try (Statement statement = mainDbConn.createStatement()) {
            statement.execute(getSql_CreateMainTable());
            statement.execute("INSERT INTO " + "words" + " (for_word, nat_word, prob_factor) VALUES ('The', " +
                    "'The collection is empty, push Show All an add phrases', 30)");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in checkDbForExistingAllTables() from DAO during create main table in main DB");
        }
    }

    private void createStatisticsDb(){
        try (Statement statement = mainDbConn.createStatement()) {
            System.out.println("Execute " + getSql_CreateStatTable());
            statement.execute(getSql_CreateStatTable());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in checkDbForExistingAllTables() from DAO during create stat table in main DB");
        }
    }

    private void checkDbForExistingAllTables() {

        /*boolean mainDbExists = false;
        boolean statDbExists = false;
        try (ResultSet rs_tables = mainDbConn.createStatement().executeQuery
                ("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='guessword'")) {
            while (rs_tables.next()) {
                //Check for exisiting the Main DB
                if (rs_tables.getString("TABLE_NAME").equals(loginBean.getUser()))
                    mainDbExists = true;
                //Check for exisiting the Stat DB
                if (rs_tables.getString("TABLE_NAME").equals(loginBean.getUser() + "_stat"))
                    statDbExists = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in checkDbForExistingAllTables() from DAO");
        }

        if (!mainDbExists) {
            createMainDb();
        }

        if (!statDbExists) {
            createStatisticsDb();
        }*/
    }

    private Phrase getPhraseById(int id) throws PhraseNotFoundException{
        //Возвращает фразу из коллекции по данному id

        for (Phrase phrase : allPhrases) {
            if (phrase.id == id)
                return phrase;
        }

        throw new PhraseNotFoundException();
    }

    private Phrase getPhraseByIndex(long index) {
        // @param index индекс, как правило, рандомный по которому искать фразу в коллекции
        // @return Возвращает фразу из коллекции
        long startTime = System.nanoTime();
        for (Phrase phrase : activePhrases) {
            if (index >= phrase.indexStart && index <= phrase.indexEnd) {
                //Записываем время доступа в объект фразы
                phrase.setTimeOfReturningFromList(System.nanoTime() - startTime);
                return phrase;
            }
        }
        throw new RuntimeException();
    }

    private void checkLastSevenPhraseSatck(){
        if (lastSevenPhrasesStack == null || lastSevenPhrasesStack.length > totalActiveWordsAmount) {
            if (totalActiveWordsAmount >= 7) {
                lastSevenPhrasesStack = new Phrase[7];
                lastSevenStackCurrentPosition = 0;
            }else {
                lastSevenPhrasesStack = new Phrase[0];
            }
        }
    }

    private int getCurrentLssPosition(){
        int position = lastSevenStackCurrentPosition > lastSevenPhrasesStack.length - 1 ? lastSevenStackCurrentPosition = 0 : lastSevenStackCurrentPosition;
        return position++;
    }

    private void pushToLastSevenPhrasesStack(Phrase addedPhrase) {

        checkLastSevenPhraseSatck();

        if (!doesStackContainPhrase(addedPhrase) && lastSevenPhrasesStack != null) {
            lastSevenPhrasesStack[getCurrentLssPosition()] = addedPhrase;
        }
    }

    private boolean doesStackContainPhrase(Phrase givenPhrase){
        if(givenPhrase == null){
            throw new IllegalArgumentException("Given phrase was null");
        }
        checkLastSevenPhraseSatck();
        for (Phrase currentPhraseFromStack : lastSevenPhrasesStack) {
            if (currentPhraseFromStack != null && currentPhraseFromStack.id == givenPhrase.id) {
                return true;
            }
        }
        return false;
    }




}

