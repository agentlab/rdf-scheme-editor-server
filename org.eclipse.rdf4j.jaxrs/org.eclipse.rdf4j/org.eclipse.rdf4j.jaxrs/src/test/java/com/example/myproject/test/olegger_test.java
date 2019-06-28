package com.example.myproject.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import jdk.nashorn.internal.ir.annotations.Reference;
import org.eclipse.rdf4j.http.server.repository.RepositoryConfigController;
import org.eclipse.rdf4j.http.server.repository.RepositoryController;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.http.server.transaction.TransactionQuery;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.karaf.itests.KarafTestSupport;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class olegger_test extends KarafTestSupport {

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
    public void getQueryTest1() throws Exception {
        
        URL url = new URL("http://localhost:8181/rdf4j2-server/repositories/rpotest?query=ask%20{?s%20?p%20?o}");
        URLConnection conn = url.openConnection();
        
        InputStream input = conn.getInputStream();
        
        String inputStr = input.toString();
        
        if (inputStr != "true") {
        	fail("Транзакция не успешна");
        }
    	  
    }
    
    @Test
    public void getQueryTest2() throws Exception {
       
        URL url = new URL("http://localhost:8181/rdf4j2-server/repositories/rpotest?query=construct");
        URLConnection conn = url.openConnection();
        
        InputStream input = conn.getInputStream();
        
        String inputStr = input.toString();
        
        if (inputStr == "true") {
        	fail("Транзакция успешна");
        }   
        
    }
} 