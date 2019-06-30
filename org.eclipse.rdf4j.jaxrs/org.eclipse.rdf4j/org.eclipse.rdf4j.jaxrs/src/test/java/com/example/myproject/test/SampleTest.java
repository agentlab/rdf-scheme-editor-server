package com.example.myproject.test;

import javax.servlet.http.HttpServletRequest;

import org.apache.karaf.itests.KarafTestSupport;
import org.eclipse.rdf4j.http.server.repository.RepositoryConfigController;
import org.eclipse.rdf4j.http.server.repository.TransactionControllerGet;
import org.eclipse.rdf4j.http.server.repository.TransactionControllerSize;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.portable.OutputStream;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
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
    public void getHelloService() throws Exception {
        // installing a feature and verifying that it's correctly installed
        installAndAssertFeature("scr");

        //installAndAssertFeature("org.eclipse.rdf4j.jaxrs");

        // testing a command execution
        String bundles = executeCommand("bundle:list -t 0");
        System.out.println(bundles);
        assertContains("junit", bundles);

        String features = executeCommand("feature:list -i");
        System.out.print(features);
        assertContains("scr", features);

        // using a service and assert state or result
        RepositoryManager manager = getOsgiService(RepositoryManager.class);
        assertNotNull(manager);
        System.out.println("Size=" + manager.getAllRepositories().size());
        System.out.println("Location=" + manager.getLocation());
    }

    @Test
    public void getChanges() throws IOException {
        RepositoryConfigController rcc = getOsgiService(RepositoryConfigController.class);
        String repId = "id128";
        String context = "%3Cfile://C:/fakepath/example.xml%3E";
        String actionGet = "GET";
        String actionUpdate = "UPDATE";
        String actionAdd = "ADD";
        String bodyBefore = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<rdf:RDF\n" +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "xmlns:si=\"https://www.w3schools.com/rdf/\">\n" +
                "\n" +
                "<rdf:Description rdf:about=\"https://www.w3schools.com\">\n" +
                " <si:title>W3Schools</si:title>\n" +
                " <si:author>Jan Egil Refsnes</si:author>\n" +
                "</rdf:Description>\n" +
                "\n" +
                "</rdf:RDF>";
        String bodyAfter = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<rdf:RDF\n" +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "xmlns:si=\"https://www.w3schools.com/rdf/\">\n" +
                "\n" +
                "<rdf:Description rdf:about=\"https://www.w3schools.com\">\n" +
                " <si:title>548</si:title>\n" +
                " <si:author>q</si:author>\n" +
                "</rdf:Description>\n" +
                "\n" +
                "</rdf:RDF>";

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
        //for (Repository repository : manager.getAllRepositories()) System.out.println(repository);
        //manager.init();
        Repository repository = manager.getRepository("id128");
        RepositoryConnection repositoryConnection = repository.getConnection();

        repositoryConnection.begin();


        TransactionControllerGet controllerGet = new TransactionControllerGet();
        try {
            controllerGet.handleRequestInternal(null, null, "id128", "id322");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Транзакция не удалась");
        }


        ActiveTransactionRegistry.INSTANCE.register("1234",repositoryConnection);
        //TransactionControllerGet controller = new TransactionControllerGet();

        //HttpServletRequest req = mock(HttpServletRequest.class);
        //when(req.getParameter("action")).thenReturn(null);

        String restLocation = "http://localhost:8181/rdf4j2-server/repositories/rpo13/transactions/1234";

        String addparam = URLEncoder.encode("action=ADD", "UTF-8");
        URL urlAdd = null;
        try {
            urlAdd = new URL(restLocation+"?" + addparam);
        } catch (MalformedURLException e) {
            fail("Транзакция не удалась");
            e.printStackTrace();
        }
        //добавили данные
        HttpURLConnection connectionAdd = (HttpURLConnection) urlAdd.openConnection();
        try {
            connectionAdd.setRequestMethod("PUT");
            connectionAdd.setDoOutput(true);
            OutputStream os = connectionAdd.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(bodyBefore);
            System.out.println(connectionAdd.getResponseCode());
            System.out.println(connectionAdd.getResponseMessage());
            osw.flush();
            osw.close();
            if (connectionAdd.getResponseCode() != 200) {
                System.out.println("Транзакция не успешна");
            }
            connectionAdd.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Транзакция не удалась");
            repositoryConnection.close();
        }

        String updateparam = URLEncoder.encode("action=UPDATE", "UTF-8");
        URL urlUpdate = null;
        try {
            urlUpdate = new URL(restLocation+"?" + updateparam);
        } catch (MalformedURLException e) {
            fail("Транзакция не удалась");
            e.printStackTrace();
        }
        //обновили данные
        HttpURLConnection connectionUpdate = (HttpURLConnection) urlUpdate.openConnection();
        try {
            connectionUpdate.setRequestMethod("PUT");
            connectionUpdate.setDoOutput(true);
            OutputStream os = connectionAdd.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(bodyAfter);
            System.out.println(connectionAdd.getResponseCode());
            System.out.println(connectionAdd.getResponseMessage());
            osw.flush();
            osw.close();
            if (connectionUpdate.getResponseCode() != 200) {
                System.out.println("Транзакция не успешна");
            }
            connectionUpdate.disconnect();


        } catch (Exception e) {
            e.printStackTrace();
            fail("Транзакция не удалась");
            repositoryConnection.close();
        }

        String getparam = URLEncoder.encode("action=GET", "UTF-8");
        URL urlGet = null;
        try {
            urlGet = new URL(restLocation+"?" + getparam);
        } catch (MalformedURLException e) {
            fail("Транзакция не удалась");
            e.printStackTrace();
        }
        //проверка
        HttpURLConnection connectionCheck = (HttpURLConnection) urlGet.openConnection();
        try {
            connectionCheck.setRequestMethod("PUT");

            connectionCheck.connect();
            if (connectionCheck.getResponseCode() != 200) {
                System.out.println("Транзакция не успешна");
            } else {
                //проверка, что получено измененное
				InputStream in = connectionCheck.getInputStream();
				String encoding = connectionCheck.getContentEncoding();
				encoding = encoding == null ? "UTF-8" : encoding;
				String bodyAfterResponse = IOUtils.toString(in, encoding);
                assertTrue(bodyAfterResponse.contains(bodyAfter));
            }
            connectionCheck.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Транзакция не удалась");
            repositoryConnection.close();
        }
        repositoryConnection.close();
    }
}