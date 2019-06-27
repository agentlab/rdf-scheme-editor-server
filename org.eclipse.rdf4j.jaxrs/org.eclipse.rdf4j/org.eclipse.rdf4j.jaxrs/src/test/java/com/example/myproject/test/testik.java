package com.example.myproject.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import jdk.nashorn.internal.ir.annotations.Reference;
import org.eclipse.rdf4j.http.server.repository.RepositoryConfigController;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.http.server.transaction.TransactionQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.karaf.itests.KarafTestSupport;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SampleTest extends KarafTestSupport {

    //@Inject
    //private RepositoryManager manager;

    @Override
    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz");

        String httpPort = "8181";
        String rmiRegistryPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        String sshPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));
        String localRepository = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        if (localRepository == null) {
            localRepository = "";
        }

        return new Option[] {
                //KarafDistributionOption.debugConfiguration("8889", true),
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                // configureConsole().ignoreLocalConsole(),
                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                mavenBundle().groupId("org.awaitility").artifactId("awaitility").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
                mavenBundle().groupId("org.apache.karaf.itests").artifactId("common").versionAsInProject(),
                //    mavenBundle().groupId("org.mockito").artifactId("mockito-core").version("2.23.4"),
                features(maven().groupId("ru.agentlab.rdf4j.server")
                        .artifactId("ru.agentlab.rdf4j.server.features").type("xml")
                        .version("0.0.1-SNAPSHOT"), "org.eclipse.rdf4j.jaxrs"),
                junitBundles(),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", localRepository)
        };
    }


    @Test
    public void makequery() throws IOException {
        executeCommand("bundle:dynamic-import 75");
        executeCommand("bundle:dynamic-import 175");
        RepositoryConfigController rcc = getOsgiService(TransactionQuery.class);
        String repId = "id127";
        ConfigTemplate ct = rcc.getConfigTemplate("memory");
        System.out.println("ConfigTemplate: " + ct);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Repository ID", repId);
        String strConfTemplate = ct.render(queryParams);
        System.out.println("ConfigTemplate render: " + strConfTemplate);
        System.out.println("------------------------------");
        System.out.println(strConfTemplate);
        RepositoryConfig rc = rcc.updateRepositoryConfig(strConfTemplate);
		
        RepositoryManager manager = getOsgiService(RepositoryManager.class);
        Repository repository = manager.getRepository("id127");
        RepositoryConnection repositoryConnection = repository.getConnection();

        repositoryConnection.begin();
		
		//добавили транзакцию
        ActiveTransactionRegistry.INSTANCE.register("4321",repositoryConnection);
        TransactionQuery controller = new TransactionQuery();
		
		
		String actionGet = "GET";
        String actionUpdate = "UPDATE";
        String actionAdd = "ADD";
        String bodyBefor = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<rdf:RDF\n" +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "xmlns:dc=\"http://ld-r.org/config/facetproperty_creator_adms3\"\n" +
                "\n" +
                "<rdf:Description rdf:about=\"http://example/book1/\">\n" +
				"<facetproperty_creator_adms3title xmlns=\"http://ld-r.org/config/\">103</facetproperty_creator_adms3title>\n" +
				"</rdf:Description>" +
                "\n" +
                "</rdf:RDF>";		
		
        String restLocation = "http://localhost:8181/rdf4j2-server/repositories/rpo13/transactions/4321";
        URL url = null;
        try {
            url = new URL(restLocation);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
		
		
		//добавили данные
        HttpURLConnection connectionAdd = (HttpURLConnection) url.openConnection();
        try {
            connectionAdd.setRequestMethod("PUT");
            connectionAdd.setRequestAction(actionAdd);
            connectionAdd.setRequestBody(bodyBefor);
            connectionAdd.setRequestContext(context);
            connectionAdd.connect();
            if (connectionAdd.getResponseCode() != 200) {
                System.out.println("Транзакция не успешна");
            }
            connectionAdd.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            repositoryConnection.close();
        }
		
		
		//query
		//SELECT ?s ?p ?o WHERE {<http://example/book1/> <http://exampleSub> 103 . ?s ?p ?o . }
		query = "queryLn=SPARQL&query=PREFIX%20dc%3A%20%3Chttp%3A%2F%2Fld-r.org%2Fconfig%2Ffacetproperty_creator_adms"+
					"3%3E%0A%0ASELECT%20%3Fs%20%3Fp%20%3Fo%0AWHERE%20%7B%20%0A%20%20%20%20%3Chttp%3A%2F%2FexampleSub%3E%20%"+
					"3Chttp%3A%2F%2FexampleSub%3E%2070%20.%20%3Fs%20%3Fp%20%3Fo%20.%0A%7D&infer=true"
		connection.SetRequestBody(query)
		connection.SetRequestMethod("PUT")
		response.SetRequestMethod("GET")
		HttpServletRequest connection = (HttpServletRequest) url.openConnection();
		HttpServletRequest response = (HttpServletRequest) url.openConnection();
        try {
            controller.handleRequestInternal(connection, response, "rpo13", "4321");
            connection.disconnect();
			data = response.toString()
			data.contains("<http://example/book1/> <http://exampleSub> 103")
        } catch (Exception e) {
            e.printStackTrace();
            fail("Транзакция не успешна");
			repositoryConnection.close();
        }
        try {
            ActiveTransactionRegistry.INSTANCE.deregister("4321");
        } catch (Exception e){
            assertEquals(e, new RepositoryException());
        }
		
        connection.disconnect();
		response.disconnect();
        repositoryConnection.close();
    }
}