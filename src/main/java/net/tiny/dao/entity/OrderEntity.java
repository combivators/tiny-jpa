package net.tiny.dao.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Entity - 排序基类
 *
 */
@MappedSuperclass
@XmlTransient
public abstract class OrderEntity extends BaseEntity implements Comparable<OrderEntity> {

	@Transient
	private static final long serialVersionUID = 1L;

	/** "排序"属性名称 */
	@Transient
	public static final String ORDER_PROPERTY_NAME = "order";

	/** 排序 */
	@Min(0)
	@Column(name = "orders")
	private Integer order;

	/**
	 * 获取排序
	 *
	 * @return 排序
	 */
	public Integer getOrder() {
		return order;
	}

	/**
	 * 设置排序
	 *
	 * @param order
	 *            排序
	 */
	public void setOrder(Integer order) {
		this.order = order;
	}

	/**
	 * 实现compareTo方法
	 *
	 * @param orderEntity
	 *            排序对象
	 * @return 比较结果
	 */
	@Override
	public int compareTo(OrderEntity orderEntity) {
		int ret = 0;
		if(null != getOrder() && null != orderEntity.getOrder()) {
			ret = getOrder().compareTo(orderEntity.getOrder());
			if(ret != 0) return ret;
		}
		return getId().compareTo(orderEntity.getId());
	}

}