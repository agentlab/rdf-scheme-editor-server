package ru.agentlab.rdf4j.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
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

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GraphControllerTest extends Rdf4jJaxrsTestSupport {
    String ENDPOINT_ADDRESS;
    String address;
    String file = "/testcases/default-graph-1.ttl";
    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
    
    String repId = "id1238";
    Repository repository;
    RepositoryConnection repositoryCon;
    
    @Inject
    protected RepositoryManagerComponent manager;

    @Before
    public void init() throws Exception {
        ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
        address = ENDPOINT_ADDRESS + repId + "/rdf-graphs/graph1";
        repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        repositoryCon = repository.getConnection();
    }
    
    @After
    public void cleanup() {
        repositoryCon.close();
    }

    @Test
    public void postStatementsToGraphShouldWorkOk() throws IOException {
        System.out.println("POST statements to graph on address=" + address);
        WebClient client = WebClient.create(address);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        InputStream dataStream = RepositoryControllerTest.class.getResourceAsStream(file);
        assertNotNull(dataStream);
        assertThat("dataStream.available", dataStream.available(), greaterThan(0));
        Response response = client.post(dataStream);
        System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
        System.out.println("response.body=" + response.readEntity(String.class));
        assertEquals(204, response.getStatus());
        client.close();

        RepositoryConnection repositoryCon = repository.getConnection();
        assertThat("repositoryCon.size", repositoryCon.size(), equalTo(4L));
        
        System.out.println("GET statements from named graph on address=" + address);
        WebClient client2 = WebClient.create(address);
        client2.accept(MediaType.WILDCARD);
        Response response2 = client2.get();
        System.out.println("response2.status=" + response2.getStatusInfo().getReasonPhrase());
        System.out.println("response2.body=" + response2.readEntity(String.class));
        assertEquals(200, response2.getStatus());
        client2.close();
        
        /*String address = ENDPOINT_ADDRESS + repId + "/rdf-graphs/graph1";
        System.out.println("GET statements from default graph on address=" + address2);
        WebClient client3 = WebClient.create(address);
        client3.accept(MediaType.WILDCARD);
        Response response3 = client3.get();
        System.out.println("response3.status=" + response3.getStatusInfo().getReasonPhrase());
        System.out.println("response3.body=" + response3.readEntity(String.class));
        assertEquals(200, response3.getStatus());
        client3.close();*/
    }
}
