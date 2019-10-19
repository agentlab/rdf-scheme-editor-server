package ru.agentlab.rdf4j.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
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
import org.hamcrest.core.IsNot;
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
public class TransactionsControllerTest extends Rdf4jJaxrsTestSupport {
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
        address = ENDPOINT_ADDRESS + repId + "/transactions";
        repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        repositoryCon = repository.getConnection();
    }
    
    @After
    public void cleanup() {
        repositoryCon.close();
    }

    @Test
    public void addDataItTransactionShouldWork() throws IOException {
        String location = createTransaction();
        assertThat(location, notNullValue());
    }
    
    protected String createTransaction() {
        String address = ENDPOINT_ADDRESS + repId + "/transactions";
        
        WebClient client = WebClient.create(address);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        Response response = client.post(null);
        System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
        System.out.println("response.body=" + response.readEntity(String.class));
        assertEquals(204, response.getStatus());
        client.close();
        
        return response.getHeaderString("Location");
    }
}
