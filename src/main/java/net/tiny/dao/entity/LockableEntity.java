package net.tiny.dao.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlTransient;

@MappedSuperclass
@XmlTransient
public abstract class LockableEntity extends BaseEntity {

	@Transient
	private static final long serialVersionUID = 1L;

	/** 乐观排他 */
	@Version
	@Column(name="version", nullable = false)
    private long version;

	/**
	 * 获取排他版本号
	 *
	 * @return 排他版本号
	 */
	public long getVersion() {
		return version;
	}

	/**
	 * 设置排他版本号
	 *
	 * @param version
	 *            排他版本号
	 */
	public void setVersion(long version) {
		this.version = version;
	}
}
