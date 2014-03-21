package de.unigoettingen.sub;

import de.unigoettingen.sub.medas.client.MeDASClient;
import de.unigoettingen.sub.medas.model.Doc;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdo on 3/21/14.
 */
public class MeDASReader {

    private MeDASClient meDAS;

    public MeDASReader(){
        meDAS = new MeDASClient();
    }

    /**
     * Get all documents from the MeDAS service.
     *
     * @return
     */
    public Set<Doc> getMETsFromMeDAS() {
        HashSet<Doc> mets = new HashSet<>();

        Set<Doc> allDocs = meDAS.getAllDocuments().getDocs();
        for (Doc doc : allDocs) {
            if (doc.getDDC() != null) { //FIXME remove again. What should be done with unclassified documents?
                mets.add(doc);
            }
        }
        return mets;
    }

    void setMeDASClient(MeDASClient client){
       meDAS = client;
    }
}
