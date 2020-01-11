package net.tiny.dao.entity;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.groups.Default;

/**
 * Entity - 基类
 *
 */
@EntityListeners(EntityListener.class)
@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    @Transient
    private static final long serialVersionUID = 1L;

    /** "ID"属性名称 */
    public static final String ID_PROPERTY_NAME = "id";

    /** "创建日期"属性名称 */
    public static final String CREATE_DATE_PROPERTY_NAME = "createDate";

    /** "修改日期"属性名称 */
    public static final String MODIFY_DATE_PROPERTY_NAME = "modifyDate";

    public static Calendar MAX_DATE = Calendar.getInstance();
    static {
        //9999-12-31 23:59:59
        MAX_DATE.set(9999, 11,  31, 23, 59, 59);
    }

    /**
     * 保存验证组
     */
    public interface Save extends Default {}

    /**
     * 更新验证组
     */
    public interface Update extends Default {}

    /** ID */
    //@DocumentId

    // MySQL/SQLServer: @GeneratedValue(strategy = GenerationType.AUTO)
    // Oracle: @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequenceGenerator")
    // H2 @GeneratedValue(strategy = GenerationType.AUTO)
    //Eclipselink @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequenceGenerator")
    //Derby @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequenceGenerator")
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    //@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    //@GeneratedValue(strategy = GenerationType.AUTO)
    //@GeneratedValue(strategy = GenerationType.TABLE, generator = "id_generator")
    //@TableGenerator(name = "id_generator", allocationSize = 100)
    //@GeneratedValue(strategy = GenerationType.AUTO, generator = "sequenceGenerator")
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	@XmlID
//	@Basic(optional = false)
//	@Id
//	@Column(name = "id")
//	protected long id;

    /** 创建日期 */
    @Column(name="create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    /** 修改日期 */
    @Column(name="modify_date", nullable = false)
    private LocalDateTime modifyDate;

    /**
     * 获取ID
     *
     * @return ID
     */
    public abstract Long getId();

    /**
     * 设置ID
     *
     * @param id
     *            ID
     */
    public abstract void setId(Long id);

    /**
     * 获取创建日期
     *
     * @return 创建日期
     */
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    /**
     * 设置创建日期
     *
     * @param createDate
     *            创建日期
     */
    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    /**
     * 获取修改日期
     *
     * @return 修改日期
     */
    public LocalDateTime getModifyDate() {
        return modifyDate;
    }

    /**
     * 设置修改日期
     *
     * @param modifyDate
     *            修改日期
     */
    public void setModifyDate(LocalDateTime modifyDate) {
        this.modifyDate = modifyDate;
    }

    /**
     * 生成ETag码(UUID)
     * id(Long) + modifyTime(Long) + class name
     */
    public String createEntityTag() {
        String className = getClass().getName();
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES*2 + className.length());
        buffer.putLong(getId() == null ? 0L : getId())
            .putLong(getModifyDate().getLong(ChronoField.MICRO_OF_SECOND))
            .put(className.getBytes());
        byte[] bytes = buffer.array();
        return UUID.nameUUIDFromBytes(bytes).toString();
    }

    /**
     * 变更时自动设置修改日期
     *
     */
    @PrePersist @PreUpdate
    private void now(){
        setModifyDate(LocalDateTime.now());
    }

    /**
     * 重写equals方法
     *
     * @param obj
     *            对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!BaseEntity.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        BaseEntity other = (BaseEntity) obj;
        return getId() != null ? getId().equals(other.getId()) : false;
    }

    /**
     * 重写hashCode方法
     *
     * @return hashCode
     */
    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode += null == getId() ? 0 : getId().hashCode() * 31;
        return hashCode;
    }

    @Override
    public String toString() {
        Table table = getClass().getAnnotation(Table.class);
        String tableName = getClass().getSimpleName();
        if(null != table) {
            tableName = table.name();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(tableName);
        if(null == getId()) {
            sb.append("@").append(hashCode());
        } else {
            sb.append("#").append(getId());
        }
        return sb.toString();
    }

}