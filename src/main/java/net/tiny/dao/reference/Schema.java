package net.tiny.dao.reference;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "schema")
public class Schema {
	
	@XmlAttribute(name = "name")
	private String name;
	
	@XmlAttribute(name = "tables")
	private List<Table> tables = new ArrayList<Table>();

	public Schema() {}

	public Schema(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public  List<Table> getTables() {
		return this.tables;
	}

	public  void setTables(List<Table> tables) {
		this.tables = tables;
	}
}
