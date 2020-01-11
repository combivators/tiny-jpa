package net.tiny.dao.test.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.tiny.dao.entity.BaseEntity;

@Entity
@Table(name = "xx_id1")
public class Id1 extends BaseEntity {
    @Transient
    private static final long serialVersionUID = 1L;

    @GeneratedValue(strategy=GenerationType.AUTO)
    @Id
    @Column(name = "id")
    private Long id;

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
}
