package com.lazan.dependency.export

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
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

        // alle Konfigurationen filtern die nicht resolved werden dürfen
        defaultConfigurations = defaultConfigurations.findAll { it.canBeResolved  }

        println "Verwendete Konfigurationen: " + defaultConfigurations*.name

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
            println "Kopiere ${artifact.file} ..."
            def moduleVersionId = artifact.moduleVersion.id
            File moduleDir = new File(exportDir, getPath(moduleVersionId.group, moduleVersionId.name, moduleVersionId.version))
            project.mkdir(moduleDir)
            project.copy {
                from artifact.file
                into moduleDir
            }
        }
    }

    protected void copyPoms(Configuration config, ModelResolver modelResolver) {
        def componentIds = config.incoming.resolutionResult.allDependencies.collect { it.selected.id }

        def result = project.dependencies.createArtifactResolutionQuery()
                .forComponents(componentIds)
                .withArtifacts(MavenModule, MavenPomArtifact)
                .execute()

		DefaultModelBuilderFactory factory = new DefaultModelBuilderFactory()
		DefaultModelBuilder builder = factory.newInstance()
		
        for(component in result.resolvedComponents) {
            def componentId = component.id

            if (componentId instanceof ModuleComponentIdentifier) {
                File moduleDir = new File(exportDir, getPath(componentId.group, componentId.module, componentId.version))
                project.mkdir(moduleDir)
                component.getArtifacts(MavenPomArtifact).each { ArtifactResult artifactResult ->
                    File pomFile = artifactResult.file
                    project.copy {
                        println "Kopiere ${pomFile} ..."
                        from pomFile
                        into moduleDir
                    }
					
					// force the parent POMs and BOMs to be downloaded and copied
                    try {
                        ModelBuildingRequest req = new DefaultModelBuildingRequest()
                        req.setModelResolver(modelResolver)
                        req.setPomFile(pomFile)
                        // System Properties kopieren da diese in einigen POMs benötigt werden
                        req.getSystemProperties().put("java.version", System.getProperty("java.version"))
                        req.getSystemProperties().put("java.home", System.getProperty("java.home"))
                        // Minimale Validierung der Parent POMs
                        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
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
            		println "Kopiere ${pomFile} ..."
			from pomFile
			into moduleDir
		}
	}

    protected String getPath(String group, String module, String version) {
        return "${group.replace('.','/')}/${module}/${version}"
    }
}
