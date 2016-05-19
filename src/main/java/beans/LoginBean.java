package beans;

import logic.DAO;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

@ManagedBean(name="login")
@SessionScoped
public class LoginBean/* implements Serializable */{
    private String user;
    private String password;
    private String text;
    private DAO dao;
    public LoginBean(){

    }
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


    public void checkUser(){
        //!!! EXC !!!

            if ((this.user.equals("aleks") || (this.user.equals("oksana"))) && (this.password.equals("vlenaf13") || this.password.equals("oks2804"))) {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                try {
                    request.getRequestDispatcher("learn.xhtml").forward(request, response);
                } catch (ServletException e) {
                    System.out.println("--- Servlet exception during request.getRequestDispatcher(\"learn.xhtml\").forward(request, response);");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("--- IOException during request.getRequestDispatcher(\"learn.xhtml\").forward(request, response);");
                    e.printStackTrace();
                }
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                try {
                    response.sendRedirect("error.xhtml");
                } catch (IOException e) {
                    System.out.println("--- IOException during response.sendRedirect(\"error.xhtml\");");
                    e.printStackTrace();
                }
            }

    }

    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text = text;
    }
    public DAO returnDAO(){
        dao = new DAO(this);
        System.out.println("DAO is " + dao);
        return this.dao;
    }
}
