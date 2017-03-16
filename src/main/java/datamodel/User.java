package datamodel;

/**
 * Created by Aleks on 20.05.2016.
 */
public class User {
    public int id;
    public String login;
    public String name;
    public String password;
    public String email;
    public User(int id, String login, String name, String password, String email){
        this.id = id;
        this.login = login;
        this.name = name;
        this.password = password;
        this.email = email;
    }
}
