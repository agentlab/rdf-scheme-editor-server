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
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.ws.rs.core.Response.ResponseBuilder;
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
	@Path("/repositories/import/{fileName}")
	@Consumes({"*/*"})
	public Response importDocuments(@Context UriInfo uriInfo,
			@PathParam("fileName") String fileName,
			InputStream input) throws WebApplicationException, IOException{
		System.out.println("Handle data");		
		Pattern pattern = Pattern.compile(".+\\.(docx|csv)");
		Matcher matcher = pattern.matcher(fileName);
		boolean found = matcher.matches();
		if (found) 
		{
			try {
				System.out.println("Import data");
				int read = 0;
				byte[] bytes = new byte[input.available()]; 
			    try
			    {  
			    	 File file = new File("./" + fileName);
			    	 if(!file.exists()) 
			    	 {
			    		 if (file.createNewFile()) {
					         FileOutputStream out = new FileOutputStream(file);  
					         while ((read = input.read(bytes)) != -1) {  
					             out.write(bytes, 0, read);  
					         }  
					         out.flush();  
					         out.close(); 
			    		 }
			    		 getDirectoryAndFiles();
			    		 return Response.created(uriInfo.getAbsolutePath()).build();
			    	 }
			    	 else
			    	 {
			    		 System.out.println("File exists. Set a different file name");
			    		 return Response.status(409).build();
			    	 }
			    }
			    catch (IOException e) {return Response.serverError().build();}
			} 
			catch (Exception e) {return Response.serverError().build();}
		}
		else {return Response.status(415).build();}		
	}

 	@GET
	@Path("/repositories/import/{fileName}")
	@Produces("*/*")
	public Response getFile(@PathParam("fileName") String fileName) {

         File file = new File("./" + fileName);
        if (file.exists()) {
	        ResponseBuilder response = Response.ok((Object) file);
	        response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
	        return response.build();	
        }
        else { return Response.status(204).build(); }
    }

 	private void getDirectoryAndFiles() {
		try {
			File dir = new File("./");
	        // если объект представляет каталог
	        if(dir.isDirectory())
	        {
	            // получаем все вложенные объекты в каталоге
	            for(File item : dir.listFiles()){	              
	                 if(item.isDirectory()){

 	                     System.out.println(item.getName() + "  \t folder");
	                 }
	                 else{

 	                     System.out.println(item.getName() + "\t file");
	                 }
	             }
	        }
        }
		catch(Exception e) {System.out.println("Error! Directory not exist");}
	}

 }