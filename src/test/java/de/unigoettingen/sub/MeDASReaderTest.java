package de.unigoettingen.sub;

import de.unigoettingen.sub.medas.client.MeDASClient;
import de.unigoettingen.sub.medas.model.Doc;
import de.unigoettingen.sub.medas.model.Docs;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Created by jdo on 3/21/14.
 */
public class MeDASReaderTest {


    @Test
    public void testGetMeDASClient() {
        Doc doc1 = mock(Doc.class);
        when(doc1.getDDC()).thenReturn("400 Arts");

        MeDASClient mockClient = mock(MeDASClient.class);
        Docs docs = new Docs();
        docs.addDocs(doc1);
        docs.addDocs(new Doc());
        docs.addDocs(null);
        when(mockClient.getAllDocuments()).thenReturn(docs);

        MeDASReader reader = new MeDASReader();
        reader.setMeDASClient(mockClient);
        assertEquals(1, reader.getMETsFromMeDAS().size());

        verify(mockClient, times(1)).getAllDocuments();
    }


}