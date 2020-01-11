package net.tiny.dao.reference;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "column")
public class Column {

    @XmlAttribute(name = "name")
    private String columnName;

    @XmlAttribute(name = "dataType")
    private int dataType;

    @XmlAttribute(name = "typeName")
    private String typeName;

    @XmlAttribute(name = "columnSize")
    private int columnSize;

    @XmlAttribute(name = "decimalDigits")
    private int deciialDigits;

    @XmlAttribute(name = "numPrecRadix")
    private int numPrecRadix;

    @XmlAttribute(name = "nullable")
    private int nullable;

    @XmlAttribute(name = "pkey")
    private PrimaryKey pkey = null;

    @XmlAttribute(name = "index")
    private Index index = null;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    public int getDeciialDigits() {
        return deciialDigits;
    }

    public void setDeciialDigits(int deciialDigits) {
        this.deciialDigits = deciialDigits;
    }

    public int getNumPrecRadix() {
        return numPrecRadix;
    }

    public void setNumPrecRadix(int numPrecRadix) {
        this.numPrecRadix = numPrecRadix;
    }

    public int getNullable() {
        return nullable;
    }

    public void setNullable(int nullable) {
        this.nullable = nullable;
    }

    public boolean isPrimaryKey() {
        return (null != pkey);
    }
    public PrimaryKey getPrimaryKey() {
        return pkey;
    }

    public void setPrimaryKey(PrimaryKey pkey) {
        this.pkey = pkey;
    }

    public boolean isIndex() {
        return (null != index);
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }
}
