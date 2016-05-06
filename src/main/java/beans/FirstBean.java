package beans;

import java.io.Serializable;
import java.sql.*;
import java.util.Random;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class FirstBean implements Serializable{
    @ManagedProperty(value="#{loginBean}")
    private LoginBean logBean;


    private String name = "Default name";
    private String outputText = "def text";
    static String host1 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static String host2 = "jdbc:mysql://127.0.0.1:3307/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static Statement st;
    static {
        try{
            Connection conn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            st = conn.createStatement();
        }catch (SQLException e){
            try {
                Connection conn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
                st = conn.createStatement();
            }catch (SQLException e1){
                e1.printStackTrace();
            }
        }

    }
    Random random = new Random();

    public LoginBean getLogBean(){
        return logBean;
    }
    public void setLogBean(LoginBean logBean){
        this.logBean = logBean;
    }
    public FirstBean(){
        System.out.println("-------- Bean was created");
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getOutputText(){
        return outputText;
    }

    public void setOutputText(String dispText){
        this.outputText = dispText;
    }

    public void refresh() throws SQLException{
        System.out.println("-------- refresh() was called");
        System.out.println("-------- Password=" + logBean.getPassword());

        try{
            Connection conn = DriverManager.getConnection(host1, "adminLtuHq9R", "d-AUIKakd1Br");
            st = conn.createStatement();
            Random rand = new Random();
            int random = rand.nextInt(999991183);
//            System.out.println(random);
            ResultSet rs1 = st.executeQuery("SELECT * FROM aleks " + "WHERE index_start<=" + random + " AND index_end>=" + random);
            rs1.next();
            outputText = rs1.getString("for_word")+" - " + rs1.getString("nat_word");
        }catch (SQLException e){
            Connection conn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
            st = conn.createStatement();
            Random rand = new Random();
            int random = rand.nextInt(999991183);
//            System.out.println(random);
            ResultSet rs1 = st.executeQuery("SELECT * FROM aleks " + "WHERE index_start<=" + random + " AND index_end>=" + random);
            rs1.next();
            outputText = rs1.getString("for_word")+" - " + rs1.getString("nat_word");
        }
        System.out.println("-------- output text is: " + outputText);
    }



}
