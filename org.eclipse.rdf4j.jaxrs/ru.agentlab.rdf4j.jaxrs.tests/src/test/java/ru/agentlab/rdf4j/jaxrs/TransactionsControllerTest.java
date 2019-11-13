package ru.agentlab.rdf4j.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.hamcrest.core.IsNot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.internal.matchers.Equals;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TransactionsControllerTest extends Rdf4jJaxrsTestSupport2 {

    @Inject
    protected RepositoryManagerComponent manager;

    String ENDPOINT_ADDRESS;
    String address;
    String file = "/testcases/default-graph-1.ttl";
    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
    
    String repId;
    final protected String ACTION = "action=";
    String COMMIT = "COMMIT";
    String ADD = "ADD";
    String GET = "GET";
    String DELETE = "DELETE";
    Repository repository;
    RepositoryConnection repositoryCon;
    private final String testType;

    @Configuration
    public static Option[] config2() {
        return config();
    }

    @ProbeBuilder
    public static TestProbeBuilder probeConfiguration2(TestProbeBuilder probe) {
        return probeConfiguration(probe);
    }

    public TransactionsControllerTest(String typeTest){
        this.testType = typeTest;
    }
    @Parameterized.Parameters
    public static List<String[]> data(){
        return Arrays.asList(new String[][]{
            {"memory"},{"native"},{"native-rdfs"}
        });
    }

    @Before
    public void init() throws Exception {
        UUID uuid = UUID.randomUUID();
        repId = uuid.toString();
        ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
        address = ENDPOINT_ADDRESS + repId + "/transactions";
        repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        repositoryCon = repository.getConnection();
    }
    
    @After
    public void cleanup() {
        repositoryCon.close();
    }

    public WebClient webClientCreator(String myAddress){
        WebClient client = WebClient.create(address);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        return client;
    }

    public Model modelCreator(InputStream inputStream, String baseUrl, RDFFormat rdfFormat){
        Reader reader = new InputStreamReader(inputStream);
        Model model = null;
        try {
            model = Rio.parse(reader,baseUrl,rdfFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }
    public Model modelCreator(String string,String baseUrl,RDFFormat rdfFormat){
        Reader reader = new StringReader(string);
        Model model = null;
        try {
            model = Rio.parse(reader, baseUrl,rdfFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }
    public Model getStatementsFromServer(){
        WebClient client = webClientCreator(address);
        client.accept(new MediaType("text", "turtle"));
        Response response = client.get();
        String gotString = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        Model modelFromServer = modelCreator(gotString,"",RDFFormat.TURTLE);
        client.close();
        return modelFromServer;
    }

    protected String createTransaction() {
        WebClient client = webClientCreator(address);
        Response response = client.post(null);
        assertEquals(201, response.getStatus());
        client.close();
        return response.getHeaderString("Location");
    }

    protected void addToTransaction(String transId, String fileAdd){
        String thisAddress = address + "/" + transId + ACTION + ADD;
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(fileAdd);
        WebClient client = webClientCreator(thisAddress);
        Response response = client.post(inputStream);
        assertThat("addToTransactionError:", response.getStatus(), equalTo(204));
        client.close();
    }

    protected String getDataFromTransaction(String transId){
        String thisAddress = address + "/" + transId + ACTION + GET;
        WebClient client = webClientCreator(thisAddress);
        Response response = client.put(null);
        return  response.readEntity(String.class);
    }

    protected void commitTransaction(String transId){
        String thisAddress = address + "/" + transId + "?" + ACTION + COMMIT;
        WebClient client = webClientCreator(thisAddress);
        Response response = client.post(null);
        assertThat("commitTransactionError:", response.getStatus(), equalTo(204));
        
        client.close();
    }


    @Test
    public void commitingTransactionShouldWorkOK() throws IOException {
        String location = createTransaction();
        assertThat(location, notNullValue());
    }
    

}
