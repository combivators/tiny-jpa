package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;


import net.tiny.dao.Reinjection;

public class ReinjectionTest {

    @Test
    public void testReinject() throws Exception {
        User user = new User();
        user.setName("Hoge");
        UserAttribute attribute = new UserAttribute();
        attribute.setAttribute("Beijing");
        user.setAttribute(attribute);

        //Before parent is null
        assertNull(attribute.getUser());

        Order order = new Order();
        order.setDetail("mobile");
        user.addOrder(order);
        assertNull(order.getUser());

        order = new Order();
        order.setDetail("office");
        user.addOrder(order);
        assertNull(order.getUser());


        Reinjection reinjection = new Reinjection();
        reinjection.reinject(User.class, user);
        assertNotNull(user.getAttribute().getUser());
        assertSame(user, user.getAttribute().getUser());

        //After parent was injected
        List<Order> orders = user.getOrders();
        for(Order o : orders) {
            assertNotNull(o.getUser());
            assertSame(user, o.getUser());
        }
    }

    //@Entity
    static class User {
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
        private String name;
        @JoinColumn @OneToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
        private UserAttribute attribute;
        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Order> orders = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public UserAttribute getAttribute() { return attribute; }
        public void setAttribute(UserAttribute attribute) { this.attribute = attribute; }
        public List<Order> getOrders() { return orders; }
        public void setOrders(List<Order> orders) { this.orders = orders; }
        public void addOrder(Order order) {
            if(!this.orders.contains(order)) {this.orders.add(order);}
        }
    }

    //@Entity
    static class UserAttribute {
        @Id
        private Long id;
        private String attribute;
        @OneToOne(cascade = CascadeType.ALL, mappedBy="attribute")
        private User user;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }

    //@Entity
    static class Order {
        @Id
        private Long id;
        private String detail;
        @JoinColumn @ManyToOne(fetch = FetchType.LAZY)
        private User user;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }
}
