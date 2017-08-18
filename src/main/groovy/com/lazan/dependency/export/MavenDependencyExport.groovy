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

class MavenDependencyExport extends DefaultTask {
    private Collection<Configuration> configurations = new LinkedHashSet<Configuration>();

    @InputFiles
    FileCollection getInputFiles() {
        return project.files(prepareConfigurations())
    }

    @OutputDirectory
    File exportDir = new File(project.buildDir, 'maven-dependency-export')

    private Collection<Configuration> prepareConfigurations() {
        if (!configurations.empty) {
            return configurations
        }
        Collection<Configuration> defaultConfigurations = new LinkedHashSet<Configuration>()
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
        for (Configuration config : prepareConfigurations()) {
            copyJars(config)
            copyPoms(config)
        }
    }

    private void copyJars(Configuration config) {
        for (ResolvedArtifact artifact : config.resolvedConfiguration.resolvedArtifacts) {
            def moduleVersionId = artifact.moduleVersion.id
            File moduleDir = new File(exportDir, getPath(moduleVersionId.group, moduleVersionId.name, moduleVersionId.version))
            project.mkdir(moduleDir)
            project.copy {
                from artifact.file
                into moduleDir
            }
        }
    }

    private void copyPoms(Configuration config) {
        def componentIds = config.incoming.resolutionResult.allDependencies.collect { it.selected.id }

        def result = project.dependencies.createArtifactResolutionQuery()
                .forComponents(componentIds)
                .withArtifacts(MavenModule, MavenPomArtifact)
                .execute()

        for(component in result.resolvedComponents) {
            def componentId = component.id

            if (componentId instanceof ModuleComponentIdentifier) {
                File moduleDir = new File(exportDir, getPath(componentId.group, componentId.module, componentId.version))
                project.mkdir(moduleDir)
                component.getArtifacts(MavenPomArtifact).each { ArtifactResult artifactResult ->
                    File pomFile = artifactResult.file
                    project.copy {
                        from pomFile
                        into moduleDir
                    }
                }
            }
        }
    }

    private String getPath(String group, String module, String version) {
        return "${group.replace('.','/')}/${module}/${version}"
    }
}
