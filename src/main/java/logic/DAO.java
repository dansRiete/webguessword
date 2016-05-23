package logic;

import Exceptions.DataBaseConnectionException;
import beans.LoginBean;

import java.sql.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Aleks on 11.05.2016.
 */
public class DAO {
    Random random = new Random();
    static String host1 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static String host2 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&characterEncoding=utf8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    Connection mainDbConn;
    Connection inMemDbConn;
    private int totalNumberOfWords;
    private int totalNumberOfLearnedWords;
    private LoginBean loginBean;
    public String table;
    String user;
    String password;
    public ArrayList<String> labels = new ArrayList<>();
    public double learnedWords;
    final double chanceOfLearnedWords = 1d/15d;

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
    private final String createTableSql = "CREATE TABLE " + "aleks" + "\n" +
            "(\n" +
            "    id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,\n" +
            "    for_word VARCHAR(250) NOT NULL,\n" +
            "    nat_word VARCHAR(250) NOT NULL,\n" +
            "    transcr VARCHAR(100),\n" +
            "    prob_factor DOUBLE,\n" +
            "    create_date TIMESTAMP,\n" +
            "    label VARCHAR(50),\n" +
            "    last_accs_date TIMESTAMP,\n" +
            "    exactmatch BOOLEAN,\n" +
            "    index_start DOUBLE,\n" +
            "    index_end DOUBLE\n" +
            ");\n" +
            "ALTER TABLE " + "aleks" + " ADD CONSTRAINT unique_id UNIQUE (id);";

    private void copyDb(){
        try {
            Statement inMemSt = inMemDbConn.createStatement();
            inMemSt.execute(createTableSql);
            Statement mainSt = mainDbConn.createStatement();
            ResultSet rs = mainSt.executeQuery("SELECT * FROM " + "aleks");
            PreparedStatement ps = inMemDbConn.prepareStatement("INSERT INTO aleks " +
                    "(id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
                    "index_start, index_end, exactmatch) VALUES (?,?,?,?,?,?,?,?,?,?,?)");

            while (rs.next()){
                ps.setInt(1, rs.getInt("id"));
                ps.setString(2, rs.getString("for_word"));
                ps.setString(3, rs.getString("nat_word"));
                ps.setString(4, (rs.getString("transcr")==null?null:rs.getString("transcr")));
                ps.setDouble(5, rs.getDouble("prob_factor"));
                ps.setTimestamp(6, rs.getTimestamp("create_date"));
                ps.setString(7, (rs.getString("label")==null?null:rs.getString("label")));
                ps.setTimestamp(8, (rs.getTimestamp("last_accs_date")==null?null:rs.getTimestamp("last_accs_date")));
                ps.setDouble(9, rs.getDouble("index_start"));
                ps.setDouble(10, rs.getDouble("index_end"));
                ps.setBoolean(11, rs.getBoolean("exactmatch"));
                ps.execute();
            }

            rs = inMemSt.executeQuery("SELECT * FROM " + "aleks");
            int counter = 0;
            while (rs.next()){
//                System.out.println("--- id from in memory DB " + rs.getInt("id"));
                counter++;
            }
            System.out.println("--- " + counter + " elements was added into in memory DB");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DAO(){
        try{
            mainDbConn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            System.out.println("--- Remote DB was connected");
        }catch (SQLException e){
            try{
                mainDbConn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
                System.out.println("--- Local DB was connected");
            }catch (SQLException e1){
                e1.printStackTrace();
                System.out.println("--- There was an error during connecting DB");
                throw new DataBaseConnectionException();
            }
        }
        inMemDbConn = getDBConnection();
        System.out.println("inMemDbConn is " + inMemDbConn);
        copyDb();
    }



    private static Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
//            return dbConnection;
        } catch (SQLException e) {
            System.out.println("--- Exception during getDBConnection()");
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }

    public void updateProb(Phrase phrase){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try {
            Statement ps = inMemDbConn.createStatement();
//            System.out.println("--- updateProb() SQL UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='"+timestamp+"' WHERE id="+phrase.id);
            ps.executeUpdate("UPDATE " + user + " SET prob_factor=" + phrase.prob + ", last_accs_date='"+timestamp+"' WHERE id="+phrase.id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLoginBean(LoginBean loginBean){
        user = loginBean.getUser();
        password = loginBean.getPassword();
        table = user;
        System.out.println("user is " + user);
        reloadLabelsList();

    }

    public Connection getConnection(){
        return mainDbConn;
    }

    public void reloadLabelsList(){
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
            e.printStackTrace();
        }
    }

    private void getStatistic(){
        Statement st = null;
        ResultSet rs = null;
        try {
            st = inMemDbConn.createStatement();
            rs = st.executeQuery("SELECT COUNT(*) FROM " + table + "  WHERE PROB<=3");
            rs.next();
            totalNumberOfLearnedWords = rs.getInt(1);
            rs = st.executeQuery("SELECT COUNT(*) FROM " +table);
            rs.next();
            totalNumberOfWords = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void reloadIndices() throws SQLException{
        long start = System.currentTimeMillis();
        double temp = 0;
        double nonLearnedWords = 0;
        double indOfLW;     //Индекс выпадения изученных
        double rangeOfNLW;  //Диапазон индексов неизученных слов
        double scaleOf1prob;    //rangeOfNLW/summProbOfNLW  цена одного prob
        ArrayList<Integer> idArr = new ArrayList<>();
        ResultSet rs = null;
        Statement statement = inMemDbConn.createStatement();
        int summProbOfNLW = 0;

        //Заполняем idArr айдишниками
        try {
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
            e.printStackTrace();
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
//                System.out.println("--- for(" + i + ": idArr)");

                //Переменной prob присваивается prob фразы с currentPhraseId = i;
                rs = statement.executeQuery("SELECT prob_factor FROM " + user + " WHERE id=" + i);
                float prob;
                rs.next();
                prob = rs.getFloat(1);
                //            System.out.println("prob=" + prob);


                //Если nonLearnedWords == 0, то есть, все слова выучены устанавливаются равные для всех индексы
                if (nonLearnedWords == 0) {
                    statement.execute("UPDATE " + user + " SET index_start=" + Math.round(temp * 1000000000) + " WHERE id=" + i);
                    temp += chanceOfLearnedWords / learnedWords;
                    statement.execute("UPDATE " + user + " SET index_end=" + Math.round((temp * 1000000000) - 1) + " WHERE id=" + i);
                } else { //Если нет, то индексы ставяться по алгоритму
                    if (prob > 3) {
                        //                    System.out.println("UPDATE ALEKS SET INDEX1=" + Math.round(temp*1000000000) + " WHERE ID=" + i);
                        statement.execute("UPDATE " + user + " SET index_start=" + Math.round(temp * 1000000000) + " WHERE id=" + i);
                        double i1 = temp;
                        temp += scaleOf1prob * prob;
                        //                    System.out.println("UPDATE ALEKS SET INDEX2=" + Math.round((temp *1000000000)-1) + " WHERE ID=" + i);
                        //                    System.out.println("%=" + (temp - MINFLOAT-i1));
                        statement.execute("UPDATE " + user + " SET index_end=" + Math.round((temp * 1000000000) - 1) + " WHERE id=" + i);
                    } else {
                        //                    System.out.println("Index1LW для ID=" + i + "=" + temp);
                        statement.execute("UPDATE " + user + " SET index_start=" + Math.round(temp * 1000000000) + " WHERE id=" + i);
                        temp += indOfLW;
                        //                    System.out.println("temp "+temp + "= temp "+temp+"+indOfLW "+indOfLW);
                        //                    System.out.println("Index2LW для ID=" + i + "=" + (temp - 1));
                        statement.execute("UPDATE " + user + " SET index_end=" + Math.round((temp * 1000000000) - 1) + " WHERE id=" + i);
                    }
                }
                countOfModIndices++;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
//        System.out.println("Изменено индексов для "+countOfModIndices+" позиций");
        System.out.println("Time of performing reloadIndices method is " + (System.currentTimeMillis()-start) + "ms");
    }

    public Phrase nextPhrase(){
        int id = random.nextInt(999991183);
        ResultSet rs;
        Phrase phrase = null;
        Statement st = null;

        try {
            st = inMemDbConn.createStatement();
            String sql = "SELECT * FROM " + table + " WHERE index_start<=" + id + " AND index_end>=" + id;
//            System.out.println(sql);
            rs = st.executeQuery(sql);
            rs.next();
            phrase = new Phrase(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getDouble("prob_factor"),
                    rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                    rs.getDouble("index_start"), rs.getDouble("index_end"), rs.getBoolean("exactmatch"), this);
        } catch (SQLException e) {
            System.out.println("Exception during nextPhrase()");
            e.printStackTrace();
        }
        return phrase;
    }

    public void insertPhrase(Phrase phrase){
        try {
            PreparedStatement ps = inMemDbConn.prepareStatement("INSERT INTO " + table + " (id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, index_start, index_end, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
//            ps.setInt();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void backupDB(){
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
                ps.setTimestamp(9, (rs.getTimestamp("last_accs_date")==null?null:rs.getTimestamp("last_accs_date")));
                ps.setDouble(10, rs.getDouble("index_start"));
                ps.setDouble(11, rs.getDouble("index_end"));
                ps.setBoolean(12, rs.getBoolean("exactmatch"));
                ps.execute();
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis()-start;
        System.out.println("Copied " + count + " elements, total time=" + end + " ms");
    }

    //Getters setters
    public int getTotalNumberOfWords(){
        return totalNumberOfWords;
    }
    public int getTotalNumberOfLearnedWords(){
        return totalNumberOfLearnedWords;
    }

}
