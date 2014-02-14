package de.unigoettingen.sub.model;

import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author doenitz@sub.uni-goettingen.de
 *
 */
@XmlType
@XmlRootElement(name = "doc")
public class Doc {

    private String docid;
    private Id id;
    private String title;
    private String titleShort;
    private String mets;
    private String preview;
    private String tei;
    private String teiEnriched;
    private String pageCount;
    private String fulltext;
    private Set<RelatedItem> relatedItems;
    private Set<Classification> classifications;

    public String getDocid() {
        return docid;
    }

    public void setDocid(String docid) {
        this.docid = docid;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleShort() {
        return titleShort;
    }

    public void setTitleShort(String titleShort) {
        this.titleShort = titleShort;
    }

    public String getMets() {
        return mets;
    }

    public void setMets(String mets) {
        this.mets = mets;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getTei() {
        return tei;
    }

    public void setTei(String tei) {
        this.tei = tei;
    }

    public String getTeiEnriched() {
        return teiEnriched;
    }

    public void setTeiEnriched(String teiEnriched) {
        this.teiEnriched = teiEnriched;
    }

    public String getPageCount() {
        return pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount;
    }

    public String getFulltext() {
        return fulltext;
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

//    @XmlElementWrapper(name = "relatedItems")
    @XmlElement(name = "relatedItems")
    public Set<RelatedItem> getRelatedItems() {
        return relatedItems;
    }

    public void setRelatedItems(Set<RelatedItem> relatedItems) {
        this.relatedItems = relatedItems;
    }

//    @XmlElementWrapper(name = "classifications")
    @XmlElement(name = "classifications")
    public Set<Classification> getClassifications() {
        return classifications;
    }

    public void setClassifications(Set<Classification> classifications) {
        this.classifications = classifications;
    }
}
