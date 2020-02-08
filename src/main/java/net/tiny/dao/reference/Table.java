package net.tiny.dao.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "table")
public class Table {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "columns")
    private List<Column> columns = new ArrayList<Column>();

    public Table() {}

    public Table(String name, List<Column> columns) {
        setName(name);
        setColumns(columns);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public  List<Column> getColumns() {
        return this.columns;
    }

    public  void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public boolean hasColumn(String name) {
        for(Column column : this.columns) {
            if(name.equalsIgnoreCase(column.getColumnName())) {
                return true;
            }
        }
        return false;
    }

    public Optional<Column> getColumn(String name) {
        return columns.stream()
            .filter(c -> name.equalsIgnoreCase(c.getColumnName()))
            .findFirst();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" {");
        for(Column column : this.columns) {
            sb.append(column.getColumnName());
            sb.append(","); //TODO
        }
        sb.append("}");
        return sb.toString();
    }
}
