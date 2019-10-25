package ru.agentlab.rdf4j.jaxrs;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jws.WebParam;
import javax.swing.text.html.parser.Entity;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HTTPRepositoryTest {

    String file =  "/testcases/default-graph-1.ttl";

    Repository rep;
    RepositoryConnection repcon;
    private class Checker{
        String requestAnswer;
        int size;
        boolean testCheck;
        RepositoryResult<Statement> resultStatement;
    }

    @Before
    public void init(){
        rep = new SailRepository( new MemoryStore());
        rep.init();
        repcon = rep.getConnection();
    }

    @After
    public void cleanup() {
        rep.shutDown();
    }

    public  Checker getHTTPRep(){
        Checker checker = new Checker();
        checker.resultStatement = repcon.getStatements(null,null,null);

        return checker;
    }

    public Checker addHTTPRep(){
        Checker checker = new Checker();
        Model model = null;
        InputStream input = HTTPRepositoryTest.class.getResourceAsStream(file);
        try {
            model = Rio.parse(input, "", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        repcon.add(model);

        Checker gotChecker = new Checker();
        gotChecker = getHTTPRep();
        RepositoryResult<Statement> result2 = gotChecker.resultStatement;
        Model newModel = QueryResults.asModel(result2);

        checker.testCheck = isSubset(model,newModel);
        return checker;
    }

    public Checker deleteHTTPRep( RepositoryResult<Statement> beforeDelete){
        Model modelBeforeDelete = QueryResults.asModel(beforeDelete);
        Checker checker = new Checker();
        repcon.clear(null, null,null);

        Checker gotChecker = new Checker();
        gotChecker = getHTTPRep();
        RepositoryResult<Statement> result = gotChecker.resultStatement ;
        Model modelAfterDelete = QueryResults.asModel(result);

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
        Checker checker = new Checker();
        String queryString = "DELETE ?x ?p \"Bob\"\n";
        queryString += "INSERT <urn:x-local:graph1> dc:publisher \"Bob23\"\n";
        queryString += "WHERE {?x ?p \"Bob\"}";
        TupleQuery tupleQuery =repcon.prepareTupleQuery(QueryLanguage.SPARQL,queryString);

        String strShouldBe = "(urn:x-local:graph1, http://purl.org/dc/elements/1.1/publisher, \"Bob23\") [null]";

        checker = getHTTPRep();
        RepositoryResult<Statement> afterUpdate = checker.resultStatement;
        while (afterUpdate.hasNext()){
            if((afterUpdate + "") == strShouldBe){
                checker.testCheck = true;
            }
        }
        return checker;
    }

    @Test
    public void HTTPRepositoryShouldWorkOk(){
        Checker checker ;
        checker = getHTTPRep();
        RepositoryResult<Statement>  emptyResult = checker.resultStatement;

        checker = addHTTPRep();
        assertThat("AddHTTPRepo is Match: ", checker.testCheck, equalTo(true));

        checker= sparqlSelect();
        assertThat("deleteHTTPRepo is Match: ", checker.testCheck, equalTo(true));

//        checker = sparqlUpdate();
//        assertThat("deleteHTTPRepo is Match: ", checker.testCheck, equalTo(true));

        checker = deleteHTTPRep(emptyResult);
        assertThat("deleteHTTPRepo is Match: ", checker.testCheck, equalTo(true));
//        assertThat("deleteHTTPRepo length is zero: ", checker.size, equalTo(0));

    }

}
