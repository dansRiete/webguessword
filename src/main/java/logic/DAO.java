package logic;

import java.sql.*;
import java.util.Random;

/**
 * Created by Aleks on 11.05.2016.
 */
public class DAO {
    Random random = new Random();
    static String host1 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static String host2 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    Connection conn;
    Statement st;
    {
        try{
            conn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            st = conn.createStatement();
        }catch (SQLException e){
            try{
                conn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
                st = conn.createStatement();
            }catch (SQLException e1){
                e1.printStackTrace();
            }

        }
    }
    public DAO(){}

    public Phrase nextPhrase(){
        int id = random.nextInt(999991183);
        ResultSet rs;
        Phrase phrase = null;
        try {
            rs = st.executeQuery("SELECT * FROM aleks " + "WHERE index_start<=" + random + " AND index_end>=" + random);
            rs.next();
            phrase = new Phrase(rs.getInt("id"), rs.getString("for_word"), rs.getString("nat_word"), rs.getString("transcr"), rs.getDouble("prob_factor"),
                    rs.getTimestamp("create_date"), rs.getString("label"), rs.getTimestamp("last_accs_date"),
                    rs.getDouble("index_start"), rs.getDouble("index_end"), rs.getBoolean("exactmatch"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return phrase;
    }

    public void insertPhrase(Phrase phrase){
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO aleks (id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, index_start, index_end, exactmatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
//            ps.setInt();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void backupDB(){
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        int count = 0;
        long start = System.currentTimeMillis();
        try {
            ResultSet rs = st.executeQuery("SELECT * FROM aleks");
            PreparedStatement ps = conn.prepareStatement("INSERT INTO resaleks " +
                    "(date, id, for_word, nat_word, transcr, prob_factor, create_date, label, last_accs_date, " +
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

}
