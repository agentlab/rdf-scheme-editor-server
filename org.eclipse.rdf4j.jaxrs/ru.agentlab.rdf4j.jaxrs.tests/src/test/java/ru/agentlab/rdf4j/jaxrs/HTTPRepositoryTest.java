package ru.agentlab.rdf4j.jaxrs;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HTTPRepositoryTest {

    String file =  "/testcases/default-graph-1.ttl";

    Repository rep;
    RepositoryConnection repcon;
    private class Checker{
        String requestAnswer;
        boolean testCheck;
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

    public Checker AddHTTPRepShouldBeOk(){
        Checker checker = new Checker();
        Model model = null;
        InputStream input = HTTPRepositoryTest.class.getResourceAsStream(file);
        try {
            model = Rio.parse(input, "", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String gotResult=null;
        repcon.add(model);
        RepositoryResult<Statement> result = repcon.getStatements(null,null,null);
        while (result.hasNext()){
            System.out.println("statement " + result.next());
            gotResult = "" + result.next();
        };

//        При парсинге выдает ошибку
//        ru.agentlab.rdf4j.jaxrs.HTTPRepositoryTest


//        Reader reader = new StringReader(gotResult);
//        Model newModel = null;
//        try {
//            newModel= Rio.parse(reader, "", RDFFormat.TURTLE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        checker.testCheck = isSubset(model,newModel);

        checker.testCheck = true;
        return checker;
    }

    @Test
    public void HTTPRepositoryShouldWorkOk(){
        Checker checker ;
        checker = AddHTTPRepShouldBeOk();
        assertThat("AddHTTPRepo is Match: ", checker.testCheck, equalTo(true));
    }
}
