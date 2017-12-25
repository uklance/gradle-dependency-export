package com.lazan.dependency.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

@SuppressWarnings("deprecation")
public class ModelResolverImpl implements ModelResolver {
	private final String taskName;
	private final Project project;
	private final ModelResolveListener listener;
	private final AtomicInteger configurationCount = new AtomicInteger(0);

	public ModelResolverImpl(String taskName, Project project, ModelResolveListener listener) {
		super();
		this.taskName = taskName;
		this.project = project;
		this.listener = listener;
	}

	@Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        String configName = String.format("%s%s", taskName, configurationCount.getAndIncrement());
        Configuration config = project.getConfigurations().create(configName);
        config.setTransitive(false);
        String depNotation = String.format("%s:%s:%s@pom", groupId, artifactId, version);
        org.gradle.api.artifacts.Dependency dependency = project.getDependencies().create(depNotation);
        config.getDependencies().add(dependency);

        File pomXml = config.getSingleFile();
        System.err.println(String.format("############# %s:%s:%s", groupId, artifactId, version));
        listener.onResolveModel(groupId, artifactId, version, pomXml);
        return new ModelSource() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(pomXml);
            }

            @Override
            public String getLocation() {
                return pomXml.getAbsolutePath();
            }
        };
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
        // ignore
    }

    @Override
    public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {
        // ignore
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }
}