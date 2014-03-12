package de.unigoettingen.sub;

import de.unigoettingen.sub.model.Doc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
    private Set<Doc> mets = new HashSet<>();
    // the largest region which contains all other
    private Region world;
    // the DOM document
    private Document document;
    // the first graphics (g) element in the SVG document
    private Element firstG;
    private int circleSize = 20;
    // The directory with the METS files

    private DDCResolver ddcResolver;

    public static void main(String[] args) throws SAXException, FileNotFoundException, IOException {

        TopicMapCreator app = new TopicMapCreator();
        app.run(args);
    }

    private void run(String[] args) throws SAXException, FileNotFoundException, IOException {
        long start = System.currentTimeMillis();
        processArgs(args);
        world = buildTreeFromMets();

        writeDotFile(world);
        System.out.println("processed METS " + (System.currentTimeMillis() - start) + "ms");
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
//            System.exit(2);
            return;
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

//    /**
//     * Reads the METS files in the directory given as first parameter. The files
//     * are not read recursively. All parsed METS files are parsed in the file
//     * <code>mets</code> and are returned by the function.
//     *
//     * @throws SAXException
//     * @throws FileNotFoundException
//     * @throws IOException
//     */
//    private Set<Doc> readMETs() throws SAXException {
//
//        File[] metsFiles = metsDir.listFiles();
//        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
//        MetsContentHandler metsContentHandler = new MetsContentHandler();
//
//        for (int i = 0; i < metsFiles.length; i++) {
//            try {
//                if (metsFiles[i].isDirectory()) {
//                    continue;
//                }
//                InputSource inputSource = new InputSource(new FileReader(metsFiles[i]));
//                xmlReader.setContentHandler(metsContentHandler);
//                xmlReader.parse(inputSource);
//                Doc m = metsContentHandler.getMets();
//                mets.add(m);
////                if (true || m.getDdcNumber() != null && m.getDdcNumber().startsWith("7") || m.getPPN().contains("487748506")) { //TODO || m.getDdcNumber() != null && m.getDdcNumber().startsWith("7")
////                    //            if (m.getDdcNumber() != null ) {
////                    mets.add(m);
////                }
//            } catch (FileNotFoundException ex) {
//                System.out.println("The file " + metsFiles[i] + " could not be found, skipping. " + ex);
//            } catch (IOException ex) {
//                System.out.println("The file " + metsFiles[i] + " could not be read, skipping. " + ex);
//            } catch (Exception ex) {
//                System.out.println("The file " + metsFiles[i] + " could not be parsed, skipping. " + ex);
//            }
//        }
//        return mets;
//    }

    private Set<Doc> readMETsFromMeDAS() {
        mets = new HashSet<>();
        MeDASClient meDAS = new MeDASClient();
        Set<Doc> allDocs = meDAS.getAllDocuments().getDocs();
        for (Doc doc : allDocs) {
            if (doc.getDDC() != null) { //FIXME remove again
                mets.add(doc);
            }
        }
        return mets;
    }


    private Mets createMetsFromDoc(Doc doc) {
        Mets m = new Mets();
        m.setMedasID(doc.getDocid());
//        if (doc.getClassifications() != null && doc.getClassifications().size() > 0) {
//            m.setDdcNumber(doc.getClassifications().iterator().next().getValue());
//        }
        m.setDdcNumber(doc.getDDCNumber());
//        if (doc.getRelatedItems() != null && doc.getRelatedItems().size() > 0) {
//            m.setHost(doc.getRelatedItems().iterator().next().getHost());
//        }
        m.setHost(doc.getHostPPN());
        m.setPPN(doc.getId().getValue());
        return m;
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

    /**
     * Reads the METS files and build a hierarchy of them. Therefore each
     * {@link Mets} is stored as document in its {@link Region}.
     *
     * @return The most general region (world).
     */
    private Region buildTreeFromMets() throws SAXException, FileNotFoundException, IOException {

//        readMETs();
        readMETsFromMeDAS();
        for (Doc m : mets) {
            Region r = getRegionFor(m);
            r.getDocuments().put(m, null);
        }
//        printTree(world, 0);
//        System.exit(0);
        return world;
    }

    /**
     * Debugging method to print the hierarchy to the console. For each level the function calls itself recursively.
     * For the first call level should be "0". The level controls the indenting of the tree branch.
     *
     * @param r The base node of the tree.
     * @param level The current depth of the given base node.
     */
    private void printTree(Region r, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        System.out.println(r);
        for (Doc m : r.getDocuments().keySet()) {
            for (int i = 0; i < level; i++) {
                System.out.print("  ");
            }
            System.out.println(" - " + m);
        }
        for (Region child : r.getChildren()) {
            printTree(child, level + 2);
        }
    }

    /**
     * Write the hierarchy of objects to a dot file to be processed by graphviz.
     * @param r The base node of the tree.
     */
    private void writeDotFile(Region r) {
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(new File(System.getProperty("java.io.tmpdir"), "topicMaps.dot")));
            fw.write("graph world {\n");
            fw.write("node [id=\"\\N\"];edge [id=\"\\T-\\H\"];");
            //Each main topic of the DDC is on cluster
            int cluster = 1;
            HashMap<String, String> clusterMap = new HashMap<String, String>();
            for (Region island : r.getChildren()) {
                fw.write("subgraph sub_" + island.getName() + " {\n");
                writeDotLine(island, fw, cluster, clusterMap);
                fw.write("}\n");
                cluster++;
            }
            for (String key : clusterMap.keySet()) {
                fw.write(key + clusterMap.get(key));
            }
            fw.write("}\n");
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeDotLine(Region r, BufferedWriter fw, int cluster, Map<String, String> clusterMap) throws IOException {

        for (Region child : r.getChildren()) {
            fw.write(String.format("%s -- %s ;\n", r.getName(), child.getName()));
            clusterMap.put(r.getName(), String.format(" [cluster=\"%s\", label=\"%s\"];\n", cluster, getDDCResolver().getLabelForClass(r.getName())));

            clusterMap.put(child.getName(), String.format(" [cluster=\"%s\"];\n", cluster));
            writeDotLine(child, fw, cluster, clusterMap);
        }
        writeDocumentToDot(r, fw, cluster, clusterMap);
    }

    private void writeDocumentToDot(Region r, BufferedWriter fw, int cluster, Map<String, String> clusterMap) throws IOException {

        for (Doc m : r.getDocuments().keySet()) {
            String ppn = m.getId().getValue().replaceAll("-", "_"); //TODO change to getPPN
            fw.write(String.format("%s -- %s ;\n", r.getName(), ppn));
            clusterMap.put(ppn, String.format(" [cluster=\"%s\", label=\"%s\"];\n ", cluster, m.getId().getValue()));
        }
    }

    private void getIDsForRegion(Region requestedRegion, Region currentRegion){
        List<String> idList = new LinkedList<>();
    }
    private void getIDsForRegion(Region r, List<String> list){
        list.add(r.getName());
        for  (Doc  m: r.getDocuments().keySet()){
//            list.add(m.g) //FIXME
        }
    }
    /**
     * @param m
     * @return
     */
    private Region getRegionFor(Doc m) {

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
    private Region getDdcRegionFor(Doc m) {
        String ddc = m.getDDCNumber();
        if (ddc == null) {
            System.out.println("no ddc for " + m.getId());
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
     * @param doc         the Mets to place
     * @param ddcRegion the DDC region where the host should be searched or
     *                  created.
     * @return The host region for the Mets.
     */
    private Region getJournalRegionFor(Doc doc, Region ddcRegion) {
        String host = doc.getHostPPN();
        if (host == null || host.trim().length() < 1) {
            System.out.println("no journal for " + doc);
            return ddcRegion;
        }
//        if (host.equals(doc.getDdcNumber())) {
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
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @ #document}
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
        //root = svgGenerator.getRoot();
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
//
//    private Graphics2D getJavaGraphics(int size) {
//        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
//        return (Graphics2D) bi.getGraphics();
//    }
//

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
        for (Doc key : r.getDocuments().keySet()) {

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

    private Element createRectElem(Region r, Point pos, Doc m) {
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Element e = document.createElementNS(svgNS, "rect");
        e.setAttributeNS(svgNS, "id", m.getId().getValue());
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

    private DDCResolver getDDCResolver() {
        if (ddcResolver == null) {
            ddcResolver = new DDCResolver();
        }
        return ddcResolver;
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

    class Region implements Comparable<Region> {

        private int documentNumber;
        private String name;
        private List<Region> children = new LinkedList<Region>();
        private Point position;
        private int radius = 0;
        private Map<Doc, Point> documents = new HashMap<>();

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
         * @return
         * @ #circleSize}.
         */
        private int placeDocuments() {
            int currentRadius = 30;
            List<Doc> docs = new ArrayList<>(getDocuments().keySet());

            for (int i = 0; i < docs.size(); ) { // increment is in the inner loop
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
