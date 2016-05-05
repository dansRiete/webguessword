package beans;

import java.io.Serializable;
import java.sql.*;
import java.util.Random;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class FirstBean implements Serializable{
    private String name = "Default name1";
    private String text = "Default text";
    static String host1 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    static String host2 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    Random random = new Random();

    public FirstBean(){}

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getText(){
        return text;
    }

    public void setText(String dispText){
        this.text = dispText;
    }

    public void refresh(){
        text = new Integer(random.nextInt(999999)).toString();
    }

    public String returnFromBase() throws SQLException{
//        Class.forName("com.mysql.cj.jdbc.Driver");
        try{
            Connection conn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM aleks");
            rs.next();
            return rs.getString("engname")+" - " + rs.getString("rusname");
        }catch (SQLException e){
            Connection conn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM aleks");
            rs.next();
            return rs.getString("engname")+" - " + rs.getString("rusname");
        }

    }

}
