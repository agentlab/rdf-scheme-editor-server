package org.eclipse.rdf4j.http.server.repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RepositoryDataImport.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j2-server")
public class RepositoryDataImport {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
		
	public RepositoryDataImport() {
		System.out.println("Init RepositoryDataImport");
	}
	
	@PUT
	@Path("/repositories/import")
	@Consumes({"*/*"})
	public void importDocuments(InputStream input) throws WebApplicationException, IOException{
		System.out.println("Handle data");		
		
		String path = "/rdf4j2-server/repositories/import/";
		try {
			System.out.println("Import data");
			int read = 0;
			byte[] bytes = new byte[input.available()];
			String fileLocation = path + "test.docx";  
             //saving file  
	     try {  
	         FileOutputStream out = new FileOutputStream(new File(fileLocation));          
	         out = new FileOutputStream(new File(fileLocation));  
	         while ((read = input.read(bytes)) != -1) {  
	             out.write(bytes, 0, read);  
	         }  
	         out.flush();  
	         out.close();  
	     } catch (IOException e) {e.printStackTrace();}  
	     String output = "File successfully uploaded to : " + fileLocation;  
		} 
		catch (RDF4JException e) {
			logger.error("error while attempting to get template '", e);
			throw new WebApplicationException("error while attempting to get template: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		} 
	}
	
//	private void saveToFile(InputStream uploadedInputStream)
//	{
//	    try {
//	        OutputStream out = null;
//	        int read = 0;
//	        byte[] bytes = new byte[1024];
//	        
//	        out = new FileOutputStream(new File());
//	        while ((read = uploadedInputStream.read(bytes)) != -1) {
//	            out.write(bytes, 0, read);
//	        }
//	        out.flush();
//	        out.close();
//	    } catch (IOException e) {
//
//	        e.printStackTrace();
//	    }
//	}

}



