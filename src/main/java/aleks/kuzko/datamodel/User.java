package aleks.kuzko.datamodel;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Aleks on 20.05.2016.
 */

@Entity
@Table(name = "users")
public class User implements Serializable {

    /*@javax.persistence.Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public long id;*/

    public String getLogin() {
        return login;
    }

    @javax.persistence.Id
    public String login;

    @Column
    public String name;

    @Column
    public String password;

    @Column
    public String email;

    public User(){}

    public User(String login, String name, String password, String email){
        this.login = login;
        this.name = name;
        this.password = password;
        this.email = email;
    }

    /*public long getId() {
        return id;
    }*/
}
