package de.unigoettingen.sub;

import de.unigoettingen.sub.medas.client.MeDASClient;
import de.unigoettingen.sub.medas.model.Doc;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class TopicMapCreator {

    // All process METS files, unsorted and without hierarchy
    private Set<Doc> mets = new HashSet<>();
    // the largest region which contains all other
    private Region world;

    private DDCResolver ddcResolver;

    public static void main(String[] args) throws  IOException {

        TopicMapCreator app = new TopicMapCreator();
        app.run(args);
    }

    private void run(String[] args) throws  IOException {
        long start = System.currentTimeMillis();
        world = buildTreeFromMets();
    DotExporter dotExporter = new DotExporter();
        dotExporter.writeDotFile(world);
        System.out.println("processed METS " + (System.currentTimeMillis() - start) + "ms");
        Region region400 = getRegionWithName("430", getWorld());
        System.out.println(getIDsForRegion(region400));
    }


    /**
     * Get all documents from the MeDAS service.
     *
     * @return
     */
    @Deprecated
    private Set<Doc> readMETsFromMeDAS() {
        mets = new HashSet<>();
        MeDASClient meDAS = new MeDASClient();
        Set<Doc> allDocs = meDAS.getAllDocuments().getDocs();
        for (Doc doc : allDocs) {
            if (true || doc.getDDC() != null) { //FIXME remove again. What should be done with unclassified documents?
                mets.add(doc);
            }
        }
        return mets;
    }

    /**
     * Reads the METS from MeDAS and build a hierarchy of them. Therefore each
     * {@link Mets} is stored as document in its {@link Region}.
     *
     * @return The most general region (world).
     */
    private Region buildTreeFromMets()  {

//        getMETsFromMeDAS();
        MeDASReader reader = new MeDASReader();

        for (Doc m : reader.getMETsFromMeDAS()) {
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
//
//    /**
//     * Write the hierarchy of objects to a dot file to be processed by graphviz.
//     *
//     * @param r The base node of the tree.
//     */
//    private void writeDotFile(Region r) {
//        try {
//            BufferedWriter fw = new BufferedWriter(new FileWriter(new File(System.getProperty("java.io.tmpdir"), "topicMaps.dot")));
//            fw.write("graph world {\n");
//            fw.write("node [id=\"\\N\"];edge [id=\"\\T-\\H\"];");
//            //Each main topic of the DDC is on cluster
//            int cluster = 1;
//            HashMap<String, String> clusterMap = new HashMap<>();
//            for (Region island : r.getChildren()) {
//                fw.write("subgraph sub_" + island.getName() + " {\n");
//                writeDotLine(island, fw, cluster, clusterMap);
//                fw.write("}\n");
//                cluster++;
//            }
//            for (String key : clusterMap.keySet()) {
//                fw.write(key + clusterMap.get(key));
//            }
//            fw.write("}\n");
//            fw.close();
//        } catch (IOException ex) {
//            Logger.getLogger(TopicMapCreator.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    private void writeDotLine(Region r, BufferedWriter fw, int cluster, Map<String, String> clusterMap) throws IOException {
//
//        for (Region child : r.getChildren()) {
//            fw.write(String.format("%s -- %s ;\n", r.getName(), child.getName()));
//            clusterMap.put(r.getName(), String.format(" [cluster=\"%s\", label=\"%s\"];\n", cluster, getDDCResolver().getLabelForClass(r.getName())));
//
//            clusterMap.put(child.getName(), String.format(" [cluster=\"%s\"];\n", cluster));
//            writeDotLine(child, fw, cluster, clusterMap);
//        }
//        writeDocumentToDot(r, fw, cluster, clusterMap);
//    }
//
//    private void writeDocumentToDot(Region r, BufferedWriter fw, int cluster, Map<String, String> clusterMap) throws IOException {
//
//        for (Doc m : r.getDocuments().keySet()) {
//            String ppn = m.getPPN().replaceAll("-", "_");
//            fw.write(String.format("%s -- %s ;\n", r.getName(), ppn));
//            clusterMap.put(ppn, String.format(" [cluster=\"%s\", label=\"%s\"];\n ", cluster, m.getPPN()));
//        }
//    }

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
            idSet.add(doc.getPPN());
        }
        for (Region child : requestedRegion.getChildren()) {
            getIDsForRegion(child, idSet);
        }
        return idSet;
    }

    private Set<String> getIDsForRegion(Region r, Set<String> list) {
        list.add(r.getName());
        for (Doc m : r.getDocuments().keySet()) {
            list.add(m.getPPN());
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
        return getJournalRegionFor(m, r);
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
     * @param doc       the Mets to place
     * @param ddcRegion the DDC region where the host should be searched or
     *                  created.
     * @return The host region for the Mets.
     */
    private Region getJournalRegionFor(Doc doc, Region ddcRegion) {
        String host = doc.getHostPPN().iterator().next(); //TODO
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

}
