package org.eclipse.rdf4j.http.server.repository;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.*;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Enumeration;

import static org.eclipse.rdf4j.http.protocol.Protocol.Action.UPDATE;
import static org.eclipse.rdf4j.http.protocol.Protocol.*;

@Component(service = TransactionControllerUpdate.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionControllerUpdate {

    private static Logger logger = LoggerFactory.getLogger(TransactionControllerUpdate.class.getName());

    public TransactionControllerUpdate() {
        System.out.println("Init TransactionControllerUpdate");
    }

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId) throws Exception {

        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());


        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        if (action == UPDATE) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                    transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST);
            }
            getSparqlUpdateResult(connection, request);
        } else {
            throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED);
        }
    }

    private void getSparqlUpdateResult(RepositoryConnection conn, HttpServletRequest request)
            throws WebApplicationException, HTTPException {
        String sparqlUpdateString = null;
        final String contentType = request.getContentType();
        if (contentType != null && contentType.contains(Protocol.SPARQL_UPDATE_MIME_TYPE)) {
            try {
                final String encoding = request.getCharacterEncoding() != null
                        ? request.getCharacterEncoding() : "UTF-8";
                sparqlUpdateString = IOUtils.toString(request.getInputStream(), encoding);
            } catch (IOException e) {
                logger.warn("error reading sparql update string from request body", e);
                throw new WebApplicationException(
                        "could not read SPARQL update string from body: " + e.getMessage(), Response.Status.BAD_REQUEST);
            }
        } else {
            sparqlUpdateString = request.getParameter(Protocol.UPDATE_PARAM_NAME);
        }

        logger.debug("SPARQL update string: {}", sparqlUpdateString);

        QueryLanguage queryLn = QueryLanguage.SPARQL;

        String queryLnStr = request.getParameter(QUERY_LANGUAGE_PARAM_NAME);
        logger.debug("query language param = {}", queryLnStr);

        if (queryLnStr != null) {
            queryLn = QueryLanguage.valueOf(queryLnStr);

            if (queryLn == null) {
                throw new WebApplicationException("Unknown query language: " + queryLnStr, Response.Status.BAD_REQUEST);
            }
        }

        String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);

        boolean includeInferred = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        String[] defaultRemoveGraphURIs = request.getParameterValues(REMOVE_GRAPH_PARAM_NAME);
        String[] defaultInsertGraphURIs = request.getParameterValues(INSERT_GRAPH_PARAM_NAME);
        String[] defaultGraphURIs = request.getParameterValues(USING_GRAPH_PARAM_NAME);
        String[] namedGraphURIs = request.getParameterValues(USING_NAMED_GRAPH_PARAM_NAME);

        SimpleDataset dataset = new SimpleDataset();

        if (defaultRemoveGraphURIs != null) {
            for (String graphURI : defaultRemoveGraphURIs) {
                try {
                    IRI uri = null;
                    if (!"null".equals(graphURI)) {
                        uri = conn.getValueFactory().createIRI(graphURI);
                    }
                    dataset.addDefaultRemoveGraph(uri);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException(
                            "Illegal URI for default remove graph: " + graphURI, Response.Status.BAD_REQUEST);
                }
            }
        }

        if (defaultInsertGraphURIs != null && defaultInsertGraphURIs.length > 0) {
            String graphURI = defaultInsertGraphURIs[0];
            try {
                IRI uri = null;
                if (!"null".equals(graphURI)) {
                    uri = conn.getValueFactory().createIRI(graphURI);
                }
                dataset.setDefaultInsertGraph(uri);
            } catch (IllegalArgumentException e) {
                throw new WebApplicationException(
                        "Illegal URI for default insert graph: " + graphURI, Response.Status.BAD_REQUEST);
            }
        }

        if (defaultGraphURIs != null) {
            for (String defaultGraphURI : defaultGraphURIs) {
                try {
                    IRI uri = null;
                    if (!"null".equals(defaultGraphURI)) {
                        uri = conn.getValueFactory().createIRI(defaultGraphURI);
                    }
                    dataset.addDefaultGraph(uri);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException(
                            "Illegal URI for default graph: " + defaultGraphURI, Response.Status.BAD_REQUEST);
                }
            }
        }

        if (namedGraphURIs != null) {
            for (String namedGraphURI : namedGraphURIs) {
                try {
                    IRI uri = null;
                    if (!"null".equals(namedGraphURI)) {
                        uri = conn.getValueFactory().createIRI(namedGraphURI);
                    }
                    dataset.addNamedGraph(uri);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException(
                            "Illegal URI for named graph: " + namedGraphURI, Response.Status.BAD_REQUEST);
                }
            }
        }

        try {
            Update update = conn.prepareUpdate(queryLn, sparqlUpdateString, baseURI);

            update.setIncludeInferred(includeInferred);

            if (dataset != null) {
                update.setDataset(dataset);
            }

            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();

                if (parameterName.startsWith(BINDING_PREFIX)
                        && parameterName.length() > BINDING_PREFIX.length()) {
                    String bindingName = parameterName.substring(BINDING_PREFIX.length());
                    Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
                            conn.getValueFactory());
                    update.setBinding(bindingName, bindingValue);
                }
            }

            update.execute();
        } catch (UpdateExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                throw (HTTPException) e.getCause();
            } else {
                throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
            }
        } catch (RepositoryException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                throw (HTTPException) e.getCause();
            } else {
                throw new ServerHTTPException("Repository update error: " + e.getMessage(), e);
            }
        } catch (MalformedQueryException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), Response.Status.BAD_REQUEST);
        }
    }

}
