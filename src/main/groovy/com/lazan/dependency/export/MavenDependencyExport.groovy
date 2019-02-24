package com.lazan.dependency.export

import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.result.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

import org.apache.maven.model.Model
import org.apache.maven.model.building.DefaultModelBuilder
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.resolution.ModelResolver

class MavenDependencyExport extends DefaultTask {
    private Collection<Configuration> configurations = new LinkedHashSet<>()
	
	public Map<String, Object> systemProperties = System.getProperties()

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
        defaultConfigurations.addAll(project.buildscript.configurations)
        defaultConfigurations.addAll(project.configurations)
        return defaultConfigurations
    }

    void configuration(String name) {
        configurations.add(project.configurations.getByName(name))
    }

    void configuration(Configuration configuration) {
        configurations.add(configuration)
    }

    @TaskAction
    void build() {
		ModelResolveListener resolveListener = { String groupId, String artifactId, String version, File pomFile ->
			copyAssociatedPom(groupId, artifactId, version, pomFile)
		}
		ModelResolver modelResolver = new ModelResolverImpl(name, project, resolveListener)
        for (Configuration config : prepareConfigurations()) {
            copyJars(config)
            copyPoms(config, modelResolver)
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
                        e.printStackTrace(System.out)
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
