<<<<<<< HEAD
package org.eclipse.rdf4j.http.server.repository;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.*;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static org.eclipse.rdf4j.http.protocol.Protocol.*;

import java.util.HashMap;
import java.util.Map;


@Component(service = TransactionControllerGet.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionControllerGet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public TransactionControllerGet() {
        System.out.println("Init TransactionControllerUpdate");
    }

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @Context HttpServletResponse response,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId
                                        ) throws Exception {
        String reqMethod = request.getMethod();
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());


        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        if (action == Action.GET) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                    transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST
                        );
            }
            getExportStatementsResult(connection, transactionId, request, response);
            logger.info("{} txn size request finished", reqMethod);
        } else {
            throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED
                    );
        }
    }

    private void getExportStatementsResult(RepositoryConnection conn, String txnId,
                                                   HttpServletRequest request, HttpServletResponse response)
            throws ClientHTTPException
    {
        ProtocolUtil.logRequestParameters(request);

        ValueFactory vf = conn.getValueFactory();

        Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
        IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
        Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
        Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
        boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response, RDFWriterRegistry.getInstance());

        Map<String, Object> model = new HashMap<String, Object>();
//        model.put(ExportStatementsView.SUBJECT_KEY, subj);
//        model.put(ExportStatementsView.PREDICATE_KEY, pred);
//        model.put(ExportStatementsView.OBJECT_KEY, obj);
//        model.put(ExportStatementsView.CONTEXTS_KEY, contexts);
//        model.put(ExportStatementsView.USE_INFERENCING_KEY, useInferencing);
//        model.put(ExportStatementsView.FACTORY_KEY, rdfWriterFactory);
    }

=======
package org.eclipse.rdf4j.http.server.repository;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.*;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static org.eclipse.rdf4j.http.protocol.Protocol.*;

import java.util.HashMap;
import java.util.Map;


@Component(service = TransactionControllerGet.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionControllerGet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public TransactionControllerGet() {
        System.out.println("Init TransactionControllerUpdate");
    }

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @Context HttpServletResponse response,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId
                                        ) throws Exception {
        String reqMethod = request.getMethod();
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());


        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        if (action == Action.GET) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                    transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST
                        );
            }
            getExportStatementsResult(connection, transactionId, request, response);
            logger.info("{} txn size request finished", reqMethod);
        } else {
            throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED
                    );
        }
    }

    private void getExportStatementsResult(RepositoryConnection conn, String txnId,
                                                   HttpServletRequest request, HttpServletResponse response)
            throws ClientHTTPException
    {
        ProtocolUtil.logRequestParameters(request);

        ValueFactory vf = conn.getValueFactory();

        Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
        IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
        Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
        Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
        boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response, RDFWriterRegistry.getInstance());

        Map<String, Object> model = new HashMap<String, Object>();
//        model.put(ExportStatementsView.SUBJECT_KEY, subj);
//        model.put(ExportStatementsView.PREDICATE_KEY, pred);
//        model.put(ExportStatementsView.OBJECT_KEY, obj);
//        model.put(ExportStatementsView.CONTEXTS_KEY, contexts);
//        model.put(ExportStatementsView.USE_INFERENCING_KEY, useInferencing);
//        model.put(ExportStatementsView.FACTORY_KEY, rdfWriterFactory);
    }

>>>>>>> 4e002495842c02ff0066ad379e3d176ba774e857
}