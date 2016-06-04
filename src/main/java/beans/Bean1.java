package beans;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;

/**
 * Created by Aleks on 04.06.2016.
 */
@ManagedBean
@RequestScoped
public class Bean1 {
    private String input;
    public Bean1(){

    }
    public String submit(){
        System.out.println(input);
        return input;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
