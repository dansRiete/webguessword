package logic;

import Exceptions.DataBaseConnectionException;
import beans.LoginBean;

import javax.faces.bean.ManagedProperty;
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
    Connection conn;
    private int totalNumberOfWords;
    private int totalNumberOfLearnedWords;
    private LoginBean loginBean;
    public String table;
    String user;
    String password;
    public ArrayList<String> labels = new ArrayList<>();

    public void setLoginBean(LoginBean loginBean){
        user = loginBean.getUser();
        password = loginBean.getPassword();
        table = user;
        System.out.println("user is " + user);
        reloadLabelsList();

    }
    public Connection getConnection(){
        return conn;
    }
    public DAO(){
        try{
            conn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            System.out.println("--- Remote DB was connected");
        }catch (SQLException e){
            try{
                conn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
                System.out.println("--- Local DB was connected");
            }catch (SQLException e1){
                e1.printStackTrace();
                System.out.println("--- There was an error during connecting DB");
                throw new DataBaseConnectionException();
            }

        }
    }

    public void reloadLabelsList(){
        labels.clear();
        labels.add("All");
        Statement st = null;
        ResultSet rs = null;
        String temp = null;
        try {
            st = conn.createStatement();

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
            st = conn.createStatement();
            rs = st.executeQuery("SELECT COUNT(*) FROM " +table + "  WHERE PROB<=3");
            rs.next();
            totalNumberOfLearnedWords = rs.getInt(1);
            rs = st.executeQuery("SELECT COUNT(*) FROM " +table);
            rs.next();
            totalNumberOfWords = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }


    public Phrase nextPhrase(){
        int id = random.nextInt(999991183);
        ResultSet rs;
        Phrase phrase = null;
        Statement st = null;

        try {
            st = conn.createStatement();
            String sql = "SELECT * FROM " + table + " WHERE index_start<=" + id + " AND index_end>=" + id;
            System.out.println(sql);
            rs = st.executeQuery(sql);
            rs.next();
            phrase = new Phrase(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getDouble("prob_factor"),
                    rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                    rs.getDouble("index_start"), rs.getDouble("index_end"), rs.getBoolean("exactmatch"));
        } catch (SQLException e) {
            System.out.println("Exception during nextPhrase()");
            e.printStackTrace();
        }
        return phrase;
    }
    public void insertPhrase(Phrase phrase){
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table + " (id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, index_start, index_end, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
            st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + table);
            PreparedStatement ps = conn.prepareStatement("INSERT INTO res" + table +
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
    public int getTotalNumberOfWords(){
        return totalNumberOfWords;
    }
    public int getTotalNumberOfLearnedWords(){
        return totalNumberOfLearnedWords;
    }

}
