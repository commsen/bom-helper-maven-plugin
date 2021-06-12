package com.commsen.maven.plugin.bomhelper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves dependencies declared in dependency management (BOM)
 */

@Mojo(
		name = "resolve",
		requiresProject = true,
		defaultPhase = LifecyclePhase.VALIDATE)
public class BomResolveMojo extends BomHelperAbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(BomResolveMojo.class);

	/**
	 * Map types to extensions
	 */
	@Parameter
	protected Map<String, String> typeExtensions;

	
	/**
	 * Remote repositories which will be searched for artifacts.
	 */
	@Parameter(
			defaultValue = "${project.remoteArtifactRepositories}",
			readonly = true,
			required = true)
	protected List<ArtifactRepository> remoteRepositories;


	@Component
	private ArtifactResolver artifactResolver;


	public void execute() throws MojoExecutionException {

		DependencyManagement dependencyManagement = project.getDependencyManagement();
		if (dependencyManagement == null) {
			logger.info("No BOM dependencies found! Nothing to resolve!");
			return;
		}

		List<Dependency> bomDependencies =  dependencyManagement.getDependencies();
		Set<String> failedArtifacts = new HashSet<>();
		ProjectBuildingRequest projectBuildingRequest = newResolveArtifactProjectBuildingRequest();

		for (Dependency dependency : bomDependencies) {
			DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
			coordinate.setGroupId(dependency.getGroupId());
			coordinate.setArtifactId(dependency.getArtifactId());
			coordinate.setVersion(dependency.getVersion());
			coordinate.setExtension(typeToExtension(dependency.getType()));
			coordinate.setClassifier(dependency.getClassifier());
			try {
				artifactResolver.resolveArtifact(projectBuildingRequest, coordinate);
			} catch (ArtifactResolverException e) {
				failedArtifacts.add(dependency.toString());
				logger.error("Failed to resolve artifact " + coordinate, e);
			}
		}

		if (!failedArtifacts.isEmpty()) {
			throw new MojoExecutionException(
					"The following dependencies found in <dependencyManagement> can not be resolved: \n - " +
							String.join("\n - ", failedArtifacts)
					);
		}

	}


	private String typeToExtension(final String type) {
		if (typeExtensions == null || !typeExtensions.containsKey(type)) {
			return type;
		}
		return typeExtensions.get(type);
	}

	private ProjectBuildingRequest newResolveArtifactProjectBuildingRequest() {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

		buildingRequest.setRemoteRepositories(remoteRepositories);

		return buildingRequest;
	}
}
