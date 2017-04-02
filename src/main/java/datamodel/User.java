package datamodel;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Aleks on 20.05.2016.
 */

@Entity
@Table(name = "users")
public class User implements Serializable {

    @javax.persistence.Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public long id;

    @Column
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

    public long getId() {
        return id;
    }
}
