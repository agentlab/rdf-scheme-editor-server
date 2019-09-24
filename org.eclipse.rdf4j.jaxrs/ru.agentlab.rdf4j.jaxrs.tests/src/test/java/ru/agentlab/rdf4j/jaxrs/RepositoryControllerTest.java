package ru.agentlab.rdf4j.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.karaf.itests.KarafTestSupport;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Constants;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class RepositoryControllerTest extends KarafTestSupport {
	String ENDPOINT_ADDRESS;

	 @Inject
	 protected RepositoryManagerComponent manager;

    @Override
    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz");

        String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        String rmiRegistryPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        String sshPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));
        String localRepository = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        if (localRepository == null) {
            localRepository = "";
        }

        return new Option[] {
            // enable for remote debugging
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
            features(maven().groupId("ru.agentlab.rdf4j")
            	.artifactId("ru.agentlab.rdf4j.features").type("xml")
            	.version("0.0.1-SNAPSHOT"), "ru.agentlab.rdf4j.jaxrs"),
            //mavenBundle().groupId("org.mockito").artifactId("mockito-core").version("2.23.4"),
            junitBundles(),
            editConfigurationFilePut("etc/org.apache.felix.http.cfg", "org.osgi.service.http.port", httpPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
            editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", localRepository)
        };
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        System.out.println("TestProbeBuilder gets called");
        //probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        probe.setHeader(Constants.IMPORT_PACKAGE, "org.eclipse.rdf4j.query.algebra.evaluation.impl,org.apache.cxf.jaxrs.client");
        return probe;
    }

    @Before
    public void init() throws Exception {
        ENDPOINT_ADDRESS = "http://localhost:" + getHttpPort() + "/rdf4j-server/repositories/";
    }

    @Override
    public String getHttpPort() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.apache.felix.http", null);
        if (configuration != null) {
            return configuration.getProperties().get("org.osgi.service.http.port").toString();
        }
        return "8181";
    }


    @Test
    public void createQuryAndDeleteNativeRepository_withRepositoryManagerComponent_ShouldWork() throws Exception {
        // testing a command execution
        String bundles = executeCommand("bundle:list -t 0");
        System.out.println(bundles);
        assertContains("junit", bundles);

        String features = executeCommand("feature:list -i");
        System.out.print(features);
        assertContains("scr", features);

        // using a service and assert state or result
        //RepositoryManagerComponent manager = getOsgiService(RepositoryManagerComponent.class);
        assertNotNull(manager);
        //System.out.println("Size=" + manager.getAllRepositories().size());
        //System.out.println("Location=" + manager.getLocation());

        String repId = "id1233";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));
        RepositoryConnection conn = repository.getConnection();

        ModelBuilder builder = new ModelBuilder();
		Model model = builder
			.setNamespace("ex", "http://example.org/")
			.namedGraph("http://www.google.com")
			.subject("ex:Picasso")
				.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
				.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
			.build();
		conn.add(model);

		// We do a simple SPARQL SELECT-query that retrieves all resources of type `ex:Artist`,
		// and their first names.
		String queryString = "PREFIX ex: <http://example.org/> \n";
		queryString += "PREFIX foaf: <" + FOAF.NAMESPACE + "> \n";
		queryString += "SELECT ?s ?n \n";
		queryString += "WHERE { \n";
		queryString += "    ?s a ex:Artist; \n";
		queryString += "       foaf:firstName ?n .";
		queryString += "}";
		TupleQuery query = conn.prepareTupleQuery(queryString);
		// A QueryResult is also an AutoCloseable resource, so make sure it gets closed when done.
		try (TupleQueryResult result = query.evaluate()) {
			// we just iterate over all solutions in the result...
			BindingSet bs = result.next();
			assertEquals("http://example.org/Picasso", bs.getValue("s").stringValue());
			assertEquals("Pablo", bs.getValue("n").stringValue());
			assertFalse(result.hasNext());

			//while (((TupleQueryResult)queryResult).hasNext()) {
			//	BindingSet solution = ((TupleQueryResult)queryResult).next();
			//	solution.forEach(b -> {
			//		System.out.println(b.getName() + "=" + b.getValue().stringValue());
			//	});
			//}
		}
		conn.close();
        manager.removeRepository(repId);
        assertNull(manager.getRepositoryInfo(repId));
    }

	@Test
	public void createAndDeleteNativeRepository_withRestApi_ShouldWork() throws IOException {
		String repId = "id1234";
		String address = ENDPOINT_ADDRESS + repId;

		ConfigTemplate ct = RepositoryManagerComponent.getConfigTemplate("native");
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("Repository ID", repId);
		String strConfTemplate = ct.render(queryParams);
		//System.out.println("ConfigTemplate render: " + strConfTemplate);

		//prereq check
		assertNull(manager.getRepositoryInfo(repId));

		WebClient client = WebClient.create(address);
		client.type("text/turtle");
		client.accept(MediaType.WILDCARD);
		System.out.println("PUT on address=" + address);
		Response response = client.put(strConfTemplate);
		System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
		System.out.println("response.body=" + response.readEntity(String.class));
		assertEquals(204, response.getStatus());
		assertNotNull(manager.getRepositoryInfo(repId));
		client.close();

		WebClient client2 = WebClient.create(address);
		client2.accept(MediaType.WILDCARD);
		System.out.println("DELETE on address=" + address);
		Response response2 = client2.delete();
		System.out.println("response2.status=" + response2.getStatusInfo().getReasonPhrase());
		System.out.println("response2.body=" + response2.readEntity(String.class));
		assertEquals(204, response2.getStatus());
		assertNull(manager.getRepositoryInfo(repId));
		client2.close();
	}

	@Test
	public void reCreateAndDeleteNativeRepository_withRestApi_ShouldWork2ndTime() throws IOException {
		createAndDeleteNativeRepository_withRestApi_ShouldWork();
	}

	@Test
	public void queryShouldWorkOk() throws IOException {
		String repId = "id1235";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));

        RepositoryConnection conn = repository.getConnection();
        ModelBuilder builder = new ModelBuilder();
		Model model = builder
			.setNamespace("ex", "http://example.org/")
			.namedGraph("http://www.google.com")
			.subject("ex:Picasso")
				.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
				.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
			.build();
		conn.add(model);
		conn.close();

		String address = ENDPOINT_ADDRESS + repId;
		WebClient client = WebClient.create(address);
		client.type("application/sparql-query");
		client.accept("application/sparql-results+json");
		System.out.println("POST on address=" + address);
		Response response = client.post("select ?s ?p ?o where {?s ?p ?o}");
		System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
		System.out.println("response.body=" + response.readEntity(String.class));
		assertEquals(200, response.getStatus());
		client.close();
	}

    @Test
	public void postStatementsShouldWorkOk() throws IOException {
		String repId = "id1237";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));

		String address = ENDPOINT_ADDRESS + repId + "/statements";
		WebClient client = WebClient.create(address);

		String file = "/testcases/default-graph-1.ttl";
		RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
		client.type(dataFormat.getDefaultMIMEType());
		client.accept(MediaType.WILDCARD);

		InputStream dataStream = RepositoryControllerTest.class.getResourceAsStream(file);
		assertNotNull(dataStream);
		assertThat("dataStream.available", dataStream.available(), greaterThan(0));

		System.out.println("POST on address=" + address);
		Response response = client.post(dataStream);
		System.out.println("response.status=" + response.getStatusInfo().getReasonPhrase());
		System.out.println("response.body=" + response.readEntity(String.class));
		assertEquals(204, response.getStatus());
		client.close();

		RepositoryConnection repositoryCon = repository.getConnection();
		assertThat("repositoryCon.size", repositoryCon.size(), equalTo(4L));
	}
    
    @Test
    public void postStatementsToGraphShouldWorkOk() throws IOException {
        String repId = "id1238";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));

        String address = ENDPOINT_ADDRESS + repId + "/rdf-graphs/graph1";
        String file = "/testcases/default-graph-1.ttl";
        RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
        
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
