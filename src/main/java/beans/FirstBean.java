package beans;

import java.io.Serializable;
import java.sql.*;
import java.util.Random;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class FirstBean implements Serializable{
    @ManagedProperty(value="#{loginBean}")
    private LoginBean logBean;
    private boolean flag = false;


    private String outcomeForWord = "";
    private String outcomeNatWord = "";
    private String outcomeTransc = "";
    private String resultOutcomeText = "";
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

    public String getOutcomeForWord(){
        return outcomeForWord;
    }

    public void setOutcomeForWord(String name){
        this.outcomeForWord = name;
    }

    public String getOutcomeNatWord(){
        return outcomeNatWord;
    }

    public void setOutcomeNatWord(String name){
        this.outcomeNatWord = name;
    }

    public String getOutcomeTransc(){
        return outcomeTransc;
    }

    public void setOutcomeTransc(String name){
        this.outcomeTransc = name;
    }

    public String getResultOutcomeText(){
        return resultOutcomeText;
    }

    public void setResultOutcomeText(String dispText){
        this.resultOutcomeText = dispText;
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
            resultOutcomeText = rs1.getString("for_word")+" - " + rs1.getString("nat_word");
        }catch (SQLException e){
            Connection conn = DriverManager.getConnection(host2, "adminLtuHq9R", "d-AUIKakd1Br");
            st = conn.createStatement();
            Random rand = new Random();
            int random = rand.nextInt(999991183);
//            System.out.println(random);
            ResultSet rs1 = st.executeQuery("SELECT * FROM aleks " + "WHERE index_start<=" + random + " AND index_end>=" + random);
            rs1.next();

            outcomeForWord = rs1.getString("for_word");
            outcomeNatWord = rs1.getString("nat_word");
            outcomeTransc = rs1.getString("transcr");
            if(flag)
                resultOutcomeText = outcomeNatWord+" - "+ outcomeForWord + " - " + outcomeTransc + "</br>"+resultOutcomeText;
            flag = true;

        }
//        System.out.println("-------- output text is: " + outcomeText);
    }



}
