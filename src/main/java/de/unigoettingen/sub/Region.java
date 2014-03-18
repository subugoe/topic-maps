package de.unigoettingen.sub;

import de.unigoettingen.sub.medas.model.Doc;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by jdo on 3/18/14.
 */
public class Region {

    private int documentNumber;
    private String name;
    private List<Region> children = new LinkedList<>();
    private Point position;
    private Map<Doc, Point> documents = new HashMap<>();

    public Region(int number, String name) {
        documentNumber = number;
        this.name = name;
    }


    public int getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(int documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Region> getChildren() {
        return children;
    }

    public void setChildren(List<Region> childs) {
        this.children = childs;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public Map<Doc, Point> getDocuments() {
        return documents;
    }

    public void setDocuments(Map<Doc, Point> documents) {
        this.documents = documents;
    }

    public void addChild(Region child) {
        if (this.children == null) {
            children = new LinkedList<Region>();
        }
        children.add(child);
    }


}


