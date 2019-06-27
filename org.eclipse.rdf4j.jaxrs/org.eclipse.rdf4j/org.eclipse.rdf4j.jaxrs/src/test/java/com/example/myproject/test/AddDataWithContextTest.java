package com.example.myproject.test;

import org.apache.karaf.itests.KarafTestSupport;
import org.eclipse.rdf4j.http.server.repository.StatementsComponent;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotEquals;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AddDataWithContextTest extends KarafTestSupport {

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
    public void TestCreateRepo() {
        StatementsComponent component = getOsgiService(StatementsComponent.class);
        RepositoryManager manager = getOsgiService(RepositoryManager.class);
        Repository repository = manager.getRepository("id322322");
        RepositoryConnection connection = repository.getConnection();

        String bodyBefore = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<rdf:RDF\n" +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "xmlns:si=\"https://www.w3schools.com/rdf/\">\n" +
                "\n" +
                "<rdf:Description rdf:about=\"https://www.w3schools.com\">\n" +
                "  <si:title>W3Schools</si:title>\n" +
                "  <si:author>Jan Egil Refsnes</si:author>\n" +
                "</rdf:Description>\n" +
                "\n" +
                "</rdf:RDF>";
        String contextBefore = "%3Cfile://C:/fakepath/example.xml%3E";



        try {
            component.method(null, "id322322", contextBefore, bodyBefore);
        } catch (IOException e) {
            e.printStackTrace();
        }

        connection.begin();
        long sizeBefore = connection.size();
        System.out.println(sizeBefore);
        String bodyAfter = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<rdf:RDF\n" +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "xmlns:si=\"https://www.w3schools.com/rdf/\">\n" +
                "\n" +
                "<rdf:Description rdf:about=\"https://www.w3schools.com\">\n" +
                "  <si:title>W3Schools</si:title>\n" +
                "  <si:author>Jan Egil Refsnes</si:author>\n" +
                "</rdf:Description>\n" +
                "\n" +
                "</rdf:RDF>";
        String contextAfter = "%3Cfile://C:/fakepath/exampleANOTHERandBIGGER.xml%3E";


        try {
            component.method(null, "id322322", contextAfter, bodyAfter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long sizeAfter = connection.size();
        connection.close();
        System.out.println(sizeAfter);
        assertNotEquals(sizeBefore, sizeAfter);
    }
}