package beans;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Created by Aleks on 23.04.2016.
 */

@ManagedBean
@SessionScoped
public class FirstBean implements Serializable{
    private String name = "Default name1";
    private String text = "Default text";
    static String host1 = "jdbc:mysql://"
            + System.getenv().get("OPENSHIFT_MYSQL_DB_HOST")
            + ":"
            + System.getenv().get("OPENSHIFT_MYSQL_DB_PORT")
            + "/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    static String host2 = "jdbc:mysql://${env.OPENSHIFT_MYSQL_DB_HOST}:${env.OPENSHIFT_MYSQL_DB_PORT}/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    static String host3 = "jdbc:mysql://127.3.47.130:3306/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
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

    public String returnFromBase() throws Exception{
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(host3, "adminLtuHq9R", "d-AUIKakd1Br");
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM aleks");
        rs.next();

        Context initContext = new InitialContext();
        Context envContext  = (Context)initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource)envContext.lookup("java:jboss/datasources/MySQLDS");
        Connection conn1 = ds.getConnection();

        return rs.getString("engname")+" - " + System.getenv("$OPENSHIFT_MYSQL_DB_HOST") + " - " + conn1;

        /*return "jdbc:mysql://"
                + System.getenv().get("$OPENSHIFT_MYSQL_DB_HOST")
                + ":"
                + System.getenv().get("$OPENSHIFT_MYSQL_DB_PORT")
                + "/guessword?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";*/

    }

}
