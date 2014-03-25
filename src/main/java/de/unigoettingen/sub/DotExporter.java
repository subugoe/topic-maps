package de.unigoettingen.sub;

import de.unigoettingen.sub.medas.model.Doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jdo on 3/25/14.
 */
public class DotExporter {

    private DDCResolver ddcResolver;

    /**
     * Write the hierarchy of objects to a dot file to be processed by graphviz.
     *
     * @param r The base node of the tree.
     */
    public void writeDotFile(Region r) {
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(new File(System.getProperty("java.io.tmpdir"), "topicMaps.dot")));
            fw.write("graph world {\n");
            fw.write("node [id=\"\\N\"];edge [id=\"\\T-\\H\"];");
            //Each main topic of the DDC is on cluster
            int cluster = 1;
            HashMap<String, String> clusterMap = new HashMap<>();
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
            String ppn = m.getPPN().replaceAll("-", "_");
            fw.write(String.format("%s -- %s ;\n", r.getName(), ppn));
            clusterMap.put(ppn, String.format(" [cluster=\"%s\", label=\"%s\"];\n ", cluster, m.getPPN()));
        }
    }

    private DDCResolver getDDCResolver() {
        if (ddcResolver == null) {
            ddcResolver = new DDCResolver();
        }
        return ddcResolver;
    }

}
