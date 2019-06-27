package org.eclipse.rdf4j.http.server.repository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component (service = GraphComponent.class, property = { "osgi.jaxrs.resource=true" })
@Path ("/rdf4j-server")
public class GraphComponent {
	@Reference
    private RepositoryManager repositoryManager;

    @Reference
    RepositoryConfigController rcc;
	
	
	public GraphComponent() {
		System.out.println("Started");
	}
		
	
	
	private void createRepConfig(RepositoryConfigController rcc, String repId) throws RDF4JException, IOException {
		ConfigTemplate ct = rcc.getConfigTemplate("native");
		System.out.println("ConfigTemplate: " + ct);
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("Repository ID", repId);
		String strConfTemplate = ct.render(queryParams);
		System.out.println("ConfigTemplate render: " + strConfTemplate);
		RepositoryConfig rc = rcc.updateRepositoryConfig(strConfTemplate);
		System.out.println("RepositoryConfig.id: " + rc.getID());
		System.out.println("RepositoryConfig: " + rc.toString());
	}
	
	
	public final StreamingOutput outputMethod(Repository repository, StreamingOutput fileStream, Resource[] graph) {
		
		fileStream =  new StreamingOutput()
        {
            @Override
            public void write(java.io.OutputStream output) throws IOException, WebApplicationException
            {
                try
                {
        			RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, output);
        			
        			repository.getConnection().exportStatements(null, null, null, true, writer, graph);
        			System.out.println ("Statements получены"); 
                }
                catch (Exception e)
                {
                    throw new WebApplicationException("File Not Found !!");
                }
            }
        };
        return fileStream;
	}

	@GET
	@Path("/repositories/{repId}/rdf-graphs/{graphName}")	
	public Response getGraph(@PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri) throws IOException {
		System.out.println("GET.Name activated");
		System.out.println("repId = " + repId);
		System.out.println("graphName = " + graphName);
		
		Repository repository = null;
		StreamingOutput outStream = null;
		StreamingOutput fileStream = null;
		
		try {
			repository = repositoryManager.getRepository(repId);
		}
		catch (RDF4JException e) {
			System.out.println ("NOPE!");
			//e.printStackTrace();
		}
		
		if (repository != null) {
			try { 
			System.out.println ("Репозиторий найден");
			ValueFactory vf = repository.getValueFactory();
			
			//@Context UriInfo uri;
			String myUri = uri.getBaseUri().toString();
			//IRI IRIgraph = vf.createIRI(graphName); 
			IRI IRIgraph = vf.createIRI(myUri);  //??????????
			Resource[] graph = new Resource[] { IRIgraph };
			//RDFHandler response = null;
			
			//FileOutputStream out = new FileOutputStream ("C:/rdf/file.rdf");
			
			
			
			/**RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, out);
			
			repository.getConnection().exportStatements(null, null, null, true, writer, graph);
			System.out.println ("Statements получены"); } **/
			
			
			fileStream = outputMethod(repository, outStream, graph);
			}
			catch (Exception e) {
				System.out.println ("NOPE! v2.0");
				e.printStackTrace();
			}
		}
		else {
			System.out.println ("Репозиторий не найден");
			createRepConfig(rcc, repId);
			try {
				repository = repositoryManager.getRepository(repId);
				repository.init();
				System.out.println ("Репозиторий создан");
			}
			catch (Exception e) {
				System.out.println ("NOPE! v2.0");
				e.printStackTrace();
			}
		}
		return Response
                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition","attachment; filename = myfile.pdf")
                .build();
		//createRepConfig(rcc, graphName);
	}
	
	//......
		
		
		@DELETE
		@Path("/repositories/{repId}/rdf-graphs/{graphName}")	
		public void deleteGraph(@PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri) throws IOException {
			System.out.println("GET.Name activated");
			System.out.println("repId = " + repId);
			System.out.println("graphName = " + graphName);
			
			Repository repository = null;
			
			try {
				repository = repositoryManager.getRepository(repId);
			}
			catch (RDF4JException e) {
				System.out.println ("NOPE!");
				//e.printStackTrace();
			}
			
			if (repository != null) {
				try { 
				System.out.println ("Репозиторий найден");
				ValueFactory vf = repository.getValueFactory();
				
				//@Context UriInfo uri;
				String myUri = uri.getBaseUri().toString();
				//IRI IRIgraph = vf.createIRI(graphName); 
				IRI IRIgraph = vf.createIRI(myUri);  //??????????
				Resource[] graph = new Resource[] { IRIgraph };
				//RDFHandler response = null;
				
				repository.getConnection().clear(graph);
				System.out.println ("Удалено"); }
				catch (Exception e) {
					System.out.println ("NOPE! v2.0");
					e.printStackTrace();
				}
			}
			else {
				System.out.println ("Репозиторий не найден");
				createRepConfig(rcc, repId);
				try {
					repository = repositoryManager.getRepository(repId);
					repository.init();
					System.out.println ("Репозиторий создан");
				}
				catch (Exception e) {
					System.out.println ("NOPE! v2.0");
					e.printStackTrace();
				}
			}
		
	};
	
	

}
