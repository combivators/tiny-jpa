package net.tiny.dao.test.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import net.tiny.dao.entity.BaseEntity;

/**
 * Entity - 日志
 *
 */
@XmlRootElement
@Entity
@Table(name = "xx_log")
public class Log extends BaseEntity {

    @Transient
    private static final long serialVersionUID = 1L;

    /** "日志内容"属性名称 */
    public static final String LOG_CONTENT_ATTRIBUTE_NAME = Log.class.getName() + ".CONTENT";

    /** ID */
    @XmlID
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logSequenceGenerator")
    @SequenceGenerator(name = "logSequenceGenerator", sequenceName = "xx_log_sequence", allocationSize=1)
    @Id
    @Column(name = "id")
    private Long id;

    /** 操作 */
    @Column(nullable = false, updatable = false)
    private String operation;

    /** 操作员 */
    @Column(updatable = false)
    private String operator;

    /** 内容 */
    @Column(length = 3000, updatable = false)
    private String content;

    /** 请求参数 */
    @Lob
    @Column(updatable = false)
    private String parameter;

    /** IP */
    @Column(nullable = false, updatable = false)
    private String ip;

    /**
     * 获取ID
     *
     * @return ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置ID
     *
     * @param id
     *            ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取操作
     *
     * @return 操作
     */
    public String getOperation() {
        return operation;
    }

    /**
     * 设置操作
     *
     * @param operation
     *            操作
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * 获取操作员
     *
     * @return 操作员
     */
    public String getOperator() {
        return operator;
    }

    /**
     * 设置操作员
     *
     * @param operator
     *            操作员
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * 获取内容
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置内容
     *
     * @param content
     *            内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取请求参数
     *
     * @return 请求参数
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * 设置请求参数
     *
     * @param parameter
     *            请求参数
     */
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    /**
     * 获取IP
     *
     * @return IP
     */
    public String getIp() {
        return ip;
    }

    /**
     * 设置IP
     *
     * @param ip
     *            IP
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

}
