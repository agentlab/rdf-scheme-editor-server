package ru.agentlab.rdf4j.jaxrs;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphControllerTest extends Rdf4jJaxrsTestSupport {
    private String address;
    private String file = "/testcases/default-graph-1.ttl";
    private RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);

    private String repId = "id1238";
    private String graphName = "graph1";
    private RepositoryConnection repositoryCon;

    @Before
    public void init() throws Exception {
        String ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
        String graphSection = "/rdf-graphs/";
        address = ENDPOINT_ADDRESS + repId + graphSection + graphName;
        Repository repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        repositoryCon = repository.getConnection();
    }

    @After
    public void cleanup() {
        repositoryCon.close();
    }

    @Test
    public void stage1_postStatementsToGraphShouldWorkOk() throws IOException {
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
        assertThat("repositoryCon.size", repositoryCon.size(), equalTo(4L));
    }

    @Test
    public void stage2_shouldGetGraphByRestAPI() throws IOException {
        System.out.println("GET statements from named graph on address=" + address);
        WebClient client = WebClient.create(address);
        client.accept(MediaType.WILDCARD);
        Response response = client.get();
        String body = response.readEntity(String.class);
        Reader reader = new StringReader(body);
        Model modelFromGet = Rio.parse(reader, "", RDFFormat.RDFXML);
        InputStream inputStream = RepositoryControllerTest.class.getResourceAsStream(file);
        Model modelFromFile = Rio.parse(inputStream, "", dataFormat);

        assertThat("File model is subset of GET model", Models.isSubset(modelFromFile, modelFromGet));

        System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
        System.out.println("response.body=" + response.readEntity(String.class));
        assertEquals(200, response.getStatus());
        client.close();
    }

    @Test
    public void stage3_shouldDeleteGraphFromRepository() {
        System.out.println("DELETE \"" + graphName + "\" from repository\"" + repId + "\" on address=" + address);
        WebClient client = WebClient.create(address);
        client.accept(MediaType.WILDCARD);
        Response response = client.delete();
        assertThat("repositoryCon.size", repositoryCon.size(), equalTo(0L));
        System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
        System.out.println("response.body=" + response.readEntity(String.class));
        assertEquals(204, response.getStatus());
        client.close();
    }
}
