package de.unigoettingen.sub;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by jdo on 3/27/14.
 */
@Path("/api")
public class RestInterface {
    @GET
    @Produces("text/plain")
    @Path("{regionName}")
    public String getIDsForRegion(@PathParam("regionName") String regionName){
        TopicMapCreator tm = TopicMapCreator.getInstance();
        Region region = tm.getRegionWithName(regionName);
        return "hello " + tm.getIDsForRegion(region);

    }
}
