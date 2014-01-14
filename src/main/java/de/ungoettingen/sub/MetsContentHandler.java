package de.ungoettingen.sub;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *
 * @author doenitz@sub.uni-goettingen.de
 */
public class MetsContentHandler implements ContentHandler {

    private String currentValue;
    private Pattern dcPattern = Pattern.compile("^([0-9]{3}).*");
    private boolean inHost = false;
    private Mets mets;
    

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentValue = new String(ch, start, length);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("mods:relatedItem")) {
            if (atts.getValue("type").equals("host")) {
                inHost = true;
            } else {
                inHost = false;
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equals("classification")) {
            extractDDCNumber();
        } else if (localName.equals("recordIdentifier")) {
            if (inHost) {
                mets.setHost(currentValue);
            } else {
                mets.setPPN(currentValue);
            }
        } else if (qName.equals("mods:relatedItem")) {
            inHost = false;
        }
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
        mets = new Mets();        
    }

    public void endDocument() throws SAXException {
        
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    private void extractDDCNumber() {
        Matcher match = dcPattern.matcher(currentValue);
        if (!match.matches()) {
            return;
        }
        mets.setDdcNumber(match.group(1));
    }

    public Mets getMets(){
        return mets;
    }    
}
