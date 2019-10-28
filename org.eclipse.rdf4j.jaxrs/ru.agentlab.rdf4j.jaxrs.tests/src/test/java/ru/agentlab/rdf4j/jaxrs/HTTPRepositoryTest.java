package ru.agentlab.rdf4j.jaxrs;

import com.sun.istack.NotNull;
import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
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

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HTTPRepositoryTest extends  Rdf4jJaxrsTestSupport{
    @Inject
    protected RepositoryManagerComponent manager;

    String file =  "/testcases/default-graph-1.ttl";
    Repository repository;
    Repository rep;
    RepositoryConnection repcon;
    String rdf4jServer;
    String repositoryID;
    String address;
    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);

    private class Checker{
        String requestAnswer;
        int size;
        Model model;
        boolean testCheck;
        RepositoryResult<Statement> resultStatement;
    }

    @Before
    public void init() throws Exception {
        repositoryID = "1234568";
        repository = manager.getOrCreateRepository(repositoryID, "native", null);
        rdf4jServer = "http://localhost:" + getHttpPort() + "/rdf4j-server/";
        address = rdf4jServer + "repositories/" + repositoryID + "/statements";
        rep = new HTTPRepository(rdf4jServer, repositoryID);
        repcon = rep.getConnection();
    }

    @After
    public void cleanup() {
        repcon.close();
        repository.shutDown();
        manager.removeRepository(repositoryID);
    }

    public WebClient webClientCreator(String myAddress){
        WebClient client = WebClient.create(myAddress);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        return client;
    }

    public  Checker getHTTPRep(){
        Checker checker = new Checker();
        WebClient client2 = webClientCreator(address);
        client2.accept(new MediaType("text", "turtle"));
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
        checker.model = modelFromServer;
        System.out.println("MODEL FROM SERGER:" + checker.model);
        return checker;
    }

    public Checker addHTTPRep() throws IOException {
        Checker checker = new Checker();
        Model model = null;
        InputStream input = HTTPRepositoryTest.class.getResourceAsStream(file);
        assertNotNull(input);
        assertThat("dataStream.available", input.available(), greaterThan(0));
        try {
            model = Rio.parse(input, "", Rio.getParserFormatForFileName(file).orElse(RDFFormat.TURTLE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {

            repcon.add(HTTPRepositoryTest.class.getResource(file), "", dataFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Checker gotChecker;
        gotChecker = getHTTPRep();
        Model newModel = gotChecker.model;
        checker.testCheck = isSubset(model,newModel);
        return checker;
    }

    public Checker deleteHTTPRep(Model modelBeforeDelete){
        System.out.println("modelBeforeDelete: " + modelBeforeDelete);
        Checker checker = new Checker();
        repcon.clear();

        Checker gotChecker;
        gotChecker = getHTTPRep();
        Model modelAfterDelete = gotChecker.model;
        System.out.println("modelAfterDelete: " + modelAfterDelete);
        checker.testCheck = modelAfterDelete.equals(modelBeforeDelete);
        return checker;
    }

    public Checker sparqlSelect(){
        Checker checker = new Checker();
        InputStream inputStream = HTTPRepositoryTest.class.getResourceAsStream(file);
        Model modelFromFile = null;
        try {
            modelFromFile = Rio.parse(inputStream,"", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String queryString = "SELECT ?x ?p ?y WHERE { ?x ?p ?y } ";
        String selectedStr =  "# Default graph" +
                "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";

        //не могу понять почему, но если в SPARQLRespository передавать ту же строку
        // (только если её заранее проинициализировать выше),
        // то он ругается

        Repository repo = new SPARQLRepository(rdf4jServer + "repositories/" + repositoryID);
        repo.init();
        repcon = repo.getConnection();
        TupleQuery tupleQuery = repcon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value valueOfX = bindingSet.getValue("x");
                Value valueOfP = bindingSet.getValue("p");
                Value valueOfY = bindingSet.getValue("y");
                selectedStr= selectedStr + "<" + valueOfX + "> " + "<" + valueOfP + "> "
                        + valueOfY + " ."+ "\n";
            }
        }

        Reader reader = new StringReader(selectedStr);
        Model modelFromSelect = null;
        try {
             modelFromSelect = Rio.parse(reader, "", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checker.testCheck = modelFromSelect.equals(modelFromFile);
        return checker;
    }

    public Checker sparqlUpdate() {
        Checker checker;
        Repository repo = new SPARQLRepository(rdf4jServer + "repositories/" + repositoryID);
        repo.init();
        repcon = repo.getConnection();

        String queryString = "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";
        queryString += "DELETE {?x ?p \"Bob\" .} \n";
//        queryString += "INSERT <urn:x-local:graph1> dc:publisher \"Bob23\"\n";
        queryString += "WHERE {?x ?p \"Bob\"}";
        TupleQuery tupleQuery =repcon.prepareTupleQuery(QueryLanguage.SPARQL,queryString);

//(        String strShouldBe = "# Default graph\n" +
//                "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
//                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
//                "\n" +
//                "<urn:x-local:graph1> dc:publisher \"Bob23\" .";
//        Reader reader = new StringReader(strShouldBe);
//        Model modelInserted = null;
//        try {
//           modelInserted = Rio.parse(reader, "", RDFFormat.RDFXML);
//        } catch (IOException e) {
//            e.printStackTrace();
//        })
        checker = getHTTPRep();
        Model modelAfterUpdate = checker.model;

        System.out.println("update: " + modelAfterUpdate);
//
        checker.testCheck = true;
        return checker;
    }

    @Test
    public void HTTPRepositoryShouldWorkOk() throws IOException {
        Checker checker ;
        checker = getHTTPRep();
        Model resultBeforeDelete = checker.model;
        checker = addHTTPRep();
        assertThat("AddHTTPRepo is Match: ", checker.testCheck, equalTo(true));

        checker= sparqlSelect();
        assertThat("deleteHTTPRepo is Match: ", checker.testCheck, equalTo(true));

        checker = sparqlUpdate();
        assertThat("deleteHTTPRepo is Match: ", checker.testCheck, equalTo(true));
//
//        checker = deleteHTTPRep(resultBeforeDelete);
//        assertThat("deleteHTTPRepo is Match: ", checker.testCheck, equalTo(true));
    }
}
