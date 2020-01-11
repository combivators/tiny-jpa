package net.tiny.dao.reference;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pkey")
public class PrimaryKey {
	@XmlAttribute(name = "name")
	private String pkName;

	@XmlAttribute(name = "column")
	private String columnName;

	@XmlAttribute(name = "keySeq")
	private int keySeq;

	public String getName() {
		return getPkName();
	}

	public void setName(String name) {
		setPkName(name);
	}

	public String getPkName() {
		return pkName;
	}

	public void setPkName(String pkName) {
		this.pkName = pkName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public int getKeySeq() {
		return keySeq;
	}

	public void setKeySeq(int keySeq) {
		this.keySeq = keySeq;
	}
}
