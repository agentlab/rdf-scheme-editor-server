package org.eclipse.rdf4j.http.server.repository;

import java.io.File;

import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;


@Component(service = RepositoryManager.class)
public class RepositoryManagerComponent extends LocalRepositoryManager {
	protected static File folder = new File("./repositories");
	
	public RepositoryManagerComponent() {
		super(folder);
	}
}
