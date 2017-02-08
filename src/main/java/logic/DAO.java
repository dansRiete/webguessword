package logic;

import Exceptions.*;
import beans.*;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Aleks on 11.05.2016.
 */
@SuppressWarnings("SqlResolve")
public class DAO {
    private Random random = new Random();
    private final String timezone = "Europe/Kiev";
//    public static final String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
//    public static final String localHost3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
//    public static final String localHost3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public HashSet<String> activeChosedLabels;
    public ArrayList<String> possibleLabels = new ArrayList<>();
    public double learnedWords;
    public double nonLearnedWords;
    public double totalActiveWordsAmount;
    public double totalPossibleWordsAmount;
    public int nonLearnedWordsProbSumm;
    public int learnedWordsProbSumm;
    private int maxPossibleAppearingIndex;
    public static final double CHANCE_OF_APPEARING_LEARNED_WORDS = 1d / 15d;
    private Phrase[] lastSevenPhrasesStack;
    private int lastSevenStackCurrentPosition;
    public Connection mainDbConn;
    private LoginBean loginBean;
    private ArrayList<Phrase> activePhrases = new ArrayList<>();
    private ArrayList<Phrase> listOfAllPhrases = new ArrayList<>();
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
        int mlseconds = Integer.parseInt(ZonedDateTime.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("SSS")));

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
            String sql = "INSERT INTO " + loginBean.getUser() + "_stat" + " VALUES ('" + dateTime + "', " + mlseconds +
                    ", '" + mode + "', " + givenPhrase.id + (learnt!=0?(", " + learnt):", NULL") + ")";

            System.out.println(sql);
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
            String sql = "UPDATE " + loginBean.getUser() + "_stat" + " SET event='" + mode + "' WHERE date='" + dateTime +"' AND id=" + phr.id
                    + (learnt!=0?(", " + learnt):", NULL");
            System.out.println("SQL from updateStatistics() is " + sql);
            System.out.println(sql);
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in updateStatistics from DAO");
        }
    }

    public ArrayList<Phrase> getTodaysPhrasesCollection(){
        ArrayList<Phrase> list = new ArrayList<>();

        try (Statement statement = mainDbConn.createStatement();
             ResultSet rs = statement.executeQuery
                     ("SELECT * FROM " + loginBean.getUser() + "_stat" + " WHERE " +
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
        //Возвращает список возможных меток для фраз + "All"
        System.out.println("CALL: retievePossibleLabels() from DAO");
        possibleLabels.clear();
        possibleLabels.add("All");
        String temp;

        try (Statement st = mainDbConn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + loginBean.getUser() + " ORDER BY LABEL")) {

            while (rs.next()) {
                temp = rs.getString("LABEL");
                possibleLabels.add(temp == null ? "null" : temp);
            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION: in retievePossibleLabels() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        return possibleLabels;

    }

    public void reloadPhrasesCollection() {
        String sql = "SELECT * FROM " + loginBean.getUser() + " ORDER BY create_date DESC, id DESC";

        try (Statement mainSt = mainDbConn.createStatement(); ResultSet rs = mainSt.executeQuery(sql)) {

            System.out.println("CALL: reloadPhrasesCollection() from DAO");

            activePhrases.clear();
            totalPossibleWordsAmount = 0;

            while (rs.next()) {

                int id = rs.getInt("id");
                String for_word = rs.getString("for_word");
                String nat_word = rs.getString("nat_word");
                String transcr = rs.getString("transcr");
                BigDecimal prob = new BigDecimal(rs.getDouble("prob_factor"));
                Timestamp create_date = rs.getTimestamp("create_date");
                String label = rs.getString("label");
                Timestamp last_accs_date = rs.getTimestamp("last_accs_date");
                double index_start = rs.getDouble("index_start");
                double index_end = rs.getDouble("index_end");
                double rate = rs.getDouble("rate");
                boolean exactmatch = rs.getBoolean("exactmatch");

                Phrase addedToCollectionPhrase = new Phrase(id, for_word, nat_word, transcr, prob, create_date, label,
                        last_accs_date, index_start, index_end, exactmatch, rate, this);

                totalPossibleWordsAmount++;
                listOfAllPhrases.add(addedToCollectionPhrase);

                if (addedToCollectionPhrase.isThisPhraseInList(activeChosedLabels)) {
                    activePhrases.add(addedToCollectionPhrase);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("SQL Exception in reloadPhrasesCollection() from DAO, sql was \"" + sql + "\"");
        }

        reloadIndices(1);
//        System.out.println("SAME PHRASES = " + sortCollectionByMatches(listOfAllPhrases).size());


    }

    private long[] reloadIndices(int id) {

        long start = System.currentTimeMillis();
        double temp = 0;
        double indOfLW;     //Индекс выпадения изученных
        double rangeOfNLW;  //Диапазон индексов неизученных слов
        double scaleOf1prob;    //rangeOfNLW/nonLearnedWordsProbSumm  цена одного probabilityFactor

        int countOfModIndices = 0;
        long[] indexes = new long[2];
        totalActiveWordsAmount = activePhrases.size(); //Считаем общее количество фраз

        //Считаем неизученные слова, nonLearnedWordsProbSumm и очищаем индексы
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

        //Считаем изученные (learnedWords)
        learnedWords = totalActiveWordsAmount - nonLearnedWords;
        indOfLW = CHANCE_OF_APPEARING_LEARNED_WORDS / learnedWords;
        rangeOfNLW = learnedWords > 0 ? 1 - CHANCE_OF_APPEARING_LEARNED_WORDS : 1;
        scaleOf1prob = rangeOfNLW / nonLearnedWordsProbSumm;

        if (nonLearnedWords == 0) {
            System.out.println("Все слова выучены!");
        }

        for (Phrase phrase : activePhrases) { //Устанавилвает индексы для неизученных слов
            long indexStart;
            long indexEnd;
            //Переменной probabilityFactor присваивается probabilityFactor фразы с currentPhraseId = i;
            double prob;
            prob = phrase.probabilityFactor.doubleValue();

            //Если nonLearnedWords == 0, то есть, все слова выучены устанавливаются равные для всех индексы
            if (nonLearnedWords == 0) {

                indexStart = Math.round(temp * 1000000000);
                phrase.indexStart = indexStart;
                temp += CHANCE_OF_APPEARING_LEARNED_WORDS / learnedWords;
                indexEnd = Math.round((temp * 1000000000) - 1);
                phrase.indexEnd = indexEnd;

            } else { //Если нет, то индексы ставяться по алгоритму

                if (prob > 3) {

                    indexStart = Math.round(temp * 1000000000);
                    phrase.indexStart = indexStart;
                    temp += scaleOf1prob * prob;
                    indexEnd = Math.round((temp * 1000000000) - 1);
                    phrase.indexEnd = indexEnd;

                } else {

                    indexStart = Math.round(temp * 1000000000);
                    phrase.indexStart = indexStart;
                    temp += indOfLW;
                    indexEnd = Math.round((temp * 1000000000) - 1);
                    phrase.indexEnd = indexEnd;

                }
            }

            countOfModIndices++;
            if(countOfModIndices== activePhrases.size()){
                maxPossibleAppearingIndex = (int) phrase.indexEnd;
            }
            if (phrase.id == id) {
                indexes[0] = indexStart;
                indexes[1] = indexEnd;
            }
        }

        System.out.println("CALL: reloadIndices() from DAO" + "Indexes changed=" + countOfModIndices + " Time taken " + (System.currentTimeMillis() - start) + "ms");
        return indexes;
    }

    public Phrase obtainRandomPhrase() {

        Phrase createdPhrase;

        do {
            createdPhrase = getPhraseByIndex(random.nextInt(maxPossibleAppearingIndex));
//            createdPhrase.timeOfReturningFromList = "";

        } while (doesStackContainPhrase(createdPhrase));

        pushToLastSevenPhrasesStack(createdPhrase);
        createdPhrase.resetPreviousValues();
        return createdPhrase;
    }

    public void insertPhrase(Phrase phrase) {
        System.out.println("CALL: insertPhrase(Phrase phrase) from DAO");
        String insertSql = "INSERT INTO " + loginBean.getUser() + " (for_word, nat_word, transcr, prob_factor, create_date," +
                " label, last_accs_date, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = mainDbConn.prepareStatement(insertSql)) {
            ps.setString(1, phrase.foreignWord);
            ps.setString(2, phrase.nativeWord);
            ps.setString(3, phrase.transcription);
            ps.setDouble(4, phrase.probabilityFactor.doubleValue());
            ps.setTimestamp(5, phrase.collectionAddingDateTime);
            ps.setString(6, phrase.label);
            ps.setTimestamp(7, phrase.lastAccessDateTime);
            ps.setBoolean(8, phrase.exactMatch);
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
        String dateTime = ZonedDateTime.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String updateSql = "UPDATE " + loginBean.getUser() + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
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
        String dateTime = ZonedDateTime.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        try {
            getPhraseById(phrase.id).probabilityFactor = phrase.probabilityFactor;
            getPhraseById(phrase.id).multiplier = phrase.multiplier;
        } catch (PhraseNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        try (Statement st = mainDbConn.createStatement()) {
            st.execute("UPDATE " + loginBean.getUser() + " SET prob_factor=" + phrase.probabilityFactor + ", last_accs_date='" + dateTime + "', rate=" + phrase.multiplier +
                    " WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }


        return reloadIndices(phrase.id);
    }

    public ArrayList<Phrase> getActivePhrases() {

        return activePhrases;

    }

    private String getSql_CreateMainTable() {
        return "CREATE TABLE " + loginBean.getUser() + "\n" +
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
                "    rate DOUBLE" +
                ")";
    }

    private String getSql_CreateStatTable() {

        return "CREATE TABLE " + loginBean.getUser() + "_stat (date DATETIME NOT NULL, ms INT NOT NULL, event VARCHAR(30) NOT NULL," +
                " id INT NOT NULL, learnt INT)";

    }

    private void initThisDayStatistics(){
        try(
                ResultSet rs1 = mainDbConn.createStatement().executeQuery
                        ("SELECT COUNT(*) FROM " + loginBean.getUser() + "_stat WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)");
                ResultSet rs2 = mainDbConn.createStatement().executeQuery
                        ("SELECT TIMESTAMPDIFF(HOUR, (SELECT MIN(date) FROM " + loginBean.getUser() + "_stat WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)), " +
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
            statement.execute("INSERT INTO " + loginBean.getUser() + " (for_word, nat_word, prob_factor) VALUES ('The', " +
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

        boolean mainDbExists = false;
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
        }
    }

    private Phrase getPhraseById(int id) throws PhraseNotFoundException{
        //Возвращает фразу из коллекции по данному id

        for (Phrase phrase : listOfAllPhrases) {
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

