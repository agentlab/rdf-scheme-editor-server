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
        repository = manager.getOrCreateRepository(repId, "memory", null);
        repositoryCon = repository.getConnection();
    }

    @After
    public void cleanup() {
        repositoryCon.close();
    }

    @Test
    public void postStatementsShouldWorkOk() throws IOException {

        System.out.println("Statement POST Test ***********************");
        /**
         *  TEST :Post statement to repository
         **********************************
         */
        WebClient client = WebClient.create(address);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        InputStream dataStream = RepositoryControllerTest.class.getResourceAsStream(file);
        assertNotNull(dataStream);
        assertThat("dataStream.available", dataStream.available(), greaterThan(0));
        Response response = client.post(dataStream);
        assertEquals(204, response.getStatus());
        assertEquals("", response.readEntity(String.class));
        client.close();

        assertThat("repositoryCon.size", repositoryCon.size(), equalTo(4L));

        System.out.println("Statement Subset Test ***********************");
        /**
         * / TEST checks is our post statement Subset of Statements at the server
         * *********************************************
         */
        WebClient client2 = WebClient.create(address);
        client2.type(dataFormat.getDefaultMIMEType());
        client2.accept(MediaType.WILDCARD);
        Response response2 = client2.get();

        InputStream dataStream2 = RepositoryControllerTest.class.getResourceAsStream(file);
        Model modelFromFile = Rio.parse(dataStream2,"",RDFFormat.TURTLE);

        String gotString = response2.readEntity(String.class);
        assertEquals(200, response2.getStatus());
        System.out.println("Response.getStatus():" + response2.getStatus());

        Reader reader = new StringReader(gotString);
        Model modelFromServer = Rio.parse(reader,"", RDFFormat.TURTLE);

        //isSubset
        assertTrue(isSubset(modelFromFile,modelFromServer));
        System.out.println("OurFileIsSubset: " + isSubset(modelFromFile, modelFromServer));

        client2.close();


        System.out.println("Statement Delete Test ***********************");
        /**
         * **************************
        Here begins (all) Statements delete tests
         *****************************
         **/
        //webClient Delete all
        WebClient clientDeleter = WebClient.create(address);
        clientDeleter.type(dataFormat.getDefaultMIMEType());
        clientDeleter.accept(MediaType.WILDCARD);
        Response responseForDelete = clientDeleter.delete();

        assertEquals(204, responseForDelete.getStatus());
        clientDeleter.close();

        //Client check for deleting status

        WebClient clientChecker = WebClient.create(address);
        clientChecker.type(dataFormat.getDefaultMIMEType());
        clientChecker.accept(MediaType.WILDCARD);
        Response responseDeleteCheck = clientChecker.get();


        boolean isEmptyRep = responseDeleteCheck.readEntity(String.class).isEmpty();
        assertTrue(isEmptyRep);
        System.out.println("String after delete " + responseDeleteCheck.readEntity(String.class));
        System.out.println("Out Reppository is empty: " + isEmptyRep);

        clientChecker.close();



    }
}
