package com.example.myproject.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.http.server.repository.RepositoryConfigController;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.http.server.transaction.TransactionControllerUpdate;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

        return new Option[]{
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
                //  mavenBundle().groupId("org.mockito").artifactId("mockito-core").version("2.23.4"),
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
    public void update() throws IOException {

        executeCommand("bundle:dynamic-import 75");
        executeCommand("bundle:dynamic-import 175");

        String repId = "id128";
        String transactionId = "1234";
        String context = "%3Cfile://C:/fakepath/example.xml%3E";
        String actionGet = "GET";
        String actionAdd = "ADD";
        String body = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<rdf:RDF\n" +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "xmlns:si=\"https://www.w3schools.com/rdf/\">\n" +
                "\n" +
                "<rdf:Description rdf:about=\"https://www.w3schools.com\">\n" +
                " <si:title>BMSTU</si:title>\n" +
                " <si:author>Ivanov</si:author>\n" +
                "</rdf:Description>\n" +
                "\n" +
                "</rdf:RDF>";

        RepositoryConfigController rcc = getOsgiService(RepositoryConfigController.class);

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
        Repository repository = manager.getRepository(repId);
        RepositoryConnection repositoryConnection = repository.getConnection();

        repositoryConnection.begin();
        //добавили транзакцию
        ActiveTransactionRegistry.INSTANCE.register(transactionId, repositoryConnection);
        //TransactionControllerUpdate controller = new TransactionControllerUpdate();

        String restLocation = "http://localhost:8181/rdf4j2-server/repositories/rpo13/transactions/1234";
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
            connectionAdd.setRequestBody(body);
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
        //откатываем транзакцию
        HttpURLConnection connectionRollback = (HttpURLConnection) url.openConnection();
        try {
            connectionRollback.setRequestMethod("DELETE");
            connectionRollback.connect();
            if (connectionRollback.getResponseCode() != 201) {
                System.out.println("Транзакция не откатана");
            }
            connectionRollback.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            repositoryConnection.close();
        }
        //проверка
        HttpURLConnection connectionCheck = (HttpURLConnection) url.openConnection();
        try {
            connectionCheck.setRequestMethod("PUT");
            connectionCheck.setRequestAction(actionGet);

            connectionCheck.connect();
            if (connectionCheck.getResponseCode() != 200) {
                System.out.println("Транзакция не успешна");
            } else {
                //ожидаем что полученный равен нулю
                assertEquals(connectionCheck.getResponse().toString, "");
            }
            connectionCheck.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            repositoryConnection.close();
            fail();
        }
        repositoryConnection.close();
    }
}