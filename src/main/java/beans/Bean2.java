package beans;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Created by Aleks on 04.06.2016.
 */
@ManagedBean
@RequestScoped
public class Bean2 {
    private String input;
    public Bean2(){

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
