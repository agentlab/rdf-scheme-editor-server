package ru.agentlab.rdf4j.jaxrs;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class) @ExamReactorStrategy(PerClass.class) @FixMethodOrder(MethodSorters.NAME_ASCENDING) public class GraphControllerTest extends Rdf4jJaxrsTestSupport {
    private String address;
    private String firstTTL = "/testcases/default-graph-1.ttl";
    private String secondTTL = "/testcases/default-graph-2.ttl";
    private RDFFormat dataFormat = Rio.getParserFormatForFileName(firstTTL).orElse(RDFFormat.RDFXML);

    private String repId = "id1238";
    private String graphNameFirst = "graph1";
    private String graphNameSecond = "graph2";
    private RepositoryConnection repositoryCon;
    private ModelBuilder builder = new ModelBuilder();

    @Before public void init() throws Exception {
        String ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
        String graphSection = "/rdf-graphs/";
        address = ENDPOINT_ADDRESS + repId + graphSection;
        Repository repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        repositoryCon = repository.getConnection();
    }

    @After public void cleanup() {
        repositoryCon.close();
    }

    @Test public void shouldAddGetDeleteGraph() throws Exception {
        //Добавляем тестовый трипл в graph1
        System.out.println("POST statements to graph on address=" + address + graphNameFirst);
        WebClient clientForFirstGraph = WebClient.create(address + graphNameFirst);
        clientForFirstGraph.type(dataFormat.getDefaultMIMEType());
        clientForFirstGraph.accept(MediaType.WILDCARD);
        InputStream dataStream1 = RepositoryControllerTest.class.getResourceAsStream(firstTTL);
        assertNotNull(dataStream1);
        assertThat("dataStream.available", dataStream1.available(), greaterThan(0));
        Response responseFirst = clientForFirstGraph.post(dataStream1);
        System.out.println("response.status=" + responseFirst.getStatusInfo().getReasonPhrase());
        System.out.println("response.body=" + responseFirst.readEntity(String.class));
        assertEquals(204, responseFirst.getStatus());
        //assertThat("repositoryCon.size", repositoryCon.size(), equalTo(4L));

        //Добавляем другой трипл в graph2
        System.out.println("POST statements to graph on address=" + address + graphNameSecond);
        WebClient clientForSecondGraph = WebClient.create(address + graphNameSecond);
        clientForSecondGraph.type(dataFormat.getDefaultMIMEType());
        clientForSecondGraph.accept(MediaType.WILDCARD);
        InputStream dataStream2 = RepositoryControllerTest.class.getResourceAsStream(secondTTL);
        Response responseSecond = clientForSecondGraph.post(dataStream2);

        //Проверяем, что данные в графах различны

        System.out.println("GET statements from graph1 on address=" + address + graphNameFirst);
        Response responseFromGetFirst = clientForFirstGraph.get();
        String bodyFirst = responseFromGetFirst.readEntity(String.class);
        System.out.println("BODY FROM FIRST GET:\n" + bodyFirst);
        Reader readerFirst = new StringReader(bodyFirst);
        Model modelFromGetOnFirstGraph = Rio.parse(readerFirst, "", RDFFormat.RDFXML);
        InputStream inputStreamForFirstFile = RepositoryControllerTest.class.getResourceAsStream(firstTTL);
        Model modelFromFirstFile = Rio.parse(inputStreamForFirstFile, "", dataFormat);

        //Дополнительная проверка на то, что данные в граф добавлены согласно default-graph-1.ttl
        assertThat("File model is subset of GET model", Models.isSubset(modelFromFirstFile, modelFromGetOnFirstGraph));

        System.out.println("GET statements from graph2 on address=" + address + graphNameFirst);
        Response responseFromGetSecond = clientForSecondGraph.get();
        String bodySecond = responseFromGetSecond.readEntity(String.class);
        System.out.println("BODY FROM SECOND GET:\n" + bodySecond);
        Reader readerSecond = new StringReader(bodyFirst);
        Model modelFromGetOnSecondGraph = Rio.parse(readerSecond, "", RDFFormat.RDFXML);
        InputStream inputStreamForSecondFile = RepositoryControllerTest.class.getResourceAsStream(firstTTL);
        Model modelFromSecondFile = Rio.parse(inputStreamForSecondFile, "", dataFormat);

        //Получили модели из двух разных графов, далее проверяем, что они не идентичны (не сабсеты друг для друга)
        Assert.assertNotEquals(modelFromGetOnFirstGraph, modelFromGetOnSecondGraph);
        //assertThat("Get from first and second graph is different", !Models.isSubset(modelFromGetOnFirstGraph, modelFromGetOnSecondGraph));


        /*
        Тут лежит феч для всех триплов из дефолтного графа, чтобы посмотреть,
        как они попадают в дефолтный граф из всех named графов

        WebClient webClient = WebClient.create("http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/" + repId + "/rdf-graphs/service?default");
        Response fetchAllStatements = webClient.get();
        System.out.println("fetchAllStatements from default graph body:" + fetchAllStatements.readEntity(String.class));
*/

        //Тут удаляем один граф и проверяем, не пропадают ли триплы из других графов
        System.out.println("DELETE \"" + graphNameFirst + "\" from repository\"" + repId + "\" on address=" + address);
        clientForFirstGraph = WebClient.create(address);
        clientForFirstGraph.accept(MediaType.WILDCARD);
        Response response3 = clientForFirstGraph.delete();
        assertThat("repositoryCon.size", repositoryCon.size(), equalTo(0L));
        System.out.println("response.status=" + response3.getStatusInfo().getReasonPhrase());
        System.out.println("response.body=" + response3.readEntity(String.class));
        //Удалили трипл из первого графа, проверяем, что второй в сохранности
        Response responseFromGetSecondForDelete = clientForSecondGraph.get();
        String bodySecondAfterDeletingFirstGraph = responseFromGetSecond.readEntity(String.class);
        Reader readerSecondAfterDeleting = new StringReader(bodyFirst);
        Model modelFromGetOnSecondGraphAfterDeleting = Rio.parse(readerSecond, "", RDFFormat.RDFXML);

        Assert.assertEquals(modelFromGetOnSecondGraph, modelFromGetOnSecondGraphAfterDeleting);
        assertEquals(204, response3.getStatus());
        clientForFirstGraph.close();
    }
}
