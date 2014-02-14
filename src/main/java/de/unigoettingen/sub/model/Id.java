package de.unigoettingen.sub.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author jdo
 */
@XmlType
@XmlRootElement(name="id")
public class Id {
    private boolean isRecordIdentifier;
    private String type;
    private String value;
    private String source;

    public boolean isIsRecordIdentifier() {
        return isRecordIdentifier;
    }

    public void setIsRecordIdentifier(boolean isRecordIdentifier) {
        this.isRecordIdentifier = isRecordIdentifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    
}
