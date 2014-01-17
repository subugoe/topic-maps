package de.ungoettingen.sub;

/**
 *
 * @author doenitz@sub.uni-goettingen.de
 */
public class Mets {
    
    
    private String PPN;
    private String host;
    private String ddcNumber;

    public String getPPN() {
        return PPN;
    }

    public void setPPN(String PPN) {
        this.PPN = PPN;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDdcNumber() {
        return ddcNumber;
    }

    public void setDdcNumber(String ddcNumber) {
        this.ddcNumber = ddcNumber;
    }
    public String toString(){
        return PPN;
    }       
}
