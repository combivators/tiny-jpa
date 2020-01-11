package net.tiny.dao.entity;

import java.time.LocalDateTime;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * Listener - 创建日期、修改日期处理
 *
 */
public class EntityListener {

	/**
	 * 保存前处理
	 *
	 * @param entity
	 *            基类
	 */
	@PrePersist
	public void prePersist(BaseEntity entity) {
		LocalDateTime now = LocalDateTime.now();
		entity.setCreateDate(now);
		entity.setModifyDate(now);
	}

	/**
	 * 更新前处理
	 *
	 * @param entity
	 *            基类
	 */
	@PreUpdate
	public void preUpdate(BaseEntity entity) {
		entity.setModifyDate(LocalDateTime.now());
	}

}