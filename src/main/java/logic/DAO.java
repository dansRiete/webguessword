package logic;

import Exceptions.DataBaseConnectionException;
import beans.LoginBean;

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
    private String host1 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private String host2 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=true&useTimezone=true&serverTimezone=Europe/Kiev&useSSL=false";
    private Connection mainDbConn;
    private Connection inMemDbConn;
    public String table;
    private String user;
    private String password;
    public ArrayList<String> labels = new ArrayList<>();
    public double learnedWords;
    public double nonLearnedWords;
    final double chanceOfLearnedWords = 1d/15d;
    private boolean isCopyDbExecuted;
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";

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
            mainDbConn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            System.out.println("--- Remote DB was connected");
        }catch (SQLException e){
            try{
                mainDbConn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
                System.out.println("--- Local DB was connected");
            }catch (SQLException e1){
                e1.printStackTrace();
                System.out.println("EXCEPTION: in DAO constructor");
                throw new DataBaseConnectionException();
            }
        }
        inMemDbConn = getDBConnection();
    }

    public ArrayList<PhraseDb> returnPhrasesList(){
        System.out.println("CALL: returnPhrasesList() from DAO");
        ArrayList<PhraseDb> list = new ArrayList<>();
        try {
            Statement st = inMemDbConn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + user);
            while (rs.next()){
                list.add(new PhraseDb(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getDouble("prob_factor"),
                        rs.getString("label"), rs.getTimestamp("create_date"), rs.getTimestamp("last_accs_date"), rs.getBoolean("exactmatch")));
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
        try {
            Statement st = inMemDbConn.createStatement();
            ResultSet rs = st.executeQuery("SELECT DISTINCT label FROM " + user);
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

    private void copyDb(){

        System.out.println("CALL: copyDb() from DAO");
        Statement inMemSt = null;
        try {
            inMemSt = inMemDbConn.createStatement();
            inMemSt.execute("DROP TABLE " + user);
        } catch (SQLException e) {
        }

        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            inMemSt.execute(createTableSql());
            Statement mainSt = mainDbConn.createStatement();
            rs = mainSt.executeQuery("SELECT * FROM " + user);
            ps = inMemDbConn.prepareStatement("INSERT INTO " + user +
                    "(id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
                    "index_start, index_end, exactmatch) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in copyDb() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }

        int id = 0;
        try {

            while (rs.next()) {
                id = rs.getInt("id");
                ps.setInt(1, id);
                ps.setString(2, rs.getString("for_word"));
                ps.setString(3, rs.getString("nat_word"));
                ps.setString(4, (rs.getString("transcr") == null ? null : rs.getString("transcr")));
                ps.setDouble(5, rs.getDouble("prob_factor"));
                ps.setTimestamp(6, rs.getTimestamp("create_date"));
                ps.setString(7, (rs.getString("label") == null ? null : rs.getString("label")));
                ps.setTimestamp(8, (rs.getTimestamp("last_accs_date") == null ? null : rs.getTimestamp("last_accs_date")));
                ps.setDouble(9, rs.getDouble("index_start"));
                ps.setDouble(10, rs.getDouble("index_end"));
                ps.setBoolean(11, rs.getBoolean("exactmatch"));
                ps.execute();
            }
        }catch (SQLException e){
            System.out.println("EXCEPTION#2: in copyDb() from DAO id is " + id);
            e.printStackTrace();
            throw new RuntimeException();
        }

        try {
            rs = inMemSt.executeQuery("SELECT * FROM " + user);
            int counter = 0;
            while (rs.next()){
//                System.out.println("--- id from in memory DB " + rs.getInt("id"));
                counter++;
            }
            System.out.println("--- " + counter + " elements was added into in memory DB");
        } catch (SQLException e) {
            System.out.println("EXCEPTION#3: in copyDb() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        reloadIndices(1);
    }

    private Connection getDBConnection() {
        System.out.println("CALL: getDBConnection() from DAO");
        Connection dbConnection = null;
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
//            return dbConnection;
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
        try {
            Statement inMemDbPrepStat = inMemDbConn.createStatement();
//            System.out.println("--- updateProb() SQL UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='"+timestamp+"' WHERE id="+phrase.id);
            inMemDbPrepStat.executeUpdate("UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime + "' WHERE id=" + phrase.id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        new Thread(){
            public void run(){
                try {
                    Statement st = mainDbConn.createStatement();
                    st.execute("UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='" + dateTime + "' WHERE id=" + phrase.id);
                } catch (SQLException e) {
                    System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }.run();
        return reloadIndices(phrase.id);
    }

    public void updatePhrase(Phrase phrase){
        System.out.println("CALL: updatePhrase(Phrase phrase) from DAO with id=" + phrase.id);
        String dateTime = ZonedDateTime.now(ZoneId.of("Europe/Kiev")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        try {
            PreparedStatement inMemDbPrepStat = inMemDbConn.prepareStatement("UPDATE " + user + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                    "exactmatch=?, label=? WHERE id =" + phrase.id);
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

            inMemDbPrepStat.setString(4, dateTime);
            inMemDbPrepStat.setBoolean(5, phrase.exactMatch);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in updateProb(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        new Thread(){
            public void run(){
                try {
                    PreparedStatement mainDbPrepStat = mainDbConn.prepareStatement("UPDATE " + user + " SET for_word=?, nat_word=?, transcr=?, last_accs_date=?, " +
                            "exactmatch=?, label=? WHERE id =" + phrase.id);
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
                    mainDbPrepStat.execute();
                } catch (SQLException e) {
                    System.out.println("EXCEPTION#2: in updateProb(Phrase phrase) from DAO");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }.run();
    }

    public void deleteById(int id){
        System.out.println("CALL: deleteById(int id) from DAO");
        try {
            Statement st = inMemDbConn.createStatement();
            st.execute("DELETE FROM " + user + " WHERE ID=" + id);
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in deleteById(int id) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
        new Thread(){
            public void run(){
                try {
                    Statement st = mainDbConn.createStatement();
                    st.execute("DELETE FROM " + user + " WHERE ID=" + id);
                } catch (SQLException e) {
                    System.out.println("EXCEPTION#2: in deleteById(int id) from DAO");
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }.run();
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
        Statement st = null;
        ResultSet rs = null;
        String temp = null;
        try {
            st = inMemDbConn.createStatement();

            rs = st.executeQuery("SELECT DISTINCT (LABEL) FROM " + user + " ORDER BY LABEL");
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

    public long[] reloadIndices(int id){
        System.out.println("CALL: reloadIndices() from DAO");
        long start = System.currentTimeMillis();
        double temp = 0;

        double indOfLW;     //Индекс выпадения изученных
        double rangeOfNLW;  //Диапазон индексов неизученных слов
        double scaleOf1prob;    //rangeOfNLW/summProbOfNLW  цена одного prob
        ArrayList<Integer> idArr = new ArrayList<>();
        ResultSet rs = null;
        Statement statement = null;
        int summProbOfNLW = 0;
        long[] indexes = new long[2];

        //Заполняем idArr айдишниками
        try {
            statement = inMemDbConn.createStatement();
            rs = statement.executeQuery("SELECT id FROM " + table);
            while(rs.next())
                idArr.add(rs.getInt("ID"));

            rs = statement.executeQuery("SELECT COUNT(prob_factor) FROM " + table + " WHERE prob_factor>3");
            rs.next();
            nonLearnedWords = rs.getInt(1);
//            System.out.println("nonLearnedWords: " + nonLearnedWords);

            rs = statement.executeQuery("SELECT SUM(prob_factor) FROM " + table + " WHERE prob_factor>3");
            rs.next();
            summProbOfNLW = rs.getInt(1);
//            System.out.println("summProbOfNLW: " + summProbOfNLW);

            rs = statement.executeQuery("SELECT COUNT(prob_factor) FROM " + table + " WHERE prob_factor<=3");
            rs.next();
            learnedWords = rs.getInt(1);

            statement.execute("UPDATE " + user + " SET index_start = NULL ");
            statement.execute("UPDATE " + user + " SET index_end = NULL ");
        } catch (SQLException e) {
            System.out.println("EXCEPTION#1: in reloadIndices() from DAO");
            e.printStackTrace();
            throw new RuntimeException();
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
        try {
            for (int i : idArr) { //Устанавилвает индексы для неизученных слов
                long indexStart = 0;
                long indexEnd = 0;
//                System.out.println("--- for(" + i + ": idArr)");

                //Переменной prob присваивается prob фразы с currentPhraseId = i;
                rs = statement.executeQuery("SELECT prob_factor FROM " + user + " WHERE id=" + i);
                float prob;
                rs.next();
                prob = rs.getFloat(1);
                //            System.out.println("prob=" + prob);


                //Если nonLearnedWords == 0, то есть, все слова выучены устанавливаются равные для всех индексы
                if (nonLearnedWords == 0) {
                    indexStart = Math.round(temp * 1000000000);
                    statement.execute("UPDATE " + user + " SET index_start=" + indexStart + " WHERE id=" + i);
                    temp += chanceOfLearnedWords / learnedWords;
                    indexEnd = Math.round((temp * 1000000000) - 1);
                    statement.execute("UPDATE " + user + " SET index_end=" + indexEnd + " WHERE id=" + i);
                } else { //Если нет, то индексы ставяться по алгоритму
                    if (prob > 3) {
                        //                    System.out.println("UPDATE ALEKS SET INDEX1=" + Math.round(temp*1000000000) + " WHERE ID=" + i);
                        indexStart = Math.round(temp * 1000000000);
                        statement.execute("UPDATE " + user + " SET index_start=" + indexStart + " WHERE id=" + i);
//                        double i1 = temp;
                        temp += scaleOf1prob * prob;
                        //                    System.out.println("UPDATE ALEKS SET INDEX2=" + Math.round((temp *1000000000)-1) + " WHERE ID=" + i);
                        //                    System.out.println("%=" + (temp - MINFLOAT-i1));
                        indexEnd = Math.round((temp * 1000000000) - 1);
                        statement.execute("UPDATE " + user + " SET index_end=" + indexEnd + " WHERE id=" + i);
                    } else {
                        //                    System.out.println("Index1LW для ID=" + i + "=" + temp);
                        indexStart = Math.round(temp * 1000000000);
                        statement.execute("UPDATE " + user + " SET index_start=" + indexStart + " WHERE id=" + i);
                        temp += indOfLW;
                        //                    System.out.println("temp "+temp + "= temp "+temp+"+indOfLW "+indOfLW);
                        //                    System.out.println("Index2LW для ID=" + i + "=" + (temp - 1));
                        indexEnd = Math.round((temp * 1000000000) - 1);
                        statement.execute("UPDATE " + user + " SET index_end=" + indexEnd + " WHERE id=" + i);
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
        }
//        System.out.println("Изменено индексов для "+countOfModIndices+" позиций");
//        System.out.println("Time of performing reloadIndices method is " + (System.currentTimeMillis()-start) + "ms");
        return indexes;
    }

    public Phrase createRandPhrase(){
        System.out.println("CALL: createRandPhrase() from DAO");

        int id = random.nextInt(1000000000);
        ResultSet rs;
        Phrase phrase = null;
        Statement st = null;
        String sql = null;

        try {
            st = inMemDbConn.createStatement();
            sql = "SELECT * FROM " + table + " WHERE index_start<=" + id + " AND index_end>=" + id;
//            System.out.println(sql);
            rs = st.executeQuery(sql);
            rs.next();
            phrase = new Phrase(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getDouble("prob_factor"),
                    rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                    rs.getDouble("index_start"), rs.getDouble("index_end"), rs.getBoolean("exactmatch"), this);
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in createRandPhrase() from DAO SQL was " + sql);
            e.printStackTrace();
            throw new RuntimeException();
        }
        return phrase;
    }

    public void insertPhrase(Phrase phrase){
        System.out.println("CALL: insertPhrase(Phrase phrase) from DAO");
        try {
            PreparedStatement ps = inMemDbConn.prepareStatement("INSERT INTO " + table + " (id, for_word, nat_word, transcr, prob_factor, create_date," +
                    " label, last_accs_date, index_start, index_end, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
//            ps.setInt();
        } catch (SQLException e) {
            System.out.println("EXCEPTION: in insertPhrase(Phrase phrase) from DAO");
            e.printStackTrace();
            throw new RuntimeException();
        }
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
