<<<<<<< HEAD
<<<<<<< HEAD
package com.example.myproject.test;
 
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
 
import javax.inject.Inject;

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
=======
package com.example.myproject.test;
 
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
 
import javax.inject.Inject;

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
>>>>>>> 4e002495842c02ff0066ad379e3d176ba774e857
=======
package com.example.myproject.test;
 
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
 
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.eclipse.rdf4j.http.server.repository.RepositoryConfigController;
import org.eclipse.rdf4j.http.server.repository.RepositoryController;
import org.eclipse.rdf4j.repository.Repository;

import static org.junit.Assert.assertNotNull;
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

import org.apache.karaf.itests.KarafTestSupport;
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
import junit.framework.Assert;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.io.File;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.apache.karaf.itests.KarafTestSupport;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SampleTest extends KarafTestSupport {
	

 
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
    public void TestCreateRepo() throws Exception{
    	//System.out.println(executeCommand("bundle:dynamic-import 175"));
    	HttpURLConnection connection = null;
    	RepositoryController repositoryController = getOsgiService(RepositoryController.class);
        RepositoryManager manager = getOsgiService(RepositoryManager.class);  
        
        
        for(String repo : manager.getRepositoryIDs()) System.out.println(repo);
    	  
          int size = manager.getAllRepositories().size();
          System.out.println(size);
          repositoryController.createRep("", "id3234", null);
//          URL url = new URL("http://localhost:8181/rdf4j2-server/repositories/id123");
//          HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
//          httpCon.setDoOutput(true);
//
//          httpCon.setRequestMethod("PUT");
//          OutputStreamWriter out = 	new OutputStreamWriter(
//          httpCon.getOutputStream());
//          out.write("Resource content");
//          out.close();
//          Scanner s = new Scanner(httpCon.getInputStream());
//
//          while (s.hasNextLine()) {
//          System.out.println(s.nextLine());
//          }
		  int sizeAfter = manager.getAllRepositories().size();
		  System.out.println(sizeAfter);
		  assertEquals(size, sizeAfter-1);
}
>>>>>>> master
}