package de.unigoettingen.sub;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author jdo
 */
public class DDCResolver {
    private static Properties props;
    private final File PROPS_FILE = new File(System.getProperty("java.io.tmpdir"), "ddcLabels.properties");
    public DDCResolver(){
        super();
        if (props == null){
            try {
                props = new Properties(null);
                props.load(new FileReader(PROPS_FILE));
            } catch (IOException ex) {
                Logger.getLogger(DDCResolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public String getLabelForClass(String ddcClass){
        if (!props.containsKey(ddcClass)){
            System.out.println("new label for " + ddcClass);
            try {
                String label = fetchLabelForClass(ddcClass);
                if (label != null){
                    System.out.println("got " + label);
                    props.setProperty(ddcClass, label);
                    
                    FileWriter fw = new FileWriter(PROPS_FILE);
                    props.store(fw, label);
                    fw.close();                    
                }
                return label;
            } catch (IOException ex) {
                Logger.getLogger(DDCResolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return props.getProperty(ddcClass);
    }
    private String fetchLabelForClass(String ddcClass) throws IOException {
        
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(String.format("http://dewey.info/class/%s/about.de", ddcClass));
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        try {            

            HttpEntity entity1 = response1.getEntity();
            String label = getContent(entity1.getContent());
            EntityUtils.consume(entity1);
            return label;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DDCResolver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(DDCResolver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(DDCResolver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            response1.close();
        }
        return null;
    }

    private String getContent(InputStream is) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);
        // dbf.setCoalescing(true);
        // dbf.setExpandEntityReferences(true);

        DocumentBuilder db = null;
        db = dbf.newDocumentBuilder();
//      db.setEntityResolver(new NullResolver());
        // db.setErrorHandler( new MyErrorHandler());
        Document xml = db.parse(is);
//        try {
//            printDocument(xml, System.out);
//        } catch (TransformerException ex) {
//            Logger.getLogger(DDCResolver.class.getName()).log(Level.SEVERE, null, ex);
//        }

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
//XPathExpression expr = xpath.compile("/html/body/div/a");
//        XPathExpression expr = xpath.compile("//rdf:Description[2]/skos:prefLabel[1]");
        XPathExpression expr = xpath.compile("(//*[local-name()='prefLabel'])[2]");
        String label = expr.evaluate(xml);
        return label;
    }

    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }

    public static void main(String[] args) throws IOException {
        DDCResolver app = new DDCResolver();
        app.getLabelForClass("540");
    }
}
