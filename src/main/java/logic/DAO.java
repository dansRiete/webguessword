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
    private String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private String localHost = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public String table;
    public HashSet<String> chosedLabels;
    public ArrayList<String> labels = new ArrayList<>();
    public double learnedWords;
    public double nonLearnedWords;
    public double totalWords;
    final double chanceOfLearnedWords = 1d/15d;

//    private Id[] idsArr;
    private Phrase[] lastPhrasesStack;
    private int stackNum;
    public Connection mainDbConn;
//    public Connection inMemDbConn;
    private LoginBean loginBean;
    private ArrayList<Phrase> listOfPhrases = new ArrayList<>();

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
            String str =  "CREATE TABLE " + loginBean.getUser() + "\n"  +
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

    public DAO(LoginBean loginBean){

        String dbConnected = null;
        this.loginBean = loginBean;
        table = loginBean.getUser();

        //>>>Получаем подключение к основной БД, в случае ошибки пробуем подключиться через локальнй хост (только для режима тестирования)
        try{
            mainDbConn = DriverManager.getConnection(remoteHost, "adminLtuHq9R", "d-AUIKakd1Br");
            dbConnected = "- Remote DB was connected";
        }catch (SQLException e){
            try{
                mainDbConn = DriverManager.getConnection(localHost, "adminLtuHq9R", "d-AUIKakd1Br");
                dbConnected = "- Local DB was connected" ;
            }catch (SQLException e1){
                e1.printStackTrace();
                System.out.println("EXCEPTION: in DAO constructor");
                throw new DataBaseConnectionException();
            }
        }
        System.out.println("CALL: DAO constructor " + dbConnected);
        //<<<

        checkTables();
        reloadCollectionOfPhrases();
        reloadLabelsList();

        //>>>Создаём базу данных в оперативной памяти (используется для увеличения производительности)
        /*try {
            inMemDbConn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.out.println("Exception during creating in-memory database connection");
            e.printStackTrace();
        }*/
        //<<<



    }

    /**
     * Проверяет наличие соответствующих таблиц БД необходимых для работы приложения
     */
    private void checkTables(){
        boolean mainDbExists = false;
        boolean statDbExists = false;
        try (ResultSet rs_tables = mainDbConn.createStatement().executeQuery
                ("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='guessword'")){
            while (rs_tables.next()){
                //Проверяем или в БД существует основная таблица фраз
                if(rs_tables.getString("TABLE_NAME").equals(loginBean.getUser()))
                    mainDbExists = true;
                //Проверяем или в БД существует статистическая таблица
                if(rs_tables.getString("TABLE_NAME").equals(loginBean.getUser()+"_stat"))
                    statDbExists = true;
            }
        }catch (SQLException e){
            e.printStackTrace();
            System.out.println("EXCEPTION: SQLException in checkTables() from DAO");
        }

        if(!mainDbExists){
            try(Statement statement = mainDbConn.createStatement()){
                statement.execute(getCreateMainDb_MainTable_SqlString());
                statement.execute("INSERT INTO " + loginBean.getUser() + " (for_word, nat_word, prob_factor) VALUES ('The', " +
                        "'The collection is empty, push Show All an add phrases', 30)");
            }catch (SQLException e){
                e.printStackTrace();
                System.out.println("EXCEPTION: SQLException in checkTables() from DAO during create main table in main DB");
            }

        }
    }

    public ArrayList<Phrase> returnPhrasesList(){
        System.out.println("CALL: returnPhrasesList() from DAO");
        ArrayList<Phrase> list = new ArrayList<>();


        /*try (Statement st = inMemDbConn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + loginBean.getUser() + " ORDER BY create_date DESC, id DESC")){
            while (rs.next()){
                list.add(new Phrase(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getBigDecimal("prob_factor"),
                        rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"), 0, 0, rs.getBoolean("exactmatch"), this));
            }
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in returnPhrasesList() in DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }*/



        return list;
    }

    /**
     *
     * @return Возвращает список возможных меток для фраз + "All"
     */
    public List<String> reloadLabelsList(){
        System.out.println("CALL: reloadLabelsList() from DAO");
        labels.clear();
        labels.add("All");
        String temp = null;
        HashSet<String> hsset = new HashSet<>();

        /*try (Statement st = inMemDbConn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + loginBean.getUser() + " ORDER BY LABEL")){

            while (rs.next()){
                temp = rs.getString("LABEL");
                labels.add(temp== null ? "null":temp);
            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION: in reloadLabelsList() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }*/

        for(Phrase phrase : listOfPhrases){
            hsset.add(phrase.label);
        }

        for(String str : hsset){
            labels.add(str);
        }

        return labels;

    }

    public ArrayList<Phrase> getCurrList(){
        /*ArrayList<Phrase> phrases = new ArrayList<>();
        try(Statement inMemSt = inMemDbConn.createStatement(); ResultSet rs = inMemSt.executeQuery("SELECT * FROM " + loginBean.getUser())){
            while (rs.next()){
                int id = rs.getInt("id");
                System.out.println("Id is" + id);
                Id currID = getIdById(id);
//                System.out.println("currID.index_start" + currID.index_start);
                Phrase phrase = new Phrase(id, rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), new BigDecimal(rs.getDouble("prob_factor")),
                        rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                        currID.index_start, currID.index_end, rs.getBoolean("exactmatch"), this);
//                System.out.println("phrase.indexStart="+phrase.indexStart);
                phrases.add(phrase);
            }

        }catch (SQLException e){
            System.out.println("Exception in ResultSet getCurrList() from DAO");
            e.printStackTrace();
        }*/
        return listOfPhrases;
    }

    public void reloadCollectionOfPhrases(){

        //If in-memory db is not empty, then to empty it.
        /*try (Statement inMemSt = inMemDbConn.createStatement()){
            inMemSt.execute("DROP TABLE " + loginBean.getUser());
        } catch (SQLException e) {
            //Table doesn't exist - do nothing.
        }*/

        //Creating table in in-memory DB
        /*try(Statement inMemSt = inMemDbConn.createStatement()){
            inMemSt.execute(getCreateInmemDb_MainTable_SqlString());
        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException();
        }*/

        //Populating in-memory DB by values from main DB

        String insertSql = "INSERT INTO " + loginBean.getUser() +
                "(id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
                "index_start, index_end, exactmatch) VALUES (?,?,?,?,?,?,?,?,?,?,?)";



        try (Statement mainSt = mainDbConn.createStatement();
             ResultSet rs1 = mainSt.executeQuery("SELECT * FROM " + loginBean.getUser())){

            listOfPhrases.clear();
            while (rs1.next()) {

                int id = rs1.getInt("id");
                String for_word = rs1.getString("for_word");
                String nat_word = rs1.getString("nat_word");
                String transcr = rs1.getString("transcr");
                BigDecimal prob = new BigDecimal(rs1.getDouble("prob_factor"));
                Timestamp create_date = rs1.getTimestamp("create_date");
                String label = rs1.getString("label");
                Timestamp last_accs_date = rs1.getTimestamp("last_accs_date");
                double index_start = rs1.getDouble("index_start");
                double index_end = rs1.getDouble("index_end");
                boolean exactmatch = rs1.getBoolean("exactmatch");

                listOfPhrases.add(
                        new Phrase(id, for_word, nat_word, transcr, prob, create_date, label,
                        last_accs_date, index_start, index_end, exactmatch, this)
                );

            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in reloadCollectionOfPhrases() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        reloadIndices(1);
        System.out.println("CALL: reloadCollectionOfPhrases() from DAO " + listOfPhrases.size() + " elements were added");
    }

    private Phrase getPhraseById(int id){
        for(Phrase phrase : listOfPhrases){
            if(phrase.id == id)
                return phrase;
        }
//        return null;
        throw new RuntimeException();
    }

    private Phrase getPhraseByIndex(long index){
        for(Phrase phrase : listOfPhrases){
            if(index>=phrase.indexStart && index<=phrase.indexEnd)
                return phrase;
        }
        throw new RuntimeException();
    }

    public long[] updateProb(Phrase phrase){
        System.out.println("CALL: updateProb(Phrase phrase) with id=" + phrase.id +" from DAO");
        String dateTime = ZonedDateTime.now(ZoneId.of("Europe/Kiev")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        /*try (Statement inMemDbPrepStat = inMemDbConn.createStatement()){
            inMemDbPrepStat.executeUpdate("UPDATE " + loginBean.getUser() + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime +
                    "' WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }*/
        getPhraseById(phrase.id).prob=phrase.prob;

        new Thread(){
            public void run(){
                try (Statement st = mainDbConn.createStatement()){
                    st.execute("UPDATE " + loginBean.getUser() + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime +
                            "' WHERE id=" + phrase.id);
                } catch (SQLException e) {
                    System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }.start();
        return reloadIndices(phrase.id);
    }

    public void updatePhrase(Phrase phrase){
        System.out.println("CALL: updatePhrase(Phrase phrase) from DAO with id=" + phrase.id);
        String dateTime = ZonedDateTime.now(ZoneId.of("Europe/Kiev")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String updateSql = "UPDATE " + loginBean.getUser() + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                "exactmatch=?, label=?, prob_factor=?  WHERE id =" + phrase.id;

        /*try (PreparedStatement inMemDbPrepStat = inMemDbConn.prepareStatement(updateSql)){

            inMemDbPrepStat.setString(1, phrase.forWord);
            inMemDbPrepStat.setString(2, phrase.natWord);

            if(phrase.transcr==null||phrase.transcr.equalsIgnoreCase(""))
                inMemDbPrepStat.setString(3, null);
            else
                inMemDbPrepStat.setString(3, phrase.transcr);

            if(phrase.label==null||phrase.label.equalsIgnoreCase(""))
                inMemDbPrepStat.setString(6, null);
            else
                inMemDbPrepStat.setString(6, phrase.label);
            inMemDbPrepStat.setDouble(7, phrase.prob.doubleValue());

            inMemDbPrepStat.setString(4, dateTime);
            inMemDbPrepStat.setBoolean(5, phrase.exactMatch);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }*/
        Phrase phr = getPhraseById(phrase.id);
        phr.forWord = phrase.forWord;
        phr.natWord = phrase.natWord;
        phr.transcr = phrase.transcr;
        phr.lastAccs = phrase.lastAccs;
        phr.exactMatch = phrase.exactMatch;
        phr.label = phrase.label;
        phr.prob = phrase.prob;

        new Thread(){
            public void run(){
                try (PreparedStatement mainDbPrepStat = mainDbConn.prepareStatement(updateSql)){

                    mainDbPrepStat.setString(1, phrase.forWord);
                    mainDbPrepStat.setString(2, phrase.natWord);

                    if(phrase.transcr==null||phrase.transcr.equalsIgnoreCase(""))
                        mainDbPrepStat.setString(3, null);
                    else
                        mainDbPrepStat.setString(3, phrase.transcr);

                    if(phrase.label==null||phrase.label.equalsIgnoreCase(""))
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
            }
        }.start();
    }

    public void deletePhrase(Phrase phr){
        System.out.println("CALL: deletePhrase(int id) from DAO");
        String deleteSql = "DELETE FROM " + loginBean.getUser() + " WHERE ID=" + phr.id;
        /*try (Statement st = inMemDbConn.createStatement()){
            st.execute(deleteSql);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in deletePhrase(int id) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }*/
        listOfPhrases.remove(getPhraseById(phr.id));

        new Thread(){
            public void run(){
                try (Statement st = mainDbConn.createStatement()) {
                    st.execute(deleteSql);
                } catch (SQLException e) {
                    System.out.println("EXCEPTION#2: in deletePhrase(int id) from DAO");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }.start();
    }

    public Connection getConnection(){
        return mainDbConn;
    }

    /*private Id getIdById(int id){
        for(Id id1 : idsArr){
            if(id1.id==id){
                return id1;
            }
        }
        return null;
    }

    private Id getIdByIndex(long randIndex){
        for(Id id : idsArr){
            if(randIndex>=id.index_start&&randIndex<=id.index_end){
                return id;
            }
        }
        return null;
    }

    public void setProbById(int id, double prob){
        getIdById(id).prob = prob;
    }*/

    public long[] reloadIndices(int id){

        long start = System.currentTimeMillis();
        double temp = 0;
        double indOfLW;     //Индекс выпадения изученных
        double rangeOfNLW;  //Диапазон индексов неизученных слов
        double scaleOf1prob;    //rangeOfNLW/summProbOfNLW  цена одного prob
        ArrayList<Integer> idArr = new ArrayList<>();
        ResultSet rs = null;
        int summProbOfNLW = 0;
        long[] indexes = new long[2];


//        try (Statement statement = inMemDbConn.createStatement()){

            //Заполняем idArr айдишниками
            /*rs = statement.executeQuery("SELECT id FROM " + table);
            while(rs.next())
                idArr.add(rs.getInt("ID"));*/
            for(Phrase phr : listOfPhrases){
                idArr.add(phr.id);
            }

            //Считаем неизученные слова
            /*rs = statement.executeQuery("SELECT COUNT(prob_factor) FROM " + table + " WHERE prob_factor>3");
            rs.next();
            nonLearnedWords = rs.getInt(1);*/
            nonLearnedWords = 0;
            summProbOfNLW = 0;
            for(Phrase phr : listOfPhrases){
                if(phr.prob.doubleValue() > 3 && phr.inLabels(chosedLabels)){
                    nonLearnedWords++;
                    summProbOfNLW+=phr.prob.doubleValue();
                }
            }

            //Считаем общее количество фраз
            /*rs = statement.executeQuery("SELECT COUNT(*) FROM " + loginBean.getUser());
            rs.next();
            totalWords = rs.getInt(1);*/
            totalWords = listOfPhrases.size();
            learnedWords = totalWords - nonLearnedWords;


            /*rs = statement.executeQuery("SELECT SUM(prob_factor) FROM " + table + " WHERE prob_factor>3");
            rs.next();
            summProbOfNLW = rs.getInt(1);*/


            /*rs = statement.executeQuery("SELECT COUNT(prob_factor) FROM " + table + " WHERE prob_factor<3");
            rs.next();
            learnedWords = rs.getInt(1);*/

        /*} catch (SQLException e) {
            System.out.println("EXCEPTION#1: in reloadIndices() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        } finally {
            try {
                if(rs!=null)
                    rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }*/

        indOfLW = chanceOfLearnedWords/learnedWords;
        rangeOfNLW = learnedWords>0?1-chanceOfLearnedWords:1;
        scaleOf1prob = rangeOfNLW/summProbOfNLW;
        if(nonLearnedWords==0){
            System.out.println("Все слова выучены!");
        }
        int countOfModIndices = 0;

        //Clears indexes before reloading
        /*for(Id id2 : idsArr)
            id2.index_start = id2.index_end = 0;*/
        for(Phrase phr : listOfPhrases){
            phr.indexStart = phr.indexEnd = 0;
        }

        try {
                for (int i : idArr) { //Устанавилвает индексы для неизученных слов

                    long indexStart;
                    long indexEnd;

                    //Переменной prob присваивается prob фразы с currentPhraseId = i;
                    double prob;
                    Phrase phrase = getPhraseById(i);
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
                    if(i==id){
                        indexes[0]=indexStart;
                        indexes[1]=indexEnd;
                    }
            }
        }finally {
            try {
                if(rs!=null)
                    rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("CALL: reloadIndices() from DAO" + "Indexes changed="+countOfModIndices + " Time taken " + (System.currentTimeMillis()-start) + "ms");
        return indexes;
    }

    /**
     * Помещает данный в качестве параметра Id фразы в стек, стек используется для предотвращения повторения фраз
     * @param id добавляемая фраза
     * @return true если фраза присутствует в стеке, false если отсутствует
     */
    private boolean pushIntoStack(Phrase id){
        StringBuilder msg = new StringBuilder("CALL: pushIntoStack(Phrase id) from DAO;");
        StringBuilder stackContent = new StringBuilder("Содержимое стека [");
        //Если массив не инстантиирован или количество фраз стало меньше чем размер массива-стека заново создаём его с
        // нужным размером и обнуляем положение стека
        boolean result = false;
        if(lastPhrasesStack==null||lastPhrasesStack.length>totalWords){
            //Если количество фраз больше или равно 7 создаётся стандартный стек на 7 элементов.
            if(totalWords>=7){
                lastPhrasesStack = new Phrase[7];
                stackNum = 0;
                msg.append(" создан стек на 7 элементов;");
            }
            //Если количество фраз меньше 3 в стеке нет надобности, метод возвращает false
            else if(totalWords<3) {
                msg.append(" в стеке нет необходимости - выход из метода (возврат false);");
                System.out.println(msg);
                return false;
            }
            //Если кол-во слов меньше 7 но больше трёх создаётся стек на (кол-во слов минус 1) элементов
            else {
                msg.append(" создан стек на " + ((int) totalWords - 1) + " элементов;");
                lastPhrasesStack = new Phrase[(int) totalWords - 1];
                stackNum = 0;
            }
        }
        //Проверяем или стек не содержит айдишник данной в качестве параметра фразы
        for(Phrase phrase : lastPhrasesStack){
            if(phrase != null && phrase.id == id.id){
                msg.append(" в стеке уже есть фраза " + id.forWord + " метод возвращает true;");
                result = true;
                break;
            }
        }
        if(!result){
            msg.append(" в стек помещается фраза \"" + id.natWord + "\";");
            lastPhrasesStack[stackNum>lastPhrasesStack.length-1?stackNum=0:stackNum] = id;
            stackNum++;
        }

        for(Phrase phrase : lastPhrasesStack){
            if(phrase!=null){
                stackContent.append(phrase.id + ", ");
            }else {
                stackContent.append("null, ");
            }
        }
        stackContent.append("]");
        System.out.println(msg);
        System.out.println(stackContent);
        return result;
    }

    public Phrase createRandPhrase(){

//        Phrase phrase;
        Phrase phrase = null;

        //Новая фраза создаётся пока не подтвердится, что она отсутствует в стеке(последние 7 фраз)
        do {
            int index = random.nextInt(1000000000);
            phrase = getPhraseByIndex(index);
        }while (pushIntoStack(phrase));

        /*String sql = "SELECT * FROM " + table + " WHERE id=" + currId.id;

        try (Statement st = inMemDbConn.createStatement(); ResultSet rs = st.executeQuery(sql)){
            rs.next();
            phrase = new Phrase(currId.id, rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"),
                    new BigDecimal(rs.getDouble("prob_factor")), rs.getTimestamp("create_date"), rs.getString("label"),
                    rs.getTimestamp("last_accs_date"),  currId.index_start, currId.index_end, rs.getBoolean("exactmatch"), this);
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in createRandPhrase() from DAO SQL was " + sql);
            e.printStackTrace();
            throw new RuntimeException();
        }*/
        System.out.println("CALL: createRandPhrase() from DAO phrase is " + phrase.natWord + " " +
                "indexes are: ("+phrase.indexStart + " - " + phrase.indexEnd + ")");
        return phrase;
    }

    public void insertPhrase(Phrase phrase){
        /*System.out.println("CALL: insertPhrase(Phrase phrase) from DAO");
        String insertSql = "INSERT INTO " + table + " (for_word, nat_word, transcr, prob_factor, create_date," +
                " label, last_accs_date, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = inMemDbConn.prepareStatement(insertSql)) {

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
            System.out.println("EXCEPTION: in insertPhrase(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        reloadCollectionOfPhrases();
        reloadIndices(1);

        new Thread(){
            public void run(){
                try (PreparedStatement ps = mainDbConn.prepareStatement(insertSql)){
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
                    System.out.println("EXCEPTION inside new Thread: in insertPhrase(Phrase phrase) from DAO");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }.start();*/
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
