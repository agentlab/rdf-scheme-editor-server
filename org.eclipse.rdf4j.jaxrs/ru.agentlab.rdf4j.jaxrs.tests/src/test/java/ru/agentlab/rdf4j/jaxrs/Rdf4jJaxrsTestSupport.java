package ru.agentlab.rdf4j.jaxrs;

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
import java.net.ServerSocket;

import org.apache.karaf.itests.KarafTestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Constants;

public class Rdf4jJaxrsTestSupport extends KarafTestSupport {

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
            // uncomment if you need to debug (blocks test execution and waits for the debugger)
            //KarafDistributionOption.debugConfiguration("5005", true),
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
            // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
            configureSecurity().disableKarafMBeanServerBuilder(),
            // configureConsole().ignoreLocalConsole(),
            keepRuntimeFolder(),
            logLevel(LogLevelOption.LogLevel.INFO),
            mavenBundle().groupId("org.awaitility").artifactId("awaitility").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(), mavenBundle().groupId("org.apache.karaf.itests").artifactId("common").versionAsInProject(),
            features(maven().groupId("ru.agentlab.rdf4j").artifactId("ru.agentlab.rdf4j.features").type("xml").version("0.0.1-SNAPSHOT"), "ru.agentlab.rdf4j.jaxrs"),
            // mavenBundle().groupId("org.mockito").artifactId("mockito-core").version("2.23.4"),
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
        // probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE,
        // "*,org.apache.felix.service.*;status=provisional");
        probe.setHeader(Constants.IMPORT_PACKAGE, "org.eclipse.rdf4j.query.algebra.evaluation.impl,org.apache.cxf.jaxrs.client");
        return probe;
    }

    @Override
    public String getHttpPort() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.apache.felix.http", null);
        if (configuration != null) {
            return configuration.getProperties().get("org.osgi.service.http.port").toString();
        }
        return "8181";
    }
    
    public static int getAvailablePort(int min, int max) {
        for (int i = min; i <= max; i++) {
            try (ServerSocket socket = new ServerSocket(i)) {
                System.out.println("Using port: " + i);
                return socket.getLocalPort();
            } catch (Exception e) {
                //System.err.println("Port " + i + " not available, trying next one");
                continue; // try next port
            }
        }
        throw new IllegalStateException("Can't find available network ports");
    }
}
