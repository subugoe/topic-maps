package de.unigoettingen.sub;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class TopicMapCreator {

    // All process METS files, unsorted and without hierarchy
    private Set<Mets> mets = new HashSet<Mets>();
    // the largest region which contains all other
    private Region world;
    // the DOM document
    private Document document;
    // the first graphics (g) element in the SVG document
    private Element firstG;
    private int circleSize = 20;
    // The directory with the METS files
    private File metsDir;

    public static void main(String[] args) throws SAXException, FileNotFoundException, IOException {

        TopicMapCreator app = new TopicMapCreator();
        app.run(args);
    }

    private void run(String[] args) throws SAXException, FileNotFoundException, IOException {
        long start = System.currentTimeMillis();
        processArgs(args);
        world = buildTreeFromMets();
        writeDotFile(world);
        System.out.println("donw");
        System.exit(0);
        System.out.println("processed METS " + (System.currentTimeMillis()-start) + "ms");
        layout(world);

        try {
            write();
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("runtime " + ((System.currentTimeMillis() - start)) + " ms");
    }

    /**
     * Checks if the parameter of the program is set an valid. If the parameter
     * is not valid a message is printed to err and the program exists. If the
     * parameter is missing the status code is "2", if the directory with the
     * METS files is not valid the status code is "3".
     *
     * @param args
     */
    private void processArgs(String[] args) {
        if (args.length != 1) {
            System.err.println("The programm need the directory with the METS files to parse as first parameter");
            System.exit(2);
        }
        metsDir = new File(args[0]);
        if (!metsDir.exists()) {
            System.err.println("The directory '" + metsDir + "' with the METS files does not exits.");
            System.exit(3);
        }
        if (!metsDir.isDirectory()) {
            System.err.println("'" + metsDir + "' should be a directory containing the METS files.");
            System.exit(3);
        }
        if (!metsDir.canExecute()) {
            System.err.println("The directory '" + metsDir + "' is not accessible.");
            System.exit(3);
        }
    }

    /**
     * Reads the METS files in the directory given as first parameter. The files
     * are not read recursively. All parsed METS files are parsed in the file
     * <code>mets</code> and are returned by the function.
     *
     * @throws SAXException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Set<Mets> readMETs() throws SAXException {

        File[] metsFiles = metsDir.listFiles();
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        MetsContentHandler metsContentHandler = new MetsContentHandler();

        for (int i = 0; i < metsFiles.length; i++) {
            try {
                if (metsFiles[i].isDirectory()) {
                    continue;
                }
                InputSource inputSource = new InputSource(new FileReader(metsFiles[i]));
                xmlReader.setContentHandler(metsContentHandler);
                xmlReader.parse(inputSource);
                Mets m = metsContentHandler.getMets();
                mets.add(m);
//                if (true || m.getDdcNumber() != null && m.getDdcNumber().startsWith("7") || m.getPPN().contains("487748506")) { //TODO || m.getDdcNumber() != null && m.getDdcNumber().startsWith("7")
//                    //            if (m.getDdcNumber() != null ) {
//                    mets.add(m);
//                }
            } catch (FileNotFoundException ex) {
                System.out.println("The file " + metsFiles[i] + " could not be found, skipping. " + ex);
            } catch (IOException ex) {
                System.out.println("The file " + metsFiles[i] + " could not be read, skipping. " + ex);
            } catch (Exception ex) {
                System.out.println("The file " + metsFiles[i] + " could not be parsed, skipping. " + ex);
            }
        }
        return mets;
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

    @Deprecated
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

    /**
     * Reads the METS files and build a hierarchy of them. Therefor each
     * {@link Mets} is stored as document in its {@link Region}.
     *
     * @return The most general region (world).
     */
    private Region buildTreeFromMets() throws SAXException, FileNotFoundException, IOException {

        readMETs();

        for (Mets m : mets) {
            Region r = getRegionFor(m);
            r.getDocuments().put(m, null);
        }
//        printTree(world, 0);
//        System.exit(0);
        return world;
    }

    private void printTree(Region r, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        System.out.println(r);
        for (Mets m : r.getDocuments().keySet()) {
            for (int i = 0; i < level; i++) {
                System.out.print("  ");
            }
            System.out.println("- " + m);
        }
        for (Region child : r.getChildren()) {
            printTree(child, level + 2);
        }

    }
    
    private void writeDotFile(Region r){
        System.out.println("graph world {");
        writeDotLine(r);
        System.out.println("}");
    }
    private void writeDotLine(Region r){
        for (Region child : r.getChildren()){
            System.out.println(r.getName() + " -- " + child.getName()+";");            
            writeDotLine(child);
        }
    }
    /**
     *
     * @param m
     * @return
     */
    private Region getRegionFor(Mets m) {

        Region r = getDdcRegionFor(m);
        Region journal = getJournalRegionFor(m, r);
        return journal;
    }

    /**
     * Get the DDC number of the METS. The DDC number is read from the METS
     * file, if there isn't a DDC number a warning is printed to stout and the
     * Mets is assigned to the region "world". If the region for the DDC does
     * not exists in the region "world" already it is created and added. Also
     * all required top level regions are created and inserted.
     *
     * @param m
     * @return
     */
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
                //second level
                for (Region secondLevel : firstLevel.getChildren()) {
                    if (ddc.startsWith(secondLevel.getName().substring(0, 2))) {
                        if (ddc.substring(2, 3).equals("0")) {
                            return secondLevel;
                        }
                        //third level
                        for (Region thirdLevel : secondLevel.getChildren()) {
                            if (ddc.equals(thirdLevel.getName())) {
                                return thirdLevel;
                            }
                        }
                        Region r = new Region(0, ddc);
                        secondLevel.addChild(r);
                        return r;
                    }
                }
                Region r = new Region(0, ddc.substring(0, 2) + "0");
                firstLevel.addChild(r); //second level added, try again
                return getDdcRegionFor(m);
            }
        }
        Region r = new Region(0, ddc.substring(0, 1) + "00");
        getWorld().addChild(r); // First level added, lets try again.                
        return getDdcRegionFor(m);
    }

    /**
     * Gets the region for the host (journal) of the {@link Mets} inside of the
     * region of the given DDC. If the host is not present in the DDC region
     * yet, it is created.
     *
     * @param m the Mets to place
     * @param ddcRegion the DDC region where the host should be searched or
     * created.
     * @return The host region for the Mets.
     */
    private Region getJournalRegionFor(Mets m, Region ddcRegion) {
        String host = m.getHost();
        if (host == null || host.trim().length() < 1) {
            System.out.println("no journal for " + m);
            return ddcRegion;
        }
//        if (host.equals(m.getDdcNumber())) {
//            System.out.println("?????");
//            return ddcRegion;
//        }
        for (Region child : ddcRegion.getChildren()) {
            if (child.getName().equals(host)) {
                return child;
            }
        }
        Region hostRegion = new Region(0, host);
        ddcRegion.addChild(hostRegion);

        return hostRegion;
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

    /**
     * Positions all elements of the given region. For each child a separate
     * thread is started to do the layout in parallel.
     *
     * @param r
     */
    private void layout(Region r) {
        HashSet<Thread> threadpool = new HashSet<Thread>();
        for (Region c : r.getChildren()) {
            OwnRunnable run = new OwnRunnable();
            run.setRegion(c);
            Thread t = new Thread(run);
            t.start();
            threadpool.add(t);
        }
        for (Thread t : threadpool) {
            try {
//                System.out.println("joining thread " + t);
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



    }

    /**
     * Writes the generated DOM to a SVG file. The document to write out should
     * be saved in the field {
     *
     * @ #document}
     *
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    private void write() throws TransformerConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        File outFile = new File("/var/www/topics.svg");

        transformer.transform(source, new StreamResult(outFile.getPath()));

    }

    private Graphics2D getGraphics(int size) {
        return getSVGGraphics(size);
    }
//
//    private Graphics2D getJavaGraphics(int size) {
//        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
//        return (Graphics2D) bi.getGraphics();
//    }
//

    /**
     * Generates an default SVG document. The document has the given size (as
     * quadrat) and contains the default elements (g, viewbox) but no content.
     *
     * @param size
     * @return
     */
    private Graphics2D getSVGGraphics(int size) {
        // Get a DOMImplementation.
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        //        root = svgGenerator.getRoot();
        Element svgNode = document.getDocumentElement();

        svgNode.setAttributeNS(null, "style", "stroke-dasharray:none; shape-rendering:auto; font-family:&apos;Dialog&apos;; text-rendering:auto; fill-opacity:1; color-interpolation:auto; color-rendering:auto; font-size:12; fill:black; stroke:black; image-rendering:auto; stroke-miterlimit:10; stroke-linecap:square; stroke-linejoin:miter; font-style:normal; stroke-width:1; stroke-dashoffset:0; font-weight:normal; stroke-opacity:1;");
        svgNode.setAttributeNS(null, "viewBox", String.format("0 0 %d %d", size, size));
        svgNode.setAttributeNS(null, "id", "svgroot");
        svgNode.setAttributeNS(null, "xmlns:xlink", "http://www.w3.org/1999/xlink");

        firstG = document.createElementNS(svgNS, "g");
        firstG.setAttributeNS(null, "transform", "translate(0,200)");
        firstG.setAttributeNS(svgNS, "id", "viewport");
        svgNode.appendChild(firstG);
        svgGenerator.setSVGCanvasSize(new Dimension(500, 500));
        Element scriptNode = document.createElementNS(null, "script");
        scriptNode.setAttributeNS(null, "xlink:href", "SVGPan.js");
        scriptNode.setAttributeNS(svgNS, "type", "text/javascript");
        svgNode.appendChild(scriptNode);
        return svgGenerator;
    }

    /**
     * Adds all elements of the Region to the SVG DOM.
     *
     * @param g
     * @param r
     */
    private void drawRegion(Graphics g, Region r) {
        Element circle = createCircleElement(r);

        firstG.appendChild(circle);


        if (!r.getName().startsWith("PPN")) {
            Element text = createTextElement(r);
            firstG.appendChild(text);
        }

        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element gElement = document.createElementNS(svgNS, "g");
        gElement.setAttributeNS(null, "display", "none");
        firstG.appendChild(gElement);
        for (Mets key : r.getDocuments().keySet()) {

            Point pos = r.getDocuments().get(key);

            Element rec = createRectElem(r, pos, key);
            gElement.appendChild(rec);
//            firstG.appendChild(rec);
        }
        if (r.getChildren() == null) {
            return;
        }
        r.reposChildren();
        for (Region child : r.getChildren()) {
            drawRegion(g, child);
        }
    }

    private Element createRectElem(Region r, Point pos, Mets m) {
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element e = document.createElementNS(svgNS, "rect");
        e.setAttributeNS(svgNS, "id", m.getPPN());
        e.setAttributeNS(svgNS, "type", "");
        e.setAttributeNS(svgNS, "x", Integer.toString(r.getPosition().x + pos.x));
        e.setAttributeNS(svgNS, "y", Integer.toString(r.getPosition().y + pos.y));
        e.setAttributeNS(svgNS, "width", "10");
        e.setAttributeNS(svgNS, "height", "10");
        e.setAttributeNS(svgNS, "style", "fill:none; stroke:gray; stroke-width:5;");
        return e;
    }

    private Element createTextElement(Region r) {

        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element e = document.createElementNS(svgNS, "text");
        e.setAttributeNS(svgNS, "x", Integer.toString(r.getPosition().x));
        e.setAttributeNS(svgNS, "y", Integer.toString(r.getPosition().y));
        e.setAttributeNS(svgNS, "style", "fill:blue; stroke:none; stroke-width:5;");
        e.appendChild(document.createTextNode(r.getName()));
        return e;
    }

    private Element createCircleElement(Region r) {
        //g.drawOval(r.getPosition().x - r.getRadius(), r.getPosition().y - r.getRadius(), r.getRadius() * 2, r.getRadius() * 2);
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element e = document.createElementNS(svgNS, "circle");

        e.setAttributeNS(svgNS, "r", Integer.toString(r.getRadius()));
        e.setAttributeNS(svgNS, "cx", Integer.toString(r.getPosition().x));
        e.setAttributeNS(svgNS, "cy", Integer.toString(r.getPosition().y));
        String color;
        if (r.getName().startsWith("PPN")) {
            color = "red";
            e.setAttributeNS(svgNS, "type", "journal");
        } else {
            color = "rgb(255,200,0)";
            e.setAttributeNS(svgNS, "type", "ddc_topic");
        }

        e.setAttributeNS(svgNS, "style", String.format("fill:none; stroke:%s; stroke-width:5;", color));
        return e;
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

        /**
         * Calculates the radius of a region. First the documents of the region
         * are placed on circles. Next the sub regions are placed on larger
         * circles. The radius of the circles is chosen in a way, that the
         * largest child fits on the radius. In order to place the sub regions
         * their radius has to be determined, so that all downstream regions are
         * calculated recursively.
         *
         * @return
         */
        private int getRadius() {
            if (radius > 0) {
                return radius;
            }
            radius = placeDocuments();

            if (children != null && children.size() > 0) {
                int overallPlaced = 0; // number of children placed, independed of the circle

                Collections.sort(children);
                int maxChildRadius = children.get(children.size() - 1).getRadius();

                int placedChildren = 0;
                List<Region> subList;
                while (placedChildren < children.size()) {

                    int currentChild = 1;
                    while (true) {
                        if (currentChild > children.size()) {
                            // there are no more children to place
                            break;
                        }

                        Region largest = children.get(overallPlaced + currentChild - 1);
                        int largestRadius = largest.getRadius();
                        int possible = circlesPerRadius(radius + largestRadius, largestRadius);
                        currentChild++;
                        if (possible < currentChild) {
                            break;
                        }

                    }
                    subList = children.subList(placedChildren, currentChild - 1);
                    if (subList.size() < 1) {
                        System.out.println("error sublist of children to place is empty");
                        continue;
                    }
                    radius += subList.get(subList.size() - 1).getRadius() * 2;
                    calculatePositions(radius, subList);

                    placedChildren += subList.size();
                }
                radius += maxChildRadius;
            }

            return radius;
        }

        /**
         * Place the documents of the region in the inner circles. The size of a
         * document symbol is fixed and defined in the field {
         *
         * @ #circleSize}.
         * @return
         */
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

        /**
         * Reposition the children relative to the new position of the region.
         *
         */
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
