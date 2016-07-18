package logic;

import Exceptions.DataBaseConnectionException;
import beans.LoginBean;

import java.math.BigDecimal;
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
public class DAO {
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";//
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
    private Random random = new Random();
    private String timezone = "Europe/Kiev";
    public static final String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public static final String localHost3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public static final String localHost3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public HashSet<String> chosedLabels;
    public ArrayList<String> possibleLabels = new ArrayList<>();
    public double learnedWords;
    public double nonLearnedWords;
    public double totalActiveWords;
    public double totalPossibleWords;
    private double maxIndex;
    final double chanceOfLearnedWords = 1d / 15d;
    private Phrase[] lastPhrasesStack;
    private int stackNum;
    public Connection mainDbConn;
    private LoginBean loginBean;
    private ArrayList<Phrase> listOfActivePhrases = new ArrayList<>();
    private ArrayList<Phrase> listOfAllPhrases = new ArrayList<>();

    private String getCreateInmemDb_MainTable_SqlString() {

        return "CREATE TABLE " + loginBean.getUser() + "\n" +
                "(\n" +
                "    id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,\n" +
                "    for_word VARCHAR(250) NOT NULL,\n" +
                "    nat_word VARCHAR(250) NOT NULL,\n" +
                "    transcr VARCHAR(100),\n" +
                "    prob_factor DOUBLE,\n" +
                "    create_date DATETIME,\n" +
                "    label VARCHAR(50),\n" +
                "    last_accs_date DATETIME,\n" +
                "    exactmatch BOOLEAN,\n" +
                "    index_start DOUBLE,\n" +
                "    index_end DOUBLE\n" +
                ")";
        //"ALTER TABLE " + user + " ADD CONSTRAINT unique_id UNIQUE (id);";

    }

    private String getCreateMainDb_MainTable_SqlString() {
        String str = "CREATE TABLE " + loginBean.getUser() + "\n" +
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
                "    index_end DOUBLE)";
        System.out.println(str);
        return str;

    }

    private String getCreateMainDb_StatTable_SqlString() {

        return "CREATE TABLE " + loginBean.getUser() + "\n" +
                "(\n" +
                "    id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,\n" +
                "    for_word VARCHAR(250) NOT NULL,\n" +
                "    nat_word VARCHAR(250) NOT NULL,\n" +
                "    transcr VARCHAR(100),\n" +
                "    prob_factor DOUBLE,\n" +
                "    create_date DATETIME,\n" +
                "    label VARCHAR(50),\n" +
                "    last_accs_date DATETIME,\n" +
                "    exactmatch BOOLEAN,\n" +
                "    index_start DOUBLE,\n" +
                "    index_end DOUBLE\n" +
                ")";
        //"ALTER TABLE " + user + " ADD CONSTRAINT unique_id UNIQUE (id);";

    }



    public DAO(LoginBean loginBean) {

        this.loginBean = loginBean;
//        table = loginBean.getUser();

        //>>>Получаем подключение к основной БД, в случае ошибки пробуем подключиться через локальнй хост (только для режима тестирования)
        /*try {
            mainDbConn = DriverManager.getConnection(remoteHost, "adminLtuHq9R", "d-AUIKakd1Br");
            dbConnected = "- Remote DB was connected";
        } catch (SQLException e) {
            try {
                mainDbConn = DriverManager.getConnection(localHost, "adminLtuHq9R", "d-AUIKakd1Br");
                dbConnected = "- Local DB was connected";
            } catch (SQLException e1) {
                e1.printStackTrace();
                System.out.println("EXCEPTION: in DAO constructor");
                throw new DataBaseConnectionException();
            }
        }*/
        mainDbConn = loginBean.returnConnection();
        //<<<

        checkTables();
        reloadCollectionOfPhrases();
        reloadLabelsList();


    }

    /**
     * Проверяет наличие соответствующих таблиц БД необходимых для работы приложения
     */
    private void checkTables() {
        boolean mainDbExists = false;
        boolean statDbExists = false;
        try (ResultSet rs_tables = mainDbConn.createStatement().executeQuery
                ("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='guessword'")) {
            while (rs_tables.next()) {
                //Проверяем или в БД существует основная таблица фраз
                if (rs_tables.getString("TABLE_NAME").equals(loginBean.getUser()))
                    mainDbExists = true;
                //Проверяем или в БД существует статистическая таблица
                if (rs_tables.getString("TABLE_NAME").equals(loginBean.getUser() + "_stat"))
                    statDbExists = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in checkTables() from DAO");
        }

        if (!mainDbExists) {
            try (Statement statement = mainDbConn.createStatement()) {
                statement.execute(getCreateMainDb_MainTable_SqlString());
                statement.execute("INSERT INTO " + loginBean.getUser() + " (for_word, nat_word, prob_factor) VALUES ('The', " +
                        "'The collection is empty, push Show All an add phrases', 30)");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("EXCEPTION: SQLException in checkTables() from DAO during create main table in main DB");
            }

        }
    }

    public ArrayList<Phrase> returnPhrasesList() {
//        System.out.println("CALL: returnPhrasesList() from DAO");
//        ArrayList<Phrase> list = new ArrayList<>();

        return listOfActivePhrases;
    }

    /**
     * @return Возвращает список возможных меток для фраз + "All"
     */
    public List<String> reloadLabelsList() {
        System.out.println("CALL: reloadLabelsList() from DAO");
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
            System.out.println("EXCEPTION: in reloadLabelsList() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        return possibleLabels;

    }

    public void reloadCollectionOfPhrases() {


        String insertSql = "INSERT INTO " + loginBean.getUser() +
                "(id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
                "index_start, index_end, exactmatch) VALUES (?,?,?,?,?,?,?,?,?,?,?)";


        try (Statement mainSt = mainDbConn.createStatement();
             ResultSet rs = mainSt.executeQuery("SELECT * FROM " + loginBean.getUser() + " ORDER BY create_date DESC, id DESC")) {

            System.out.println("CALL: reloadCollectionOfPhrases() from DAO");

            listOfActivePhrases.clear();
            totalPossibleWords = 0;
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
                boolean exactmatch = rs.getBoolean("exactmatch");

                Phrase phrase = new Phrase(id, for_word, nat_word, transcr, prob, create_date, label,
                        last_accs_date, index_start, index_end, exactmatch, this);

                //Добавляем в активную коллекцию если метка фразы совпадает с выбранными - "chosedLabels" и считаем totalPossibleWords
                if (phrase.inLabels(chosedLabels)) {
                    listOfActivePhrases.add(phrase);
                    listOfAllPhrases.add(phrase);
                    totalPossibleWords++;
                } else {
                    listOfAllPhrases.add(phrase);
                    totalPossibleWords++;
                }
                /*listOfActivePhrases.add(phrase);
                listOfAllPhrases.add(phrase);
                totalPossibleWords++;*/

            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in reloadCollectionOfPhrases() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        reloadIndices(1);

    }

    /**
     * @param id по данному id
     * @return Возвращает фразу из коллекции
     */
    private Phrase getPhraseById(int id) {

        for (Phrase phrase : listOfAllPhrases) {
            if (phrase.id == id)
                return phrase;
        }

        throw new RuntimeException();
    }

    /**
     * @param index индекс, как правило, рандомный по которому искать фразу в коллекции
     * @return Возвращает фразу из коллекции
     */
    private Phrase getPhraseByIndex(long index) {
        long startTime = System.nanoTime();
        for (Phrase phrase : listOfActivePhrases) {
            if (index >= phrase.indexStart && index <= phrase.indexEnd) {
                //Записываем время доступа в объект фразы
                phrase.setTimeOfReturningFromList(System.nanoTime() - startTime);
                return phrase;
            }
        }
        throw new RuntimeException();
    }

    public long[] updateProb(Phrase phrase) {
        System.out.println("CALL: updateProb(Phrase phrase) with id=" + phrase.id + " from DAO");
        String dateTime = ZonedDateTime.now(ZoneId.of("Europe/Kiev")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        getPhraseById(phrase.id).prob = phrase.prob;

        try (Statement st = mainDbConn.createStatement()) {
            st.execute("UPDATE " + loginBean.getUser() + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime +
                    "' WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }


        return reloadIndices(phrase.id);
    }

    public void updatePhrase(Phrase phrase) {
        System.out.println("CALL: updatePhrase(Phrase phrase) from DAO with id=" + phrase.id);
        String dateTime = ZonedDateTime.now(ZoneId.of("Europe/Kiev")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String updateSql = "UPDATE " + loginBean.getUser() + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                "exactmatch=?, label=?, prob_factor=?  WHERE id =" + phrase.id;
        Phrase phr = getPhraseById(phrase.id);
        phr.forWord = phrase.forWord;
        phr.natWord = phrase.natWord;
        phr.transcr = phrase.transcr;
        phr.lastAccs = phrase.lastAccs;
        phr.exactMatch = phrase.exactMatch;
        phr.label = phrase.label;
        phr.prob = phrase.prob;

        try (PreparedStatement mainDbPrepStat = mainDbConn.prepareStatement(updateSql)) {

            mainDbPrepStat.setString(1, phrase.forWord);
            mainDbPrepStat.setString(2, phrase.natWord);

            if (phrase.transcr == null || phrase.transcr.equalsIgnoreCase(""))
                mainDbPrepStat.setString(3, null);
            else
                mainDbPrepStat.setString(3, phrase.transcr);

            if (phrase.label == null || phrase.label.equalsIgnoreCase(""))
                mainDbPrepStat.setString(6, null);
            else
                mainDbPrepStat.setString(6, phrase.label);

            mainDbPrepStat.setString(4, dateTime);
            mainDbPrepStat.setBoolean(5, phrase.exactMatch);
            mainDbPrepStat.setDouble(7, phrase.prob.doubleValue());
            mainDbPrepStat.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        reloadCollectionOfPhrases();

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
        reloadCollectionOfPhrases();

    }

    public long[] reloadIndices(int id) {

        long start = System.currentTimeMillis();
        double temp = 0;
        double indOfLW;     //Индекс выпадения изученных
        double rangeOfNLW;  //Диапазон индексов неизученных слов
        double scaleOf1prob;    //rangeOfNLW/summProbOfNLW  цена одного prob
        int summProbOfNLW = 0;
        int countOfModIndices = 0;
        long[] indexes = new long[2];
        totalActiveWords = listOfActivePhrases.size(); //Считаем общее количество фраз

        //Считаем неизученные слова, summProbOfNLW и очищаем индексы
        nonLearnedWords = 0;
        summProbOfNLW = 0;
        for (Phrase phr : listOfActivePhrases) {
            phr.indexStart = phr.indexEnd = 0;
            if (phr.prob.doubleValue() > 3) {
                nonLearnedWords++;
                summProbOfNLW += phr.prob.doubleValue();
            }
        }

        //Считаем изученные (learnedWords)
        learnedWords = totalActiveWords - nonLearnedWords;
        indOfLW = chanceOfLearnedWords / learnedWords;
        rangeOfNLW = learnedWords > 0 ? 1 - chanceOfLearnedWords : 1;
        scaleOf1prob = rangeOfNLW / summProbOfNLW;
        if (nonLearnedWords == 0) {
            System.out.println("Все слова выучены!");
        }

        for (Phrase phrase : listOfActivePhrases) { //Устанавилвает индексы для неизученных слов
            long indexStart;
            long indexEnd;
            //Переменной prob присваивается prob фразы с currentPhraseId = i;
            double prob;
            prob = phrase.prob.doubleValue();

            //Если nonLearnedWords == 0, то есть, все слова выучены устанавливаются равные для всех индексы
            if (nonLearnedWords == 0) {

                indexStart = Math.round(temp * 1000000000);
                phrase.indexStart = indexStart;
                temp += chanceOfLearnedWords / learnedWords;
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
            if(countOfModIndices==listOfActivePhrases.size()){
                maxIndex = phrase.indexEnd;
//                System.out.println("maxIndex is " + maxIndex);
            }
            if (phrase.id == id) {
                indexes[0] = indexStart;
                indexes[1] = indexEnd;
            }
        }

        System.out.println("CALL: reloadIndices() from DAO" + "Indexes changed=" + countOfModIndices + " Time taken " + (System.currentTimeMillis() - start) + "ms");
        return indexes;
    }

    /*public long[] reloadIndices(int id){

        long start = System.currentTimeMillis();
        BigDecimal temp = new BigDecimal(0);
        BigDecimal indOfLW = new BigDecimal(0);     //Индекс выпадения изученных
        BigDecimal rangeOfNLW = new BigDecimal(0);  //Диапазон индексов неизученных слов
        BigDecimal scaleOf1prob = new BigDecimal(0);    //rangeOfNLW/summProbOfNLW  цена одного prob
        BigDecimal summProbOfNLW = new BigDecimal(0);
        ArrayList<Integer> idArr = new ArrayList<>();
        long[] indexes = new long[2];


        for(Phrase phr : activeListOfPhrases){
            idArr.add(phr.id);
        }

        //Считаем неизученные слова
        nonLearnedWords = 0;
        summProbOfNLW = new BigDecimal(0);
        for(Phrase phr : activeListOfPhrases){
            if(phr.prob.doubleValue() > 3){
                nonLearnedWords++;
                summProbOfNLW = summProbOfNLW.add(new BigDecimal(phr.prob.doubleValue()));
            }
        }

        //Считаем общее количество фраз

        totalActiveWords = activeListOfPhrases.size();
        learnedWords = totalActiveWords - nonLearnedWords;
        indOfLW = new BigDecimal(chanceOfLearnedWords.divide(new BigDecimal(learnedWords), 15, BigDecimal.ROUND_HALF_UP).doubleValue());
        rangeOfNLW = new BigDecimal(learnedWords>0?1-chanceOfLearnedWords.doubleValue():1);
        scaleOf1prob = new BigDecimal(rangeOfNLW.divide(summProbOfNLW, 15, BigDecimal.ROUND_HALF_UP).doubleValue());
        if(nonLearnedWords==0){
            System.out.println("Все слова выучены!");
        }
        int countOfModIndices = 0;

        //Clears indexes before reloading
        for(Phrase phr : activeListOfPhrases){
            phr.indexStart = phr.indexEnd = 0;
        }


            for (int i : idArr) { //Устанавилвает индексы для неизученных слов

                long indexStart;
                long indexEnd;

                //Переменной prob присваивается prob фразы с currentPhraseId = i;
                double prob;
                Phrase phrase = getPhraseById(i);
                prob = phrase.prob.doubleValue();

                //Если nonLearnedWords == 0, то есть, все слова выучены устанавливаются равные для всех индексы
                if (nonLearnedWords == 0) {

                    indexStart = temp.multiply(new BigDecimal(1000000000)).longValue();
                    phrase.indexStart = indexStart;
                    temp = temp.add(chanceOfLearnedWords.divide(new BigDecimal(learnedWords), 15, BigDecimal.ROUND_HALF_UP));
                    indexEnd = (temp.multiply(new BigDecimal(1000000000))).subtract(new BigDecimal(1)).longValue();
                    phrase.indexEnd = indexEnd;

                } else { //Если нет, то индексы ставяться по алгоритму

                    if (prob > 3) {

                        indexStart = temp.multiply(new BigDecimal(1000000000)).longValue();
                        phrase.indexStart = indexStart;
                        temp = temp.add(scaleOf1prob.multiply(new BigDecimal(prob)));
                        indexEnd = (temp.multiply(new BigDecimal(1000000000))).subtract(new BigDecimal(1)).longValue();
                        phrase.indexEnd = indexEnd;

                    } else {

                        indexStart = temp.multiply(new BigDecimal(1000000000)).longValue();
                        phrase.indexStart = indexStart;
                        temp = temp.add(indOfLW);
                        indexEnd = (temp.multiply(new BigDecimal(1000000000))).subtract(new BigDecimal(1)).longValue();
                        phrase.indexEnd = indexEnd;
                    }
                }

                countOfModIndices++;

                if(i==id){
                    indexes[0]=indexStart;
                    indexes[1]=indexEnd;
                }

//                System.out.println("Indexes are " + indexes[0] + " - " + indexes[1]);
            }

        System.out.println("CALL: reloadIndices() from DAO" + "Indexes changed="+countOfModIndices +
                " Time taken " + (System.currentTimeMillis()-start) + "ms");

        return indexes;

    }*/

    /**
     * Помещает данный в качестве параметра Id фразы в стек, стек используется для предотвращения повторения фраз
     *
     * @param id добавляемая фраза
     * @return true если фраза присутствует в стеке, false если отсутствует
     */
    private boolean pushIntoStack(Phrase id) {
        StringBuilder msg = new StringBuilder("CALL: pushIntoStack(Phrase id) from DAO;");
        StringBuilder stackContent = new StringBuilder("Содержимое стека [");
        //Если массив не инстантиирован или количество фраз стало меньше чем размер массива-стека заново создаём его с
        // нужным размером и обнуляем положение стека
        boolean result = false;
        if (lastPhrasesStack == null || lastPhrasesStack.length > totalActiveWords) {
            //Если количество фраз больше или равно 7 создаётся стандартный стек на 7 элементов.
            if (totalActiveWords >= 7) {
                lastPhrasesStack = new Phrase[7];
                stackNum = 0;
                msg.append(" создан стек на 7 элементов;");
            }
            //Если количество фраз меньше 3 в стеке нет надобности, метод возвращает false
            else if (totalActiveWords < 3) {
                msg.append(" в стеке нет необходимости - выход из метода (возврат false);");
                System.out.println(msg);
                return false;
            }
            //Если кол-во слов меньше 7 но больше трёх создаётся стек на (кол-во слов минус 1) элементов
            else {
                msg.append(" создан стек на " + ((int) totalActiveWords - 1) + " элементов;");
                lastPhrasesStack = new Phrase[(int) totalActiveWords - 1];
                stackNum = 0;
            }
        }
        //Проверяем или стек не содержит айдишник данной в качестве параметра фразы
        for (Phrase phrase : lastPhrasesStack) {
            if (phrase != null && phrase.id == id.id) {
                msg.append(" в стеке уже есть фраза " + id.forWord + " метод возвращает true;");
                result = true;
                break;
            }
        }
        if (!result) {
            msg.append(" в стек помещается фраза \"" + id.natWord + "\";");
            lastPhrasesStack[stackNum > lastPhrasesStack.length - 1 ? stackNum = 0 : stackNum] = id;
            stackNum++;
        }

        for (Phrase phrase : lastPhrasesStack) {
            if (phrase != null) {
                stackContent.append(phrase.id + ", ");
            } else {
                stackContent.append("null, ");
            }
        }
        stackContent.append("]");
        System.out.println(msg + " " + stackContent);
        return result;
    }

    public Phrase createRandPhrase() {
        System.out.println("CALL: createRandPhrase()");
        Phrase phrase = null;


        //Новая фраза создаётся пока не подтвердится, что она отсутствует в стеке(последние 7 фраз)
        do {
            int index = random.nextInt( (int) maxIndex);
            Phrase tempPhrase = getPhraseByIndex(index);
            phrase = new Phrase(tempPhrase.id, tempPhrase.forWord, tempPhrase.natWord, tempPhrase.transcr, tempPhrase.prob, tempPhrase.createDate,
                    tempPhrase.label, tempPhrase.lastAccs, tempPhrase.indexStart, tempPhrase.indexEnd, tempPhrase.exactMatch, this);
            phrase.timeOfReturningFromList = tempPhrase.timeOfReturningFromList;
        } while (pushIntoStack(phrase));


        System.out.println("Phrase is " + phrase.natWord + " " +
                "indexes are: (" + phrase.indexStart + " - " + phrase.indexEnd + ")");
        return phrase;
    }

    public void insertPhrase(Phrase phrase) {
        System.out.println("CALL: insertPhrase(Phrase phrase) from DAO");
        String insertSql = "INSERT INTO " + loginBean.getUser() + " (for_word, nat_word, transcr, prob_factor, create_date," +
                " label, last_accs_date, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = mainDbConn.prepareStatement(insertSql)) {
            ps.setString(1, phrase.forWord);
            ps.setString(2, phrase.natWord);
            ps.setString(3, phrase.transcr);
            ps.setDouble(4, phrase.prob.doubleValue());
            ps.setTimestamp(5, phrase.createDate);
            ps.setString(6, phrase.label);
            ps.setTimestamp(7, phrase.lastAccs);
            ps.setBoolean(8, phrase.exactMatch);
            ps.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION inside: in insertPhrase(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
//        listOfActivePhrases.add(phrase);
        reloadCollectionOfPhrases();

    }

    /*public void backupDB(){
        System.out.println("CALL: backupDB() from DAO");
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        int count = 0;
        long start = System.currentTimeMillis();
        Statement st = null;
        try {
            st = inMemDbConn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + table);
            PreparedStatement ps = mainDbConn.prepareStatement("INSERT INTO res" + table +
                    " (date, id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
                    "index_start, index_end, exactmatch) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");

            while (rs.next()){
                ps.setTimestamp(1,ts);
                ps.setInt(2, rs.getInt("id"));
                ps.setString(3, rs.getString("for_word"));
                ps.setString(4, rs.getString("nat_word"));
                ps.setString(5, (rs.getString("transcr")==null?null:rs.getString("transcr")));
                ps.setDouble(6, rs.getDouble("prob_factor"));
                ps.setTimestamp(7, rs.getTimestamp("create_date"));
                ps.setString(8, (rs.getString("label")==null?null:rs.getString("label")));
                ps.setTimestamp(9, (rs.getTimestamp("last_accs_date") == null ? null : rs.getTimestamp("last_accs_date")));
                ps.setDouble(10, rs.getDouble("index_start"));
                ps.setDouble(11, rs.getDouble("index_end"));
                ps.setBoolean(12, rs.getBoolean("exactmatch"));
                ps.execute();
                count++;
            }
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in backupDB() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        long end = System.currentTimeMillis()-start;
        System.out.println("Copied " + count + " elements, total time=" + end + " ms");
    }*/


}

