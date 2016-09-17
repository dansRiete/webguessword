package logic;

import Exceptions.PhraseNotFoundException;
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
    private Random random = new Random();
    private String timezone = "Europe/Kiev";
//    public static final String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
//    public static final String localHost3306 = "jdbc:mysql://127.0.0.1:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
//    public static final String localHost3307 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public HashSet<String> chosedLabels;
    public ArrayList<String> possibleLabels = new ArrayList<>();
    public double learnedWords;
    public double nonLearnedWords;
    public double totalActiveWords;
    public double totalPossibleWords;
    public int summProbOfNLW;
    public int summProbOfLW;
    private double maxIndex;
    public static final double CHANCE_OF_APPEARING_LEARNED_WORDS = 1d / 15d;
    private Phrase[] lastPhrasesStack;
    private int stackNum;
    public Connection mainDbConn;
    private LoginBean loginBean;
    private ArrayList<Phrase> listOfActivePhrases = new ArrayList<>();
    private ArrayList<Phrase> listOfAllPhrases = new ArrayList<>();
    public int countAnswUntil6am; //Number of replies to 6 am of the current day
    public int totalHoursUntil6am; // Number of hours spent from the very begining till 6am of the current day

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

        checkForExistAllTablesInDB();
        reloadCollectionOfPhrases();
        reloadLabelsList();
        initialStatistics();


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
                "    rate DOUBLE,\n" +
                "    index_start DOUBLE,\n" +
                "    index_end DOUBLE)";
        System.out.println(str);
        return str;

    }

    private String getCreateMainDb_StatTable_SqlString() {

        return "CREATE TABLE " + loginBean.getUser() + "_stat (date DATETIME NOT NULL, ms INT NOT NULL, event VARCHAR(30) NOT NULL," +
                " id INT NOT NULL, learnt INT)";

    }

    private void initialStatistics(){
        try(
                ResultSet rs1 = mainDbConn.createStatement().executeQuery
                    ("SELECT COUNT(*) FROM " + loginBean.getUser() + "_stat WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)");
                ResultSet rs2 = mainDbConn.createStatement().executeQuery
                    ("SELECT TIMESTAMPDIFF(HOUR, (SELECT MIN(date) FROM " + loginBean.getUser() + "_stat WHERE date < DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)), " +
                            "DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR))")
        ){
            rs1.next();
            countAnswUntil6am = rs1.getInt(1);
            rs2.next();
            totalHoursUntil6am = rs2.getInt(1);

        }catch (SQLException e){
            e.printStackTrace();
        }
    }





    private void checkForExistAllTablesInDB() {
        //Проверяет наличие соответствующих таблиц БД необходимых для работы приложения
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
            System.out.println("EXCEPTION: SQLException in checkForExistAllTablesInDB() from DAO");
        }

        if (!mainDbExists) {
            try (Statement statement = mainDbConn.createStatement()) {
                statement.execute(getCreateMainDb_MainTable_SqlString());
                statement.execute("INSERT INTO " + loginBean.getUser() + " (for_word, nat_word, prob_factor) VALUES ('The', " +
                        "'The collection is empty, push Show All an add phrases', 30)");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("EXCEPTION: SQLException in checkForExistAllTablesInDB() from DAO during create main table in main DB");
            }
        }

        if (!statDbExists) {
            try (Statement statement = mainDbConn.createStatement()) {
                System.out.println("Execute " + getCreateMainDb_StatTable_SqlString());
                statement.execute(getCreateMainDb_StatTable_SqlString());
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("EXCEPTION: SQLException in checkForExistAllTablesInDB() from DAO during create stat table in main DB");
            }
        }

    }

    public void setStatistics(Phrase phr){
        String dateTime = phr.ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        int mlseconds = Integer.parseInt(ZonedDateTime.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("SSS")));

        String mode;
        if(phr.howWasAnswered)
            mode = "r_answ";
        else
            mode = "w_answ";

        int learnt = 0;
        if(phr.isLearnt() && !phr.returnUnmodified().isLearnt())
            learnt = 1;
        else if(!phr.isLearnt() && phr.returnUnmodified().isLearnt())
            learnt = -1;

        try (Statement statement = mainDbConn.createStatement()) {
            String sql = "INSERT INTO " + loginBean.getUser() + "_stat" + " VALUES ('" + dateTime + "', " + mlseconds +
                    ", '" + mode + "', " + phr.id + (learnt!=0?(", " + learnt):", NULL") + ")";

            System.out.println(sql);
            statement.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in setStatistics() from DAO");
        }
    }

    public void updateStatistics(Phrase phr){
        String dateTime = phr.ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        String mode;
        if(phr.howWasAnswered)
            mode = "r_answ";
        else
            mode = "w_answ";

        int learnt = 0;
        if(phr.isLearnt() && !phr.returnUnmodified().isLearnt())
            learnt = 1;
        else if(!phr.isLearnt() && phr.returnUnmodified().isLearnt())
            learnt = -1;

        try (Statement statement = mainDbConn.createStatement()) {
            String sql = "UPDATE " + loginBean.getUser() + "_stat" + " SET event='" + mode + "' WHERE date='" + dateTime +"' AND id=" + phr.id
                    + (learnt!=0?(", " + learnt):", NULL");
            System.out.println(sql);
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in updateStatistics from DAO");
        }
    }

    public ArrayList<Phrase> makeInitialCollection(){
        ArrayList<Phrase> list = new ArrayList<>();

        try (Statement statement = mainDbConn.createStatement();
             ResultSet rs = statement.executeQuery
                     ("SELECT * FROM " + loginBean.getUser() + "_stat" + " WHERE date > DATE_ADD(CURRENT_DATE(), INTERVAL 6 HOUR)")) {

            while (rs.next()){
                Phrase phr = null;
                try {
                    phr = getPhraseById(rs.getInt("id"));

                    if(rs.getString("event").equalsIgnoreCase("r_answ"))
                        phr.howWasAnswered = true;
                    else
                        phr.howWasAnswered = false;

                    int learnt = rs.getInt("learnt");

                    if(learnt!=0){
                        if(learnt==1){
                            phr.unmodifiedPhrase.prob = new BigDecimal(6);
                        }else if(learnt == -1){
                            phr.unmodifiedPhrase.prob = new BigDecimal(1);
                        }
                    }

                    phr.ldt = rs.getTimestamp("date").toLocalDateTime().atZone(ZoneId.of("Europe/Helsinki"));

                    list.add(phr);

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

    public ArrayList<Phrase> returnPhrasesList() {
//        System.out.println("CALL: returnPhrasesList() from DAO");
//        ArrayList<Phrase> list = new ArrayList<>();

        return listOfActivePhrases;
    }

    public List<String> reloadLabelsList() {
        //Возвращает список возможных меток для фраз + "All"
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
                double rate = rs.getDouble("rate");
                boolean exactmatch = rs.getBoolean("exactmatch");

                Phrase phrase = new Phrase(id, for_word, nat_word, transcr, prob, create_date, label,
                        last_accs_date, index_start, index_end, exactmatch, rate, this);

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
        for (Phrase phrase : listOfActivePhrases) {
            if (index >= phrase.indexStart && index <= phrase.indexEnd) {
                //Записываем время доступа в объект фразы
                phrase.setTimeOfReturningFromList(System.nanoTime() - startTime);
                return phrase;
            }
        }
        throw new RuntimeException();
    }

    public long[] reloadIndices(int id) {

        long start = System.currentTimeMillis();
        double temp = 0;
        double indOfLW;     //Индекс выпадения изученных
        double rangeOfNLW;  //Диапазон индексов неизученных слов
        double scaleOf1prob;    //rangeOfNLW/summProbOfNLW  цена одного prob

        int countOfModIndices = 0;
        long[] indexes = new long[2];
        totalActiveWords = listOfActivePhrases.size(); //Считаем общее количество фраз

        //Считаем неизученные слова, summProbOfNLW и очищаем индексы
        nonLearnedWords = 0;
        summProbOfNLW = 0;
        summProbOfLW = 0;
        for (Phrase phr : listOfActivePhrases) {
            phr.indexStart = phr.indexEnd = 0;
            if (phr.prob.doubleValue() > 3) {
                nonLearnedWords++;
                summProbOfNLW += phr.prob.doubleValue();
            }else {
                summProbOfLW += phr.prob.doubleValue();
            }
        }

        //Считаем изученные (learnedWords)
        learnedWords = totalActiveWords - nonLearnedWords;
        indOfLW = CHANCE_OF_APPEARING_LEARNED_WORDS / learnedWords;
        rangeOfNLW = learnedWords > 0 ? 1 - CHANCE_OF_APPEARING_LEARNED_WORDS : 1;
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

    private boolean pushIntoStack(Phrase id) {
        //Помещает данный в качестве параметра Id фразы в стек, стек используется для предотвращения повторения фраз
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
                    tempPhrase.label, tempPhrase.lastAccs, tempPhrase.indexStart, tempPhrase.indexEnd, tempPhrase.exactMatch, tempPhrase.rate, this);
            phrase.timeOfReturningFromList = "";
        } while (pushIntoStack(phrase));


        System.out.println("Phrase is " + phrase.natWord + " " +
                "indexes are: (" + phrase.indexStart + " - " + phrase.indexEnd + ")");
        return phrase;
    }

    //Access to DB -------------------------------------

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

    public void updatePhrase(Phrase givenPhrase) {
        System.out.println("CALL: updatePhrase(Phrase givenPhrase) from DAO with id=" + givenPhrase.id);
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

        phraseInTheCollection.forWord = givenPhrase.forWord;
        phraseInTheCollection.natWord = givenPhrase.natWord;
        phraseInTheCollection.transcr = givenPhrase.transcr;
        phraseInTheCollection.lastAccs = givenPhrase.lastAccs;
        phraseInTheCollection.exactMatch = givenPhrase.exactMatch;
        phraseInTheCollection.label = givenPhrase.label;
        phraseInTheCollection.prob = givenPhrase.prob;
        phraseInTheCollection.rate = givenPhrase.rate;

        try (PreparedStatement mainDbPrepStat = mainDbConn.prepareStatement(updateSql)) {

            mainDbPrepStat.setString(1, givenPhrase.forWord);
            mainDbPrepStat.setString(2, givenPhrase.natWord);

            if (givenPhrase.transcr == null || givenPhrase.transcr.equalsIgnoreCase(""))
                mainDbPrepStat.setString(3, null);
            else
                mainDbPrepStat.setString(3, givenPhrase.transcr);

            if (givenPhrase.label == null || givenPhrase.label.equalsIgnoreCase(""))
                mainDbPrepStat.setString(6, null);
            else
                mainDbPrepStat.setString(6, givenPhrase.label);

            mainDbPrepStat.setString(4, dateTime);
            mainDbPrepStat.setBoolean(5, givenPhrase.exactMatch);
            mainDbPrepStat.setDouble(7, givenPhrase.prob.doubleValue());
            mainDbPrepStat.setDouble(8, givenPhrase.rate);
            mainDbPrepStat.execute();
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase givenPhrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        reloadCollectionOfPhrases();

    }

    public long[] updateProb(Phrase phrase) {
        System.out.println("CALL: updateProb(Phrase phrase) with id=" + phrase.id + " from DAO");
        String dateTime = ZonedDateTime.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        try {
            getPhraseById(phrase.id).prob = phrase.prob;
            getPhraseById(phrase.id).rate = phrase.rate;
        } catch (PhraseNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        try (Statement st = mainDbConn.createStatement()) {
            st.execute("UPDATE " + loginBean.getUser() + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime + "', rate=" + phrase.rate +
                    " WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }


        return reloadIndices(phrase.id);
    }




}

