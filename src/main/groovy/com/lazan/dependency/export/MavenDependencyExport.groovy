package com.lazan.dependency.export

import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.result.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.*
import org.gradle.api.artifacts.component.*
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.api.component.Artifact
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact

import org.apache.maven.model.Model
import org.apache.maven.model.building.DefaultModelBuilder
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.resolution.ModelResolver

class MavenDependencyExport extends DefaultTask {
	public Collection<Configuration> configurations = new LinkedHashSet<>()
	public Map<String, Object> systemProperties = System.getProperties()
	boolean exportSources
	boolean exportJavadoc
	boolean filterUnresolvable
	
	@InputFiles
	FileCollection getInputFiles() {
		return project.files(prepareConfigurations())
	}

	@OutputDirectory
	File exportDir = new File(project.buildDir, 'maven-dependency-export')

	protected Collection<Configuration> prepareConfigurations() {
		if (!configurations.empty) {
			return configurations
		}
		Collection<Configuration> defaultConfigurations = new LinkedHashSet<>()
		defaultConfigurations.addAll(project.buildscript.configurations.findAll { it.canBeResolved })
		defaultConfigurations.addAll(project.configurations.findAll { it.canBeResolved })
		return defaultConfigurations
	}

	void setConfigurations(Collection<Configuration> configs) {
		for (Configuration conf : configs)
			configuration(conf)
	}
	
	void configuration(String name) {
		Configuration config = project.configurations.getByName(name)
		if (config.canBeResolved) {
			configurations.add(config)
		} else {
			logger.warn "Configuration ${config.name} was not added cause it is not resolvable."
		}
	}

	void configuration(Configuration configuration) {
		if (configuration.canBeResolved) {
			configurations.add(configuration)
		} else {
			logger.warn "Configuration ${configuration.name} was not added cause it is not resolvable."
		}
	}

	@TaskAction
	void build() {
		ModelResolveListener resolveListener = { String groupId, String artifactId, String version, File pomFile ->
			copyAssociatedPom(groupId, artifactId, version, pomFile)
		}
		ModelResolver modelResolver = new ModelResolverImpl(name, project, resolveListener)
		for (Configuration config : prepareConfigurations()) {
			logger.info "Exporting ${config.name}..."
			copyJars(config)
			copyPoms(config, modelResolver)
			if (exportSources)
				copyAdditionalArtifacts(config, modelResolver, SourcesArtifact)
			if (exportJavadoc)
				copyAdditionalArtifacts(config, modelResolver, JavadocArtifact)
		}
		Set<String> exportedPaths = new TreeSet()
		project.fileTree(exportDir).visit {
			if (!it.directory) {
				exportedPaths << it.relativePath.pathString
			}
		}
		logger.info("Exported ${exportedPaths.size()} files to $exportDir")
		exportedPaths.each {
			logger.info("   $it")
		}
	}

	protected void copyJars(Configuration config) {
		for (ResolvedArtifact artifact : config.resolvedConfiguration.resolvedArtifacts) {
			ModuleVersionIdentifier moduleVersionId = artifact.moduleVersion.id
			File moduleDir = new File(exportDir, getPath(moduleVersionId.group, moduleVersionId.name, moduleVersionId.version))
			project.mkdir(moduleDir)
			project.copy {
				from artifact.file
				into moduleDir
			}
		}
	}
	
	protected void copyAdditionalArtifacts(Configuration config, ModelResolver modelResolver, Class<? extends Artifact> artifactType) {
	
		List<ComponentIdentifier> componentIds = config.incoming.resolutionResult.allDependencies.collect { it.selected.id }

		ArtifactResolutionResult result = project.dependencies.createArtifactResolutionQuery()
				.forComponents(componentIds)
				.withArtifacts(JvmLibrary, artifactType)
				.execute()

		for (component in result.resolvedComponents) {
			ComponentIdentifier componentId = component.id
			if (componentId instanceof ModuleComponentIdentifier) {
				File moduleDir = new File(exportDir, getPath(componentId.group, componentId.module, componentId.version))
				project.mkdir(moduleDir)
				Set<ArtifactResult> artifacts = component.getArtifacts(artifactType)
				artifacts.each { 
					ArtifactResult artifactResult ->
						File file = artifactResult.file
						project.copy {
							from file
							into moduleDir
						}
				}
			}
		}

	}

	protected void copyPoms(Configuration config, ModelResolver modelResolver) {
		List<ComponentIdentifier> componentIds = config.incoming.resolutionResult.allDependencies.collect { it.selected.id }

		ArtifactResolutionResult result = project.dependencies.createArtifactResolutionQuery()
				.forComponents(componentIds)
				.withArtifacts(MavenModule, MavenPomArtifact)
				.execute()

		DefaultModelBuilderFactory factory = new DefaultModelBuilderFactory()
		DefaultModelBuilder builder = factory.newInstance()

		for (component in result.resolvedComponents) {
			ComponentIdentifier componentId = component.id

			if (componentId instanceof ModuleComponentIdentifier) {
				File moduleDir = new File(exportDir, getPath(componentId.group, componentId.module, componentId.version))
				project.mkdir(moduleDir)
				component.getArtifacts(MavenPomArtifact).each { ArtifactResult artifactResult ->

					// only try unresolvable artifacts when filterUnresolvable is set to false
					boolean isResolvableArtifact = !(artifactResult instanceof UnresolvedArtifactResult);
					if (isResolvableArtifact || !filterUnresolvable) {
						File pomFile = artifactResult.file
						project.copy {
							from pomFile
							into moduleDir
						}
						// force the parent POMs and BOMs to be downloaded and copied
						try {
							ModelBuildingRequest req = new DefaultModelBuildingRequest()
									req.setModelResolver(modelResolver)
									req.setPomFile(pomFile)
									req.getSystemProperties().putAll(systemProperties)
									req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
									
									// execute the model building request
									builder.build(req).getEffectiveModel()
						} catch (Exception e) {
							logger.error("Error resolving $pomFile", e)
						}
					}
				}
			}
		}
	}

	protected void copyAssociatedPom(String groupId, String artifactId, String version, File pomFile) {
		File moduleDir = new File(exportDir, getPath(groupId, artifactId, version))
		project.mkdir(moduleDir)
		project.copy {
			from pomFile
			into moduleDir
		}
	}

	protected String getPath(String group, String module, String version) {
		return "${group.replace('.','/')}/${module}/${version}"
	}
}
