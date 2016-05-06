package beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

@ManagedBean
@SessionScoped

public class LoginBean implements Serializable {
    private String user;
    private String password;
    private String text;
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public void checkUser() throws IOException, ServletException{
        if(this.user.equals("aleks") && this.password.equals("vlenaf13")){
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();
            HttpServletResponse response = (HttpServletResponse)context.getExternalContext().getResponse();
            request.getRequestDispatcher("learn.xhtml").forward(request,response);
        } else{
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse)context.getExternalContext().getResponse();
            response.sendRedirect("error.xhtml");
        }
    }


    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text = text;
    }
}
