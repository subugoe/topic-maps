package de.unigoettingen.sub;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import de.unigoettingen.sub.model.Doc;
import de.unigoettingen.sub.model.Docs;
import javax.ws.rs.core.MediaType;


/**
 *
 * @author jdo
 */
public class MeDASClient {
    private Client client;
//    private final String BASE_URI = "http://10.0.2.206:8080/";
    private final String BASE_URI = "http://10.0.3.83:6789/medas/";

    public MeDASClient(){
        client = Client.create();
    }
        
    public void getSingleDoc(String id){
        String path = "documents/"+id;
        WebResource webResource = client.resource(BASE_URI);
        webResource = webResource.path(path);
        Doc response = (Doc) webResource.accept(MediaType.APPLICATION_JSON).get(Doc.class);
        System.out.println("doc " + response.getDocid());
        System.out.println("title " + response.getTitle());
        System.out.println("related "+response.getRelatedItems().size() );
        System.out.println("host " + response.getRelatedItems().iterator().next().getRecordIdentifier());
        System.out.println("ddc "  + response.getClassifications().iterator().next().getValue());        
    }
    
    public Docs getAllDocuments(){
        String path = "documents/";
        WebResource webResource = client.resource(BASE_URI);
        webResource = webResource.path(path);        
        Docs response = (Docs) webResource.accept(MediaType.APPLICATION_JSON).get(Docs.class);
        
        return response;
        
    }
    public static void main(String[] args){
        MeDASClient app = new MeDASClient();
        Docs all = app.getAllDocuments();
        System.out.println("all " + all.getDocs().size());
    }
}
