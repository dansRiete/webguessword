package logic;

import Exceptions.DataBaseConnectionException;
import beans.LoginBean;

import java.math.BigDecimal;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Aleks on 11.05.2016.
 */
public class DAO {

    private Random random = new Random();
    public String timezone = "Europe/Kiev";
    private String remoteHost = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private String localHost = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    public String table;
    private String user;
    private String password;
    public ArrayList<String> labels = new ArrayList<>();
    public double learnedWords;
    public double nonLearnedWords;
    public double totalWords;
    final double chanceOfLearnedWords = 1d/15d;
    private boolean isCopyDbExecuted;
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";//
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
    private Id[] idsArr;
//    ComboPooledDataSource cpds1 = new ComboPooledDataSource();
    public Connection mainDbConn;
    public Connection inMemDbConn;
//    ComboPooledDataSource cpds2 = new ComboPooledDataSource();

    private String createTableSql() {
     return "CREATE TABLE " + user + "\n" +
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
    private String createNewTable = "CREATE TABLE guessword.aleks\n" +
            "(\n" +
            "  id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
            "  for_word VARCHAR(250) NOT NULL,\n" +
            "  nat_word VARCHAR(250) NOT NULL,\n" +
            "  transcr VARCHAR(100),\n" +
            "  prob_factor DOUBLE,\n" +
            "  label VARCHAR(50),\n" +
            "  create_date TIMESTAMP NULL,\n" +
            "  last_accs_date TIMESTAMP NULL,\n" +
            "  exactmatch BOOLEAN,\n" +
            "  index_start DOUBLE,\n" +
            "  index_end DOUBLE\n" +
            ");\n" +
            "ALTER TABLE guessword.aleks ADD CONSTRAINT unique_id UNIQUE (id);";

    public DAO(String user){
        System.out.println("CALL: DAO constructor");
        this.user = user;
        try{
            mainDbConn = DriverManager.getConnection(remoteHost, "adminLtuHq9R", "d-AUIKakd1Br");
            System.out.println("--- Remote DB was connected");
        }catch (SQLException e){
            try{
                mainDbConn = DriverManager.getConnection(localHost, "adminLtuHq9R", "d-AUIKakd1Br");
                System.out.println("--- Local DB was connected");
            }catch (SQLException e1){
                e1.printStackTrace();
                System.out.println("EXCEPTION: in DAO constructor");
                throw new DataBaseConnectionException();
            }
        }

        inMemDbConn = getDBConnection();//
        /*try {
            cpds1.setDriverClass( "com.mysql.jdbc.Driver" );
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
        cpds1.setJdbcUrl(remoteHost);
        cpds1.setUser("adminLtuHq9R");
        cpds1.setPassword("d-AUIKakd1Br");
        cpds1.setMinPoolSize(5);
        cpds1.setAcquireIncrement(5);
        cpds1.setMaxPoolSize(20);
*/
        /*try {
            cpds2.setDriverClass( "org.h2.Driver" );
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
        cpds2.setJdbcUrl(DB_CONNECTION);
        cpds2.setUser(DB_USER);
        cpds2.setPassword(DB_PASSWORD);
        cpds2.setMinPoolSize(5);
        cpds2.setAcquireIncrement(5);
        cpds2.setMaxPoolSize(20);*/

    }

    public ArrayList<Phrase> returnPhrasesList(){
        System.out.println("CALL: returnPhrasesList() from DAO");
        ArrayList<Phrase> list = new ArrayList<>();
        try (Statement st = inMemDbConn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + user + " ORDER BY create_date DESC, id DESC")){
            while (rs.next()){
                list.add(new Phrase(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getBigDecimal("prob_factor"),
                        rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"), 0, 0, rs.getBoolean("exactmatch"), this));
            }
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in returnPhrasesList() in DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        return list;
    }

    public List<String> returnLabelsList(){
        System.out.println("CALL: returnLabelsList() from DAO");
        ArrayList<String> labelsList = new ArrayList<>();
//        labelsList.add("");
        try (Statement st = inMemDbConn.createStatement(); ResultSet rs = st.executeQuery("SELECT DISTINCT label FROM " + user)){

            while (rs.next()){
                if(rs.getString(1)!=null&&!rs.getString(1).equals(""))
                    labelsList.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in returnLabelsList() in DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        return labelsList;

    }

    public ArrayList<Phrase> getCurrList(){
        ArrayList<Phrase> phrases = new ArrayList<>();
        try(Statement inMemSt = inMemDbConn.createStatement(); ResultSet rs = inMemSt.executeQuery("SELECT * FROM " + user)){
            while (rs.next()){
                int id = rs.getInt("id");
                System.out.println("Id is" + id);
                Id currID = getIDById(id);
                System.out.println("currID.index_start" + currID.index_start);
                Phrase phrase = new Phrase(id, rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), new BigDecimal(rs.getDouble("prob_factor")),
                        rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                        currID.index_start, currID.index_end, rs.getBoolean("exactmatch"), this);
                System.out.println("phrase.indexStart="+phrase.indexStart);
                phrases.add(phrase);
            }

        }catch (SQLException e){
            System.out.println("Exception in ResultSet getCurrList() from DAO");
            e.printStackTrace();
        }
        return phrases;
    }

    private void copyDb(){

        System.out.println("CALL: copyDb() from DAO");

        //If in-memory db is not empty, then to empty it.
        try (Statement inMemSt = inMemDbConn.createStatement()){
            inMemSt.execute("DROP TABLE " + user);
        } catch (SQLException e) {
            //Table doesn't exist - do nothing.
        }

        //Creating table in in-memory DB
        try(Statement inMemSt = inMemDbConn.createStatement()){
            inMemSt.execute(createTableSql());
        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException();
        }

        //Populating in-memory DB by values from main DB

        String insertSql = "INSERT INTO " + user +
                "(id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
                "index_start, index_end, exactmatch) VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        int counter;
        try(Statement mainSt = mainDbConn.createStatement(); ResultSet rs2 = mainSt.executeQuery("SELECT COUNT(*) FROM " + user)){
            rs2.next();
            counter = rs2.getInt(1);
        }catch (SQLException e) {
            System.out.println("EXCEPTION#1: in copyDb() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        try (Statement mainSt = mainDbConn.createStatement();
             ResultSet rs1 = mainSt.executeQuery("SELECT * FROM " + user);
               PreparedStatement ps = inMemDbConn.prepareStatement(insertSql)){


            idsArr = new Id[counter];
            counter = 0;
            while (rs1.next()) {
                int id;
                double prob;
                id = rs1.getInt("id");
                ps.setInt(1, id);
                ps.setString(2, rs1.getString("for_word"));
                ps.setString(3, rs1.getString("nat_word"));
                ps.setString(4, (rs1.getString("transcr") == null ? null : rs1.getString("transcr")));
                prob = rs1.getDouble("prob_factor");
                ps.setDouble(5, prob);
                ps.setTimestamp(6, rs1.getTimestamp("create_date"));
                ps.setString(7, (rs1.getString("label") == null ? null : rs1.getString("label")));
                ps.setTimestamp(8, (rs1.getTimestamp("last_accs_date") == null ? null : rs1.getTimestamp("last_accs_date")));
                ps.setDouble(9, rs1.getDouble("index_start"));
                ps.setDouble(10, rs1.getDouble("index_end"));
                ps.setBoolean(11, rs1.getBoolean("exactmatch"));
                idsArr[counter++] = new Id(id, prob, 0, 0);
                ps.execute();
            }

            System.out.println("--- Added " + counter + " elements into in-memoryDb");


        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in copyDb() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        reloadIndices(1);
    }

    private Connection getDBConnection() {
        System.out.println("CALL: getDBConnection() from DAO");
        Connection dbConnection;

        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in getDBConnection() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        return dbConnection;
    }

    public long[] updateProb(Phrase phrase){
        System.out.println("CALL: updateProb(Phrase phrase) with id=" + phrase.id +" from DAO");
//        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String dateTime = ZonedDateTime.now(ZoneId.of("Europe/Kiev")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        try (Statement inMemDbPrepStat = inMemDbConn.createStatement()){
//            System.out.println("--- updateProb() SQL UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='"+timestamp+"' WHERE id="+phrase.id);
            inMemDbPrepStat.executeUpdate("UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime + "' WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        new Thread(){
            public void run(){
                try (Statement st = mainDbConn.createStatement()){
                    st.execute("UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime + "' WHERE id=" + phrase.id);
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
        String updateSql = "UPDATE " + user + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                "exactmatch=?, label=?, prob_factor=?  WHERE id =" + phrase.id;

        try (PreparedStatement inMemDbPrepStat = inMemDbConn.prepareStatement(updateSql)){

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
        }
        copyDb();
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
        String deleteSql = "DELETE FROM " + user + " WHERE ID=" + phr.id;
        try (Statement st = inMemDbConn.createStatement()){
            st.execute(deleteSql);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in deletePhrase(int id) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        copyDb();
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

    public void setLoginBean(LoginBean loginBean){
        System.out.println("CALL: setLoginBean(LoginBean loginBean) from DAO");
        user = loginBean.getUser();
        password = loginBean.getPassword();
        table = user;
        System.out.println("setLoginBean() in DAO user is " + user);
        if(!isCopyDbExecuted){
            copyDb();
            isCopyDbExecuted = true;
        }
        reloadLabelsList();

    }

    public Connection getConnection(){
        return mainDbConn;
    }

    public void reloadLabelsList(){
        System.out.println("CALL: reloadLabelsList() from DAO");
        labels.clear();
        labels.add("All");
        String temp = null;
        try (Statement st = inMemDbConn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + user + " ORDER BY LABEL")){

            while (rs.next()){
                temp = rs.getString("LABEL");
                labels.add(temp== null ? "null":temp);
            }

        } catch (SQLException e) {
            System.out.println("EXCEPTION: in reloadLabelsList() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private Id getIDById(int id){
        for(Id id1 : idsArr){
            if(id1.id==id){
                return id1;
            }
        }
        return null;
    }

    private Id getIDByIndex(long randIndex){
        for(Id id : idsArr){
            if(randIndex>=id.index_start&&randIndex<=id.index_end){
                return id;
            }
        }
        return null;
    }

    public void setProbById(int id, double prob){
        getIDById(id).prob = prob;
    }

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

        //Заполняем idArr айдишниками
        try (Statement statement = inMemDbConn.createStatement()){

            rs = statement.executeQuery("SELECT id FROM " + table);
            while(rs.next())
                idArr.add(rs.getInt("ID"));

            rs = statement.executeQuery("SELECT COUNT(prob_factor) FROM " + table + " WHERE prob_factor>3");
            rs.next();
            nonLearnedWords = rs.getInt(1);
//            System.out.println("nonLearnedWords: " + nonLearnedWords);

            rs = statement.executeQuery("SELECT COUNT(*) FROM " + user);
            rs.next();
            totalWords = rs.getInt(1);

            rs = statement.executeQuery("SELECT SUM(prob_factor) FROM " + table + " WHERE prob_factor>3");
            rs.next();
            summProbOfNLW = rs.getInt(1);
//            System.out.println("summProbOfNLW: " + summProbOfNLW);

            rs = statement.executeQuery("SELECT COUNT(prob_factor) FROM " + table + " WHERE prob_factor<=3");
            rs.next();
            learnedWords = rs.getInt(1);

//            statement.execute("UPDATE " + user + " SET index_start = NULL ");
//            statement.execute("UPDATE " + user + " SET index_end = NULL ");
        } catch (SQLException e) {
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
        }


        indOfLW = chanceOfLearnedWords/learnedWords;
        rangeOfNLW = learnedWords>0?1-chanceOfLearnedWords:1;
//        System.out.println("rangeOfNLW " + rangeOfNLW);
        scaleOf1prob = rangeOfNLW/summProbOfNLW;
        if(nonLearnedWords==0){
            System.out.println("Все слова выучены!");
        }
        int countOfModIndices=0;
        int test = 0;

        //Clears indexes before reloading
        for(Id id2 : idsArr)
            id2.index_start = id2.index_end = 0;

        try (Statement statement = inMemDbConn.createStatement()){
            for (int i : idArr) { //Устанавилвает индексы для неизученных слов

                long indexStart;
                long indexEnd;
//                System.out.println("--- for(" + i + ": idArr)");

                //Переменной prob присваивается prob фразы с currentPhraseId = i;
//                rs = statement.executeQuery("SELECT prob_factor FROM " + user + " WHERE id=" + i);
                double prob;
//                rs.next();
//                prob = rs.getFloat(1);
                prob = getIDById(i).prob;
                //            System.out.println("prob=" + prob);


                //Если nonLearnedWords == 0, то есть, все слова выучены устанавливаются равные для всех индексы
                if (nonLearnedWords == 0) {
                    indexStart = Math.round(temp * 1000000000);
//                    statement.execute("UPDATE " + user + " SET index_start=" + indexStart + " WHERE id=" + i);
                    getIDById(i).index_start = indexStart;
                    temp += chanceOfLearnedWords / learnedWords;
                    indexEnd = Math.round((temp * 1000000000) - 1);
//                    statement.execute("UPDATE " + user + " SET index_end=" + indexEnd + " WHERE id=" + i);
                    getIDById(i).index_end = indexEnd;
//                    System.out.println("index_start="+indexStart + " index_end="+indexEnd);
                } else { //Если нет, то индексы ставяться по алгоритму
                    if (prob > 3) {
                        //                    System.out.println("UPDATE ALEKS SET INDEX1=" + Math.round(temp*1000000000) + " WHERE ID=" + i);
                        indexStart = Math.round(temp * 1000000000);
//                        statement.execute("UPDATE " + user + " SET index_start=" + indexStart + " WHERE id=" + i);
                        getIDById(i).index_start = indexStart;
//                        double i1 = temp;
                        temp += scaleOf1prob * prob;
                        //                    System.out.println("UPDATE ALEKS SET INDEX2=" + Math.round((temp *1000000000)-1) + " WHERE ID=" + i);
                        //                    System.out.println("%=" + (temp - MINFLOAT-i1));
                        indexEnd = Math.round((temp * 1000000000) - 1);
//                        statement.execute("UPDATE " + user + " SET index_end=" + indexEnd + " WHERE id=" + i);
                        getIDById(i).index_end = indexEnd;
//                        System.out.println("index_start="+indexStart + " index_end="+indexEnd);
                    } else {
                        //                    System.out.println("Index1LW для ID=" + i + "=" + temp);
                        indexStart = Math.round(temp * 1000000000);
//                        statement.execute("UPDATE " + user + " SET index_start=" + indexStart + " WHERE id=" + i);
                        getIDById(i).index_start = indexStart;
                        temp += indOfLW;
                        //                    System.out.println("temp "+temp + "= temp "+temp+"+indOfLW "+indOfLW);
                        //                    System.out.println("Index2LW для ID=" + i + "=" + (temp - 1));
                        indexEnd = Math.round((temp * 1000000000) - 1);
//                        statement.execute("UPDATE " + user + " SET index_end=" + indexEnd + " WHERE id=" + i);
                        getIDById(i).index_end = indexEnd;
//                        System.out.println("index_start="+indexStart + " index_end="+indexEnd);
                    }
                }
                countOfModIndices++;
                if(i==id){
                    indexes[0]=indexStart;
                    indexes[1]=indexEnd;
                }
            }
        }catch (SQLException e){
            System.out.println("EXCEPTION#2: in reloadIndices() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
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

    public Phrase createRandPhrase(){
        System.out.println("CALL: createRandPhrase() from DAO");
        int index = random.nextInt(1000000000);
        Phrase phrase;
//        String sql = "SELECT * FROM " + table + " WHERE index_start<=" + id + " AND index_end>=" + id;
        Id currId = getIDByIndex(index);
        String sql = "SELECT * FROM " + table + " WHERE id=" + currId.id;

        try (Statement st = inMemDbConn.createStatement(); ResultSet rs = st.executeQuery(sql)){
            rs.next();
            phrase = new Phrase(currId.id, rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), new BigDecimal(rs.getDouble("prob_factor")),
                    rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                    currId.index_start, currId.index_end, rs.getBoolean("exactmatch"), this);
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in createRandPhrase() from DAO SQL was " + sql);
            e.printStackTrace();
            throw new RuntimeException();
        }
        return phrase;
    }

    public void insertPhrase(Phrase phrase){
        System.out.println("CALL: insertPhrase(Phrase phrase) from DAO");
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
        }.start();
    }

    public void backupDB(){
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
    }



}
