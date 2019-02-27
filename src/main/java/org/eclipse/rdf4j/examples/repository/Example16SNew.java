package org.eclipse.rdf4j.examples.repository;

import org.eclipse.rdf4j.examples.model.vocabulary.EX;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
/**
 * RDF Tutorial example 15: executing a simple SPARQL query on the database
 *
 * @author Jeen Broekstra
 */
public class Example16SNew {

    public static void main(String[] args)
            throws IOException
    {
        Repository db = new SailRepository(new MemoryStore());
        db.initialize();

        try (RepositoryConnection conn = db.getConnection()) {
            String filename = "example-data-artists.ttl";
            try (InputStream input =
                         Example15SimpleSPARQLQuery.class.getResourceAsStream("/" + filename)) {
                conn.add(input, "", RDFFormat.TURTLE );
            }

            ValueFactory vf = SimpleValueFactory.getInstance();
            String ex = "http://example.org/";
            IRI artist = vf.createIRI(ex, "Artist");

            IRI kopeykin = vf.createIRI(ex, "Kopeykin");
            conn.add(kopeykin, RDF.TYPE, artist);
            conn.add(kopeykin, FOAF.FIRST_NAME, vf.createLiteral("Nicolay"));

            ArrayList<String> Paintings = new ArrayList<>();
            Paintings.add("AmonaLisa");
            Paintings.add("MissionNevupolnima");

            IRI Painting = vf.createIRI(ex, "Painting");

            for (int i = 0; i < Paintings.size(); ++i)
            {
                conn.add(kopeykin, EX.CREATOR_OF, vf.createLiteral(Paintings.get(i)));

                IRI Painting1 = vf.createIRI(ex, Paintings.get(i));
                conn.add(Painting1, RDF.TYPE, Painting);
                conn.add(Painting1, RDFS.LABEL, vf.createLiteral(Paintings.get(i)));
            }

            String queryString = "PREFIX ex: <http://example.org/> \n";
            queryString += "PREFIX foaf: <" + FOAF.NAMESPACE + "> \n";
            queryString += "CONSTRUCT \n";
            queryString += "WHERE { \n";
            queryString += " ?s a ex:Artist; \n";
            queryString += " foaf:firstName ?n .";
            queryString += " ?p a ex:Painting; \n";
            queryString += " rdfs:label ?l .";
            queryString += "}";

            GraphQuery query = conn.prepareGraphQuery(queryString);

            RDFHandler turtleWriter = Rio.createWriter(RDFFormat.TURTLE, System.out);
            query.evaluate(turtleWriter);

            try (GraphQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    Statement st = result.next();
                    System.out.println(st);
                }
            }
        }
        finally {
            db.shutDown();
        }
    }
}