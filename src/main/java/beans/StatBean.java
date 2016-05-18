package beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class StatBean implements Serializable{

    private static final long serialVersionUID = 1L;


    public static ArrayList orderList = new ArrayList<>();
    /**
     * Displays the number of responses per day
     */
    public static Float dayCounter = 0f;
    static {
        orderList.add(new Properties("Answers per day", ++dayCounter));
    }


    public List getOrderList() {
        return orderList;
    }

    public static class Order{

        String orderNo;
        String productName;
        BigDecimal price;
        int qty;

        public Order(String orderNo, String productName, BigDecimal price, int qty) {
            this.orderNo = orderNo;
            this.productName = productName;
            this.price = price;
            this.qty = qty;
        }

        public String getOrderNo() {
            return orderNo;
        }
        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }
        public String getProductName() {
            return productName;
        }
        public void setProductName(String productName) {
            this.productName = productName;
        }
        public BigDecimal getPrice() {
            return price;
        }
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        public int getQty() {
            return qty;
        }
        public void setQty(int qty) {
            this.qty = qty;
        }
    }

    public static class Properties{

        String propertyName;
        Float propertyValue;

        public Properties(String propertyName, Float propertyValue) {
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        public String getPropertyName() {
            return propertyName;
        }
        public void setPropertyName(String orderNo) {
            this.propertyName = propertyName;
        }
        public Float getPropertyValue() {
            return propertyValue;
        }
        public void setPropertyValue(String productName) {
            this.propertyValue = propertyValue;
        }
    }
}