package ru.agentlab.rdf4j.jaxrs.repository;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;

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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = GraphComponent.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j-server")
public class GraphComponent {
    private static final Logger logger = LoggerFactory.getLogger(GraphComponent.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    public GraphComponent() {
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

    @DELETE
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public void deleteGraph(@PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri) throws IOException {
        System.out.println("GET.Name activated");
        System.out.println("repId = " + repId);
        System.out.println("graphName = " + graphName);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        try (RepositoryConnection repositoryCon = repository.getConnection()){
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
