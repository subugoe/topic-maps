package de.ungoettingen.sub;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class TopicMapCreator {

    private int circleSize = 20;

    public static void main(String[] args) {

        TopicMapCreator app = new TopicMapCreator();
        app.run();
//        app.calculatePositions(30);
    }
    private Map<String, List<String>> ddcMap;
    private Map<String, String> hostMap;

    private void run() {
//        Region world = buildTree();

        long start = System.currentTimeMillis();
        Region world = buildTreeFromMets();
        layout(world);
    }

    private void readMETs() throws SAXException, FileNotFoundException, IOException {
        File dir = new File("/home/jdo/digizeit/METS/mets_repository/indexed_mets/");
        File[] mets = dir.listFiles();
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        MetsContentHandler metsContentHandler = new MetsContentHandler();
        for (int i = 0; i < mets.length; i++) {
//            System.out.println(mets[i]);
            InputSource inputSource = new InputSource(new FileReader(mets[i]));
            xmlReader.setContentHandler(metsContentHandler);
            xmlReader.parse(inputSource);
        }
        this.ddcMap = metsContentHandler.getDdcMap();
        this.hostMap = metsContentHandler.getHostMap();
    }

    private void calculatePositions(int radius, List<Region> items) {
        int i = 0;
        double angle = 2 * Math.PI / items.size() ;
        for (Region item : items) {            
            int x = (int) (Math.cos(i * angle) * radius);
            int y = (int) (Math.sin(i * angle) * radius);
            i++;
//            System.out.println(item.getName() + " " + x + ", " + y);
            item.setPosition(new Point(x, y));
        }

    }

    private int cirlesPerRadius(int radius, int circleSize) {
        double circumference = 2 * Math.PI * radius;
        int maxcicles = (int) Math.floor(circumference / circleSize);
        return maxcicles;
    }

    private Region buildTree() {
        Region world = new Region(0, "All");
        Region r0 = new Region(0, "Informatik, Informationswissenschaft, allgemeine Werke");
        r0.addChild(new Region(8, "Libraryship"));
        world.addChild(r0);
        Region r1 = new Region(15, "Philosophy");
        world.addChild(r1);
        Region r2 = new Region(15, "Religion");
        world.addChild(r2);
        Region r3 = new Region(10, "Sociology");
        r3.addChild(new Region(33, "economics"));
        r3.addChild(new Region(27, "Economics"));
        r3.addChild(new Region(12, "Law"));
        r3.addChild(new Region(36, "Education"));
        world.addChild(r3);
        Region r4 = new Region(113, "Philology");
        r4.addChild(new Region(1, "Englisch Languages"));
        r4.addChild(new Region(80, "Germanic Languages"));
        r4.addChild(new Region(48, "Romance Languages"));
        world.addChild(r4);
        Region r5 = new Region(16, "Science");
        r5.addChild(new Region(243, "Mathematics"));
        r5.addChild(new Region(2, "Geology"));
        world.addChild(r5);
        Region r7 = new Region(145, "Arts");
        r7.addChild(new Region(30, "Musicology"));
        world.addChild(r7);
        Region r9 = new Region(84, "History");
        r9.addChild(new Region(5, "Oriental Studies"));
        world.addChild(r9);

        return world;
    }

    private Region buildTreeFromMets() {
        try {
            readMETs();
        } catch (SAXException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
        Region world = new Region(0, "All");
        world.setChildren(new LinkedList<Region>());
        LinkedList<String> keys = new LinkedList(ddcMap.keySet());
        Collections.sort(keys);
        for (String ddc : keys) {
            Region r = new Region(ddcMap.get(ddc).size(), ddc);
            getParent(ddc, world).addChild(r);
        
        }
        return world;
    }

    private Region getParent(String child, Region root) {
        if (child.substring(1, 3).equals("00")) {
            return root;
        }
        String firstLevel = child.substring(0, 1);
        for (Region firstChildren : root.getChildren()){
            if (firstChildren.getName().startsWith(firstLevel)){
                //TODO check third level
                return firstChildren;
            }
        }
        Region newRegion = new Region(0, firstLevel+"00");
        root.addChild(newRegion);
        return newRegion;
        
    }

    private void layout(Region r) {
        int size = r.getRadius() * 2 + 40;
        r.setPosition(new Point(size / 2, size / 2));

        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setFont(new Font("Arial", Font.BOLD, 44));
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setStroke(new BasicStroke(5));
//        g.setColor(Color.red);
        drawRegion(g, r);
        try {
            ImageIO.write(bi, "png", new File("/tmp/world.png"));
        } catch (IOException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void drawRegion(Graphics g, Region r) {
//        System.out.println(r.getName() + ", " + r.getRadius() + ", " + r.getPosition());
        g.setColor(Color.ORANGE);
        g.drawOval(r.getPosition().x - r.getRadius(), r.getPosition().y - r.getRadius(), r.getRadius() * 2, r.getRadius() * 2);
        g.setColor(Color.blue);
        g.drawString(r.getName(), r.getPosition().x, r.getPosition().y);
//        System.out.println(r.getDocuments().size() + " for " + r.getName());
        for (String key : r.getDocuments().keySet()) {
            Point pos = r.getDocuments().get(key);
            g.setColor(Color.GRAY);
            g.drawRect(r.getPosition().x + pos.x, r.getPosition().y + pos.y, 10, 10);
        }
        if (r.getChildren() == null) {
            return;
        }
        r.reposChildren();
        for (Region child : r.getChildren()) {
//            System.out.println(child + " for " +r );
            drawRegion(g, child);
        }
    }

    class Region implements Comparable<Region> {

        private int documentNumber;
        private String name;
        private List<Region> children;
        private Point position;
        private int radius = 0;
        private Map<String, Point> documents = new HashMap<String, Point>();

        public Region(int number, String name) {
            documentNumber = number;
            this.name = name;
        }

        public int getRadius() {
            if (radius < 1) {
                int placed = 0;
                radius = 10;
                while (placed < getDocumentNumber()) {
                    int currentCircle = cirlesPerRadius(radius, circleSize);
                    if (currentCircle + placed > getDocumentNumber()) {
                        currentCircle = getDocumentNumber() - placed;
                    }

                    double angle = 2 * Math.PI / currentCircle + 1;
                    for (int i = 0; i < currentCircle; i++) {
//                        System.out.println("placing " + i);
                        int x = (int) (Math.cos(i * angle) * radius);
                        int y = (int) (Math.sin(i * angle) * radius);
//                        i++;
                        this.getDocuments().put(currentCircle + "." + i, new Point(x, y));
                    }

                    placed += currentCircle;
                    radius += circleSize;
                } // while documents
                
                placed = 0;
                if (children != null) {
                    Collections.sort(children);
                    int maxChildRadius = children.get(children.size() - 1).getRadius();
                    int placedChildren = 0;
                    List<Region> subList;
//                                            radius += maxChildRadius + 20;
                    while (placedChildren < children.size()) {
                        int currentChilds = 1;
                        while (true) {
                            if (currentChilds > children.size()) {
                                // there are no more children to place
                                break;
                            }
                            Region largest = children.get(placed + currentChilds - 1);
                            int possible = cirlesPerRadius(radius + largest.getRadius(), largest.getRadius());
                            if (this.getName().equals("All")){
                                System.out.println(largest.getName()+  ", " + largest.getRadius() + ", " + possible + ", " + radius);
                            }
                            if (possible < currentChilds) {
                                break;
                            }
                            currentChilds++;
                        }
                        
                        subList = children.subList(placedChildren, currentChilds - 1);

                        radius = radius += subList.get(subList.size() - 1).getRadius() * 2;
                        if (this.getName().equals("All")){
                        System.out.println(radius + " contains " + subList);
                        }
                        calculatePositions(radius, subList);
                        placedChildren += subList.size();
                    }
                    radius += maxChildRadius;
                }
            }
            return radius;
        }

        public void reposChildren() {
            if (children == null) {
                return;
            }
            for (Region child : children) {
                Point oldPos = child.getPosition();
//                System.out.println(child.getName() + " oldPos " + oldPos);
                child.setPosition(new Point(oldPos.x + this.getPosition().x, oldPos.y + this.getPosition().y));
            }
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

        public Map<String, Point> getDocuments() {
            return documents;
        }

        public void setDocuments(Map<String, Point> documents) {
            this.documents = documents;
        }

        public void addChild(Region child) {
            if (this.children == null) {
                children = new LinkedList<Region>();
            }
            children.add(child);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int compareTo(Region o) {
            if (this.getRadius() > o.getRadius()) {
                return 1;
            }
            return -1;
        }
    }
}
