package ru.agentlab.rdf4j.jaxrs;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

import java.io.*;
import java.util.HashMap;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import ru.agentlab.rdf4j.jaxrs.repository.RepositoryController;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class StatementsControllerTest extends Rdf4jJaxrsTestSupport {
    String ENDPOINT_ADDRESS;
    String address;
    String file = "/testcases/default-graph-1.ttl";
    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);

    String repId = "id1237";
    Repository repository;
    RepositoryConnection repositoryCon;

    @Before
    public void init() throws Exception {
        ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
        address = ENDPOINT_ADDRESS + repId + "/statements";
        repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        repositoryCon = repository.getConnection();
    }

    @After
    public void cleanup() {
        repositoryCon.close();
    }

    public WebClient webClientCreator(){
        WebClient client = WebClient.create(address);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);

        return client;
    }

    public Model getAllStatemnts(){
        WebClient client2 = webClientCreator();
        Response response2 = client2.get();
        String gotString = response2.readEntity(String.class);

        assertEquals(200, response2.getStatus());
        Reader reader = new StringReader(gotString);
        Model modelFromServer = null;
        try {
            modelFromServer = Rio.parse(reader,"", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        client2.close();

        return modelFromServer;

    }

    public void postStatement(){
        WebClient client = webClientCreator();
        InputStream dataStream = RepositoryControllerTest.class.getResourceAsStream(file);
        assertNotNull(dataStream);
        try {
            assertThat("dataStream.available", dataStream.available(), greaterThan(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response response = client.post(dataStream);
        assertEquals(204, response.getStatus());
        assertEquals("", response.readEntity(String.class));
        client.close();

        assertThat("repositoryCon.size", repositoryCon.size(), equalTo(4L));
    }

    public void isSatementSubset(){

        InputStream dataStream2 = RepositoryControllerTest.class.getResourceAsStream(file);
        Model modelFromFile = null;
        try {
            modelFromFile = Rio.parse(dataStream2,"", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Model modelFromServer = getAllStatemnts();
        assertTrue(isSubset(modelFromFile,modelFromServer));
    }

    public void deletAllStatements(Model modelBeforeDelete){
        WebClient clientDeleter = webClientCreator();
        Response responseForDelete = clientDeleter.delete();

        assertEquals(204, responseForDelete.getStatus());
        clientDeleter.close();

        Model modelAfterDelete = getAllStatemnts();

        assertEquals(modelAfterDelete,modelBeforeDelete);


    }

    @Test
    public void postStatementsShouldWorkOk() throws IOException {
        Model modelBeforeDelete = getAllStatemnts();

        postStatement();

        isSatementSubset();

        deletAllStatements(modelBeforeDelete);




    }
}
