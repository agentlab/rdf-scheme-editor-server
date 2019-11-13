package ru.agentlab.rdf4j.jaxrs;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;



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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExamParameterized;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ru.agentlab.rdf4j.jaxrs.repository.transaction.Transaction;
import ru.agentlab.rdf4j.jaxrs.repository.transaction.TransactionController;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@RunWith(PaxExamParameterized.class)
@ExamReactorStrategy(PerClass.class)
public class TransactionsControllerTest extends Rdf4jJaxrsTestSupport2 {

    @Inject
    protected RepositoryManagerComponent manager;

    private String ENDPOINT_ADDRESS;
    private String address;
    private String addressGetStatements;
    final private String file = "/testcases/default-graph-1.ttl";
    final private String file2 = "/testcases/default-graph.ttl";
    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
    
    String repId;
    final String ACTION = "action=";
    String COMMIT = "COMMIT";
    final String ADD = "ADD";
    final String GET = "GET";
    final String DELETE = "DELETE";
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
//        repId = "rashid";
        ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
//        ENDPOINT_ADDRESS = "https://agentlab.ru/" +"rdf4j-server"+ "/repositories/";
        address = ENDPOINT_ADDRESS + repId + "/transactions";
        addressGetStatements = ENDPOINT_ADDRESS + repId + "/statements";
        repository = manager.getOrCreateRepository(repId, testType, null);
        repositoryCon = repository.getConnection();
    }
    
    @After
    public void cleanup() {
        cleanreapository();
        repositoryCon.close();
        repository.shutDown();
        manager.removeRepository(repId);
    }

    public void cleanreapository(){
        WebClient client = webClientCreator(addressGetStatements);
        Response response = client.delete();
        client.close();
    }

    public WebClient webClientCreator(String myAddress){
        WebClient client = WebClient.create(myAddress);
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
        WebClient client = webClientCreator(addressGetStatements);
        client.accept(new MediaType("text", "turtle"));
        Response response = client.get();
        String gotString = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        Model modelFromServer = modelCreator(gotString,"",RDFFormat.TURTLE);
        client.close();
        return modelFromServer;
    }

    synchronized String createTransaction() {
        WebClient client = webClientCreator(address);
        System.out.println("addddd"  + address);
        Response response = client.post(null);
        assertEquals(204, response.getStatus());
        client.close();
        return response.getHeaderString("Location");
    }

    protected void addToTransaction(String transAddress, String fileAdd){
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        WebClient client = webClientCreator(transAddress + "?" + ACTION + ADD);
        Response response = client.post(inputStream);
        assertThat("addToTransactionError:", response.getStatus(), equalTo(200));
        client.close();
    }

    protected String getDataFromTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + GET);
        Response response = client.put(null);
        client.close();
        return  response.readEntity(String.class);
    }

    protected Response commitTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + COMMIT);
        Response response = client.post(null);
        client.close();
        return response;
    }

    protected void deleteDataInTransAction(String transAddress, String myFile){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + DELETE);
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(myFile);
        Response response  = client.post(inputStream);
        assertThat("delteDataInTransactionError:", response.getStatus(), equalTo(200));
        client.close();
    }

    protected Response deleteTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress);
        Response response = client.delete();
        client.close();
        return response;
    }


    @Test
    public void commitingTransactionShouldWorkOK() throws IOException {
        String transAddress = createTransaction();
        addToTransaction( transAddress, file);
        Response response = commitTransaction(transAddress);
        assertThat("commitTransactionError:in commitingTransactionShouldWorkOK", response.getStatus(), equalTo(200));
        Model modelFormServer = getStatementsFromServer();
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        Model modelFormFile = modelCreator(inputStream,"",RDFFormat.TURTLE);
        assertThat("commitTransactionsShouldWorkOk:", isSubset(modelFormFile, modelFormServer), equalTo(true));
    }

    @Test
    public void clearBeforeCommitingShouldAddNoChange(){
        Model modelBeforeAction = getStatementsFromServer();
        String transAddress = createTransaction();
        System.out.println("transaddr " + transAddress);
        addToTransaction(transAddress,file);
        deleteDataInTransAction(transAddress,file);
        Response response = commitTransaction(transAddress);
        assertThat("commitTransactionError:in clearBeforeCommitingShouldAddNoChange", response.getStatus(), equalTo(200));
        Model modelAfterAction = getStatementsFromServer();
        assertThat("clearBeforeCommitingShouldAddNoChange: ", modelAfterAction.equals(modelBeforeAction), equalTo(true));
    }

    @Test
    public void tryDeleteAfterCommit(){
        String transAddress = createTransaction();
        addToTransaction(transAddress, file);
        Response responseCommit = commitTransaction(transAddress);
        assertThat("commitTransactionError:in tryDeleteAfterCommit", responseCommit.getStatus(), equalTo(200    ));
        Response response = deleteTransaction(transAddress);
        assertThat("tryDeleteAfterCommit ", response.getStatus(), equalTo(500));
    }

    @Test
    public void commitAfterRollbackShouldGetError(){
        Model modelBeforeAction =getStatementsFromServer();
        String transAddress = createTransaction();
        addToTransaction(transAddress,file);
        deleteTransaction(transAddress);
        Response response  = commitTransaction(transAddress);
        assertThat("commitTransactionError:in commitAfterRollbackShouldGetError()", response.getStatus(), equalTo(500));
        Model modelAfterAction = getStatementsFromServer();
        assertThat("commitAfterRollbackShouldGetError(): do not any changes", modelAfterAction.equals(modelBeforeAction), equalTo(true));
    }

    @Test
    public void addTwoTransactionCommitOneShouldWorkOK(){
        String transAddress = createTransaction();
        String transAddressSecond = createTransaction();
        addToTransaction(transAddress, file);
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        Model modelFromFirstFile = modelCreator(inputStream, "", RDFFormat.TURTLE);

        addToTransaction(transAddressSecond,file2);
        InputStream inputStream2 = TransactionsControllerTest.class.getResourceAsStream(file2);
        Model modelFromSecondFile = modelCreator(inputStream2, "", RDFFormat.TURTLE);

        Response response = deleteTransaction(transAddressSecond);
        assertThat("addTwoTransactionCommitOneShouldWorkOK: delete", response.getStatus(), equalTo(204));
        commitTransaction(transAddress);
        Response responseAfterDelete = commitTransaction(transAddressSecond);
        assertThat("commitTransactionError:in commitAfterRollbackShouldGetError()", responseAfterDelete.getStatus(), equalTo(500));
        Model modelAfterAction = getStatementsFromServer();
        assertThat("addTwoTransactionCommitOneShouldWorkOK: file should be",
                isSubset(modelFromFirstFile,modelAfterAction), equalTo(true));
        assertThat("addTwoTransactionCommitOneShouldWorkOK:file should not be",
                isSubset(modelFromSecondFile,modelAfterAction), equalTo(false));


    }
    

}
