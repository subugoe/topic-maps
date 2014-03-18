package de.unigoettingen.sub;

import de.unigoettingen.sub.medas.client.MeDASClient;
import de.unigoettingen.sub.medas.model.Doc;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class TopicMapCreator {

    // All process METS files, unsorted and without hierarchy
    private Set<Doc> mets = new HashSet<>();
    // the largest region which contains all other
    private Region world;
    // the DOM document
    private Document document;
    // the first graphics (g) element in the SVG document
    private Element firstG;

    // The directory with the METS files

    private DDCResolver ddcResolver;

    public static void main(String[] args) throws SAXException, FileNotFoundException, IOException {

        TopicMapCreator app = new TopicMapCreator();
        app.run(args);
    }

    private void run(String[] args) throws SAXException, FileNotFoundException, IOException {
        long start = System.currentTimeMillis();
        world = buildTreeFromMets();

        writeDotFile(world);
        System.out.println("processed METS " + (System.currentTimeMillis() - start) + "ms");
        Region region400 = getRegionWithName("430", getWorld());
        System.out.println(getIDsForRegion(region400));
    }


    /**
     * Get all documents from the MeDAS service.
     *
     * @return
     */
    private Set<Doc> readMETsFromMeDAS() {
        mets = new HashSet<>();
        MeDASClient meDAS = new MeDASClient();
        Set<Doc> allDocs = meDAS.getAllDocuments().getDocs();
        for (Doc doc : allDocs) {
            if (true || doc.getDDC() != null) { //FIXME remove again
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

    /**
     * Reads the METS from MeDAS and build a hierarchy of them. Therefore each
     * {@link Mets} is stored as document in its {@link Region}.
     *
     * @return The most general region (world).
     */
    private Region buildTreeFromMets() throws SAXException, FileNotFoundException, IOException {

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
     * @param r     The base node of the tree.
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
     *
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

    private Region getRegionWithName(String name, Region currentRegion) {
        if (name.equals(currentRegion.getName())) {
            return currentRegion;
        }
        for (Region child : currentRegion.getChildren()) {
            Region result = getRegionWithName(name, child);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Set<String> getIDsForRegion(Region requestedRegion) {
        Set<String> idSet = new HashSet<>();
        idSet.add(requestedRegion.getName());
        for (Doc doc : requestedRegion.getDocuments().keySet()) {
            idSet.add(doc.getId().getValue());
        }
        for (Region child : requestedRegion.getChildren()) {
            getIDsForRegion(child, idSet);
        }
        return idSet;
    }

    private Set<String> getIDsForRegion(Region r, Set<String> list) {
        list.add(r.getName());
        for (Doc m : r.getDocuments().keySet()) {
            list.add(m.getId().getValue());
        }
        for (Region child : r.getChildren()) {
            getIDsForRegion(child, list);
        }

        return list;
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
     * @param doc       the Mets to place
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


    private DDCResolver getDDCResolver() {
        if (ddcResolver == null) {
            ddcResolver = new DDCResolver();
        }
        return ddcResolver;
    }


//    class CircleRegion implements Comparable<CircleRegion> {
//
//        private int documentNumber;
//        private String name;
//        private List<CircleRegion> children = new LinkedList<CircleRegion>();
//        private Point position;
//        private int radius = 0;
//        private Map<Doc, Point> documents = new HashMap<>();
//
//        public CircleRegion(int number, String name) {
//            documentNumber = number;
//            this.name = name;
//        }
//
//        /**
//         * Calculates the radius of a region. First the documents of the region
//         * are placed on circles. Next the sub regions are placed on larger
//         * circles. The radius of the circles is chosen in a way, that the
//         * largest child fits on the radius. In order to place the sub regions
//         * their radius has to be determined, so that all downstream regions are
//         * calculated recursively.
//         *
//         * @return
//         */
//        @Deprecated
//        private int getRadius() {
//            if (radius > 0) {
//                return radius;
//            }
//            radius = placeDocuments();
//
//            if (children != null && children.size() > 0) {
//                int overallPlaced = 0; // number of children placed, independed of the circle
//
//                Collections.sort(children);
//                int maxChildRadius = children.get(children.size() - 1).getRadius();
//
//                int placedChildren = 0;
//                List<CircleRegion> subList;
//                while (placedChildren < children.size()) {
//
//                    int currentChild = 1;
//                    while (true) {
//                        if (currentChild > children.size()) {
//                            // there are no more children to place
//                            break;
//                        }
//
//                        CircleRegion largest = children.get(overallPlaced + currentChild - 1);
//                        int largestRadius = largest.getRadius();
//                        int possible = circlesPerRadius(radius + largestRadius, largestRadius);
//                        currentChild++;
//                        if (possible < currentChild) {
//                            break;
//                        }
//
//                    }
//                    subList = children.subList(placedChildren, currentChild - 1);
//                    if (subList.size() < 1) {
//                        System.out.println("error sublist of children to place is empty");
//                        continue;
//                    }
//                    radius += subList.get(subList.size() - 1).getRadius() * 2;
//                    calculatePositions(radius, subList);
//
//                    placedChildren += subList.size();
//                }
//                radius += maxChildRadius;
//            }
//
//            return radius;
//        }
//
//        /**
//         * Place the documents of the region in the inner circles. The size of a
//         * document symbol is fixed and defined in the field {
//         *
//         * @return
//         * @ #circleSize}.
//         */
//        private int placeDocuments() {
//            int currentRadius = 30;
//            List<Doc> docs = new ArrayList<>(getDocuments().keySet());
//
//            for (int i = 0; i < docs.size(); ) { // increment is in the inner loop
//                int documentsOnCircle = circlesPerRadius(currentRadius, circleSize);
//                if (documentsOnCircle > docs.size() - i) {
//                    documentsOnCircle = docs.size() - i;
//                }
//
//                double angle = 2 * Math.PI / documentsOnCircle + 1;
//                for (int j = 0; j < documentsOnCircle; j++) {
//                    int x = (int) (Math.cos(j * angle) * currentRadius);
//                    int y = (int) (Math.sin(j * angle) * currentRadius);
//
//                    documents.put(docs.get(i), new Point(x, y));
//                    i++;
//                }
//                currentRadius = currentRadius + circleSize;
//            }
//            return currentRadius;
//        }
//
//        /**
//         * Reposition the children relative to the new position of the region.
//         */
//        public void reposChildren() {
//            if (children == null) {
//                return;
//            }
//            for (CircleRegion child : children) {
//                Point oldPos = child.getPosition();
//                child.setPosition(new Point(oldPos.x + this.getPosition().x, oldPos.y + this.getPosition().y));
//            }
//        }
//
//        public int getDocumentNumber() {
//            return documentNumber;
//        }
//
//        public void setDocumentNumber(int documentNumber) {
//            this.documentNumber = documentNumber;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public void setName(String name) {
//            this.name = name;
//        }
//
//        public List<CircleRegion> getChildren() {
//            return children;
//        }
//
//        public void setChildren(List<CircleRegion> childs) {
//            this.children = childs;
//        }
//
//        public Point getPosition() {
//            return position;
//        }
//
//        public void setPosition(Point position) {
//            this.position = position;
//        }
//
//        public Map<Doc, Point> getDocuments() {
//            return documents;
//        }
//
//        public void setDocuments(Map<Doc, Point> documents) {
//            this.documents = documents;
//        }
//
//        public void addChild(CircleRegion child) {
//            if (this.children == null) {
//                children = new LinkedList<CircleRegion>();
//            }
//            children.add(child);
//        }
//
//        @Override
//        public String toString() {
//            return name + "(" + radius + ")";
//        }
//
//        @Override
//        public int compareTo(CircleRegion o) {
//
//            int otherRadius = o.getRadius();
//            int ownRadius = this.getRadius();
//
//            if (ownRadius == otherRadius) {
//                return 0;
//            }
//            if (ownRadius > otherRadius) {
//                return 1;
//            }
//            return -1;
//        }
//    }
}
