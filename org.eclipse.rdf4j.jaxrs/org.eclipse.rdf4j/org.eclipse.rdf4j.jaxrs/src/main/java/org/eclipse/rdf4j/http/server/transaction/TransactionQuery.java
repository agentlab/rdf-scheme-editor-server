package org.eclipse.rdf4j.http.server.transaction;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.*;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
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
import java.io.IOException;
import java.util.Enumeration;

import static org.apache.http.HttpStatus.*;
import static org.eclipse.rdf4j.http.protocol.Protocol.Action.QUERY;
import static org.eclipse.rdf4j.http.protocol.Protocol.*;

@Component(service = TransactionQuery.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionQuery {

    private static Logger logger = LoggerFactory.getLogger(TransactionQuery.class.getName());

    public TransactionQuery() {
        System.out.println("Init TransactionQuery");
    }

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @Context HttpServletResponse response,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId) throws Exception {

        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());

        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        if (action == QUERY) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                    transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'",
                        Response.Status.BAD_REQUEST);
            }
            processQuery(connection, transactionId, request, response);
        } else {
            throw new WebApplicationException("Action not supported: " + action,
                    Response.Status.METHOD_NOT_ALLOWED);
        }
    }

    private void processQuery(RepositoryConnection conn, String txnId, HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException, WebApplicationException, ClientHTTPException {
        String queryStr = null;
        final String contentType = request.getContentType();
        if (contentType != null && contentType.contains(Protocol.SPARQL_QUERY_MIME_TYPE)) {
            final String encoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding()
                    : "UTF-8";
            queryStr = IOUtils.toString(request.getInputStream(), encoding);
        } else {
            queryStr = request.getParameter(QUERY_PARAM_NAME);
        }
        Query query = getQuery(conn, queryStr, request, response);

        try {
            if (query instanceof TupleQuery) {
                TupleQuery tQuery = (TupleQuery) query;
                tQuery.evaluate();
            } else if (query instanceof GraphQuery) {
                GraphQuery gQuery = (GraphQuery) query;
                gQuery.evaluate();
            } else if (query instanceof BooleanQuery) {
                BooleanQuery bQuery = (BooleanQuery) query;
                bQuery.evaluate();
            } else {
                throw new WebApplicationException("Unsupported query type: " + query.getClass().getName(),
                        Response.Status.BAD_REQUEST);
            }
        } catch (QueryInterruptedException e) {
            logger.info("Query interrupted", e);
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
            throw new WebApplicationException("Query evaluation took too long", Response.Status.SERVICE_UNAVAILABLE);
        } catch (QueryEvaluationException e) {
            logger.info("Query evaluation error", e);
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                throw (WebApplicationException) e.getCause();
            } else {
                throw new WebApplicationException("Query evaluation error: " + e.getMessage());
            }
        }
    }

    private Query getQuery(RepositoryConnection repositoryCon, String queryStr, HttpServletRequest request,
                           HttpServletResponse response)
            throws IOException, WebApplicationException, ClientHTTPException {
        Query result = null;

        // default query language is SPARQL
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

        // determine if inferred triples should be included in query evaluation
        boolean includeInferred = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        // build a dataset, if specified
        String[] defaultGraphURIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
        String[] namedGraphURIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);

        SimpleDataset dataset = null;
        if (defaultGraphURIs != null || namedGraphURIs != null) {
            dataset = new SimpleDataset();

            if (defaultGraphURIs != null) {
                for (String defaultGraphURI : defaultGraphURIs) {
                    try {
                        IRI uri = null;
                        if (!"null".equals(defaultGraphURI)) {
                            uri = repositoryCon.getValueFactory().createIRI(defaultGraphURI);
                        }
                        dataset.addDefaultGraph(uri);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("Illegal URI for default graph: " + defaultGraphURI,
                                Response.Status.BAD_REQUEST);
                    }
                }
            }

            if (namedGraphURIs != null) {
                for (String namedGraphURI : namedGraphURIs) {
                    try {
                        IRI uri = null;
                        if (!"null".equals(namedGraphURI)) {
                            uri = repositoryCon.getValueFactory().createIRI(namedGraphURI);
                        }
                        dataset.addNamedGraph(uri);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("Illegal URI for named graph: " + namedGraphURI,
                                Response.Status.BAD_REQUEST);
                    }
                }
            }
        }

        try {
            result = repositoryCon.prepareQuery(queryLn, queryStr, baseURI);

            result.setIncludeInferred(includeInferred);

            if (dataset != null) {
                result.setDataset(dataset);
            }

            // determine if any variable bindings have been set on this query.
            @SuppressWarnings("unchecked")
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();

                if (parameterName.startsWith(BINDING_PREFIX)
                        && parameterName.length() > BINDING_PREFIX.length()) {
                    String bindingName = parameterName.substring(BINDING_PREFIX.length());
                    Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
                            repositoryCon.getValueFactory());
                    result.setBinding(bindingName, bindingValue);
                }
            }
        } catch (UnsupportedQueryLanguageException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
            throw new WebApplicationException(errInfo.toString(), Response.Status.BAD_REQUEST);
        } catch (MalformedQueryException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), Response.Status.BAD_REQUEST);
        } catch (RepositoryException e) {
            logger.error("Repository error", e);
            throw new WebApplicationException("Repository error" + e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return result;
    }
}
