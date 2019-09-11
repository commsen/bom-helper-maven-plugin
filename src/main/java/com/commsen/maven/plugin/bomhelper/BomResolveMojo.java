package com.commsen.maven.plugin.bomhelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal which resolves dependencies declared in dependency management (BOM)
 */

@Mojo(
		name = "resolve",		
		defaultPhase = LifecyclePhase.VALIDATE)
public class BomResolveMojo extends AbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(BomResolveMojo.class);
	
	@Component
	private ArtifactResolver artifactResolver;

	/**
	 * The Maven session
	 */
	@Parameter(
			defaultValue = "${session}",
			readonly = true,
			required = true)
	protected MavenSession session;

	/**
	 * Remote repositories which will be searched for artifacts.
	 */
	@Parameter(
			defaultValue = "${project.remoteArtifactRepositories}",
			readonly = true,
			required = true)
	private List<ArtifactRepository> remoteRepositories;

    /**
     * POM
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;	
	
	
	public void execute() throws MojoExecutionException {
		List<Dependency> bomDepenedencies =  project.getDependencyManagement().getDependencies();
		Set<String> failedArtifacts = new HashSet<>();
		ProjectBuildingRequest projectBuildingRequest = newResolveArtifactProjectBuildingRequest();
		
		for (Dependency dependency : bomDepenedencies) {
			DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
			coordinate.setGroupId(dependency.getGroupId());
			coordinate.setArtifactId(dependency.getArtifactId());
			coordinate.setVersion(dependency.getVersion());
			coordinate.setExtension(dependency.getType());
			coordinate.setClassifier(dependency.getClassifier());
			try {
				artifactResolver.resolveArtifact(projectBuildingRequest, coordinate);
			} catch (ArtifactResolverException e) {
				failedArtifacts.add(dependency.toString());
				logger.error("Failed to resolve artifact " + coordinate, e);
			}
		}

		if (!failedArtifacts.isEmpty()) {
//			failedArtifacts.stream().forEach(a -> logger.error("Failed to resolve artifact " + a));
			throw new MojoExecutionException(
					"The following dependencies found in <dependencyManagement> can not be resolved: \n" +
					failedArtifacts.stream().collect(Collectors.joining("\n - "))
					);
		}
		
	}


	private ProjectBuildingRequest newResolveArtifactProjectBuildingRequest() {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

		buildingRequest.setRemoteRepositories(remoteRepositories);

		return buildingRequest;
	}
}
