package ru.agentlab.rdf4j.jaxrs.repository;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = GraphController.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j-server")
public class GraphController {
    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    public GraphController() {
        System.out.println("Started");
    }

    @Activate
    public void activate() {
        logger.info("Activate " + this.getClass().getSimpleName());
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivate " + this.getClass().getSimpleName());
    }

    @GET
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public Response getGraph(@PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri) throws IOException {
        System.out.println("GET.Name activated");
        System.out.println("repId = " + repId);
        System.out.println("graphName = " + graphName);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        StreamingOutput fileStream = null;

        try {
            ValueFactory vf = repository.getValueFactory();

            // @Context UriInfo uri;
            String myUri = uri.getBaseUri().toString();
            // IRI IRIgraph = vf.createIRI(graphName);
            IRI IRIgraph = vf.createIRI(myUri); // ??????????
            Resource[] graph = new Resource[] { IRIgraph };
            final Repository r = repository;

            fileStream = new StreamingOutput() {
                @Override
                public void write(java.io.OutputStream output) throws IOException, WebApplicationException {
                    try {
                        RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, output);

                        r.getConnection().exportStatements(null, null, null, true, writer, graph);
                        System.out.println("Statements получены");
                    } catch (Exception e) {
                        throw new WebApplicationException("File Not Found !!", NOT_FOUND);
                    }
                }
            };
        } catch (Exception e) {
            logger.error("Cannot get graph " + graphName + ". Internal error", e);
            throw new WebApplicationException("Cannot get graph " + graphName + ". Internal error", INTERNAL_SERVER_ERROR);
        }
        return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition", "attachment; filename = myfile.pdf").build();
    }

    public static String getFileContentAsString(String bodyName) {
        // for future used
        String content = "";
        try {
            URL fileURL = GraphController.class.getResource(bodyName);
            content = Resources.toString(fileURL, Charsets.UTF_8);
        } catch (IOException ex) {
            System.out.println("Error getting turle file");
            ex.printStackTrace();
        }
        return content;
    }

    public static List<Statement> getFileContentAsStatements(String bodyName, String baseURI) {
        List<Statement> statements = null;
        try {
            String content = bodyName;
            StringReader reader = new StringReader(content);
            Model model;
            model = Rio.parse(reader, baseURI, RDFFormat.TURTLE);
            Iterator<Statement> it = model.iterator();
            statements = Lists.newArrayList(it);
        } catch (IOException | RDFParseException | UnsupportedRDFormatException ex) {
            System.out.println("Error getting turtle file");
            ex.printStackTrace();
        }
        return statements;
    }

    @POST
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public Response doPostGraphStatement(@PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri, String bodyName) throws IOException {
        System.out.println("Post graph statement");
        System.out.println("repId = " + repId);
        System.out.println("graphName = " + graphName);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);
               
        try {
            ValueFactory vf = repository.getValueFactory();
            String myUri = uri.getBaseUri().toString();
            IRI IRIgraph = vf.createIRI(myUri);
            Resource[] graph = new Resource[] { IRIgraph };
            final Repository r = repository;
            for (Statement stm : getFileContentAsStatements(bodyName, myUri)) {
                r.getConnection().add(stm, graph);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return Response.noContent().build();
    }

    @DELETE
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public void deleteGraph(@PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri) throws IOException {
        System.out.println("GET.Name activated");
        System.out.println("repId = " + repId);
        System.out.println("graphName = " + graphName);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            ValueFactory vf = repository.getValueFactory();

            // @Context UriInfo uri;
            String myUri = uri.getBaseUri().toString();
            // IRI IRIgraph = vf.createIRI(graphName);
            IRI IRIgraph = vf.createIRI(myUri); // ??????????
            Resource[] graph = new Resource[] { IRIgraph };
            // RDFHandler response = null;

            repositoryCon.clear(graph);
            logger.info("Graph {} deleted", graphName);
        } catch (Exception e) {
            logger.error("Cannot delete graph " + graphName + ". Internal error", e);
            throw new WebApplicationException("Cannot delete graph " + graphName + ". Internal error", INTERNAL_SERVER_ERROR);
        }
    };
}
