package de.ungoettingen.sub;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class TopicMapCreator {

    private int circleSize = 20;

    public static void main(String[] args) {

        TopicMapCreator app = new TopicMapCreator();
        app.run();
    }
    private Set<Mets> mets = new HashSet<Mets>();
    private Region world;

    private void run() {
//        Region world = buildTree();
        Region world = buildTreeFromMets();
        layout(world);
    }

    private void readMETs() throws SAXException, FileNotFoundException, IOException {
        File dir = new File("/home/jdo/digizeit/METS/mets_repository/indexed_mets/");
        File[] metsFiles = dir.listFiles();
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        MetsContentHandler metsContentHandler = new MetsContentHandler();
        for (int i = 0; i < metsFiles.length; i++) {
            if (metsFiles[i].isDirectory()) {
                continue;
            }
            InputSource inputSource = new InputSource(new FileReader(metsFiles[i]));
            xmlReader.setContentHandler(metsContentHandler);
            xmlReader.parse(inputSource);
            Mets m = metsContentHandler.getMets();
            if ( m.getDdcNumber() != null && ! m.getDdcNumber().startsWith("7")) {         
            this.mets.add(m);
            }
        }
    }

    private void calculatePositions(int radius, List<Region> items) {
        int i = 0;
        double angle = 2 * Math.PI / items.size();
        for (Region item : items) {
            int x = (int) (Math.cos(i * angle) * radius);
            int y = (int) (Math.sin(i * angle) * radius);
            i++;
            item.setPosition(new Point(x, y));
        }

    }

    private int circlesPerRadius(int radius, int circleSize) {
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

        for (Mets m : mets) {
            Region r = getRegionFor(m);
            r.getDocuments().put(m, null);
        }
        return world;
    }

    private Region getRegionFor(Mets m) {
        Region r = getDdcRegionFor(m);
        Region journal = getJournalRegionFor(m, r);
        return journal;
    }

    private Region getDdcRegionFor(Mets m) {
        String ddc = m.getDdcNumber();
        if (ddc == null) {
            System.out.println("no ddc for " + m.getPPN());
            return getWorld();
        }
        for (Region firstLevel : getWorld().getChildren()) {
            if (firstLevel.getName().startsWith(ddc.substring(0, 1))) {
                if (ddc.substring(1, 3).equals("00")) {
                    return firstLevel;
                }
                for (Region secondLevel : firstLevel.getChildren()) {
                    if (ddc.startsWith(secondLevel.getName().substring(0, 2))) {
                        return secondLevel;
                    }
                }
                Region r = new Region(0, ddc);
                firstLevel.addChild(r);
                return r;
            }
        }
        Region r = new Region(0, ddc.substring(0, 1) + "00");
        getWorld().addChild(r);
        return r;
    }

    private Region getJournalRegionFor(Mets m, Region ddcRegion) {
        String host = m.getHost();
        if (host == null || host.trim().length() < 1) {
            System.out.println("no journal for " + m);
            return ddcRegion;
        }
        if (host.equals(m.getDdcNumber())) {
            return ddcRegion;
        }
        for (Region child : ddcRegion.getChildren()) {
            if (child.getName().equals(host)) {
                return child;
            }
        }
        Region hostRegion = new Region(0, host);
        ddcRegion.addChild(hostRegion);
        return hostRegion;
//        return ddcRegion;
    }

    private Region getWorld() {
        if (this.world == null) {
            world = new Region(0, "All");
            world.setChildren(new LinkedList<Region>());
        }
        return world;
    }

    class OwnRunnable implements Runnable {

        private Region c;

        public void setRegion(Region c) {
            this.c = c;
        }

        public void run() {
            c.getRadius();
        }
    }

    private void layout(Region r) {
        System.out.println("doing layout");
        HashSet<Thread> threadpool = new HashSet<Thread>();
        for (Region c : r.getChildren()) {
            OwnRunnable run = new OwnRunnable();
            run.setRegion(c);
            Thread t = new Thread(run);
            t.start();
            threadpool.add(t);
            System.out.println(t + "added for " + c);
        }
        for (Thread t : threadpool) {
            try {
                System.out.println("joining thread " + t);
                t.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        int size = r.getRadius() * 2 + 40;
        r.setPosition(new Point(size / 2, size / 2));
        System.out.println("image size " + size);
        Graphics2D g = getGraphics(size);
        g.setFont(new Font("Arial", Font.BOLD, 44));
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setStroke(new BasicStroke(5));
        drawRegion(g, r);
        Writer out;
        try {            
            out = new OutputStreamWriter(new FileOutputStream(new File("/tmp/world.svg")), "UTF-8");
            Element root = ((SVGGraphics2D )g).getRoot();
            root.setAttributeNS(null, "viewBox", "0 0 "+ size + " " + size);
            ((SVGGraphics2D) g).stream(root, out, true);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SVGGraphics2DIOException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

//        try {
//            ImageIO.write(bi, "png", new File(System.getProperty("java.io.tmpdir"), "world.png"));
//        } catch (IOException ex) {
//            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    private Graphics2D getGraphics(int size) {
        if (true) {
            return getSVGGraphics(size);
        }
        return getJavaGraphics(size);
    }

    private Graphics2D getJavaGraphics(int size) {
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        return (Graphics2D) bi.getGraphics();
    }

    private Graphics2D getSVGGraphics(int size) {
        // Get a DOMImplementation.
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        Element root = svgGenerator.getRoot();
        root.setAttributeNS(null, "viewBox", "0 0 800 600");
        svgGenerator.setSVGCanvasSize(new Dimension(500,500));
        return svgGenerator;
    }

    private void drawRegion(Graphics g, Region r) {
        if (r.getName().startsWith("PPN")) {
            g.setColor(Color.red);
        } else {
            g.setColor(Color.ORANGE);
        }
        g.drawOval(r.getPosition().x - r.getRadius(), r.getPosition().y - r.getRadius(), r.getRadius() * 2, r.getRadius() * 2);
        if (!r.getName().startsWith("PPN")) {
            g.setColor(Color.blue);
            g.drawString(r.getName(), r.getPosition().x, r.getPosition().y);
        }
        for (Mets key : r.getDocuments().keySet()) {
            Point pos = r.getDocuments().get(key);
            g.setColor(Color.GRAY);
            g.drawRect(r.getPosition().x + pos.x, r.getPosition().y + pos.y, 10, 10);

        }
        if (r.getChildren() == null) {
            return;
        }
        r.reposChildren();
        for (Region child : r.getChildren()) {
            drawRegion(g, child);
        }
    }

    class Region implements Comparable<Region> {

        private int documentNumber;
        private String name;
        private List<Region> children = new LinkedList<Region>();
        private Point position;
        private int radius = 0;
        private Map<Mets, Point> documents = new HashMap<Mets, Point>();

        public Region(int number, String name) {
            documentNumber = number;
            this.name = name;
        }

        public int getRadius() {

            LinkedList<Region> l = new LinkedList<Region>();
            return getRadius(l);
        }

        private int getRadius(List<Region> parents) {
            if (parents.contains(this)) {
                System.out.println(parents + " + " + this);
                System.exit(33);
            }
//            System.out.println(this + " adding to " + parents);
            parents.add(this);
            if (radius < 1) {
                radius = placeDocuments();
                int placed = 0;
                if (children != null && children.size() > 0) {
                    Collections.sort(children);
                    int maxChildRadius = children.get(children.size() - 1).getRadius(parents);
                    int placedChildren = 0;
                    List<Region> subList;
                    while (placedChildren < children.size()) {
                        int currentChilds = 1;
                        while (true) {
                            if (currentChilds > children.size()) {
                                // there are no more children to place
                                break;
                            }

                            Region largest = children.get(placed + currentChilds - 1);
                            int largestRadius = largest.getRadius();
                            int possible = circlesPerRadius(radius + largestRadius, largestRadius);

                            if (possible < currentChilds) {
                                break;
                            }
                            currentChilds++;
                        }

                        subList = children.subList(placedChildren, currentChilds - 1);
                        if (subList.size() < 1) {
                            continue;
                        }
                        radius += subList.get(subList.size() - 1).getRadius() * 2;
                        calculatePositions(radius, subList);
                        placedChildren += subList.size();
                    }
                    radius += maxChildRadius;
                }
            }
            return radius;
        }

        private int placeDocuments() {
            int currentRadius = 30;
            List<Mets> docs = new ArrayList<Mets>(getDocuments().keySet());

            for (int i = 0; i < docs.size();) { // increment is in the inner loop
                int documentsOnCircle = circlesPerRadius(currentRadius, circleSize);
                if (documentsOnCircle > docs.size() - i) {
                    documentsOnCircle = docs.size() - i;
                }

                double angle = 2 * Math.PI / documentsOnCircle + 1;
                for (int j = 0; j < documentsOnCircle; j++) {
                    int x = (int) (Math.cos(j * angle) * currentRadius);
                    int y = (int) (Math.sin(j * angle) * currentRadius);

                    documents.put(docs.get(i), new Point(x, y));
                    i++;
                }
                currentRadius = currentRadius + circleSize;
            }
            return currentRadius;
        }

        public void reposChildren() {
            if (children == null) {
                return;
            }
            for (Region child : children) {
                Point oldPos = child.getPosition();
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

        public Map<Mets, Point> getDocuments() {
            return documents;
        }

        public void setDocuments(Map<Mets, Point> documents) {
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
            return name + "(" + radius + ")";
        }

        @Override
        public int compareTo(Region o) {

            int otherRadius = o.getRadius();
            int ownRadius = this.getRadius();

            if (ownRadius == otherRadius) {
                return 0;
            }
            if (ownRadius > otherRadius) {
                return 1;
            }

            return -1;
        }
    }
}
