package com.commsen.maven.plugin.bomhelper;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class BomHelperAbstractMojo extends AbstractMojo {

	/**
	 * The Maven session
	 */
	@Parameter(
			defaultValue = "${session}",
			readonly = true,
			required = true)
	protected MavenSession session;
	
	/**
	 * POM
	 */
	@Parameter(
			defaultValue = "${project}",
			readonly = true,
			required = true)
	protected MavenProject project;

	public BomHelperAbstractMojo() {
		super();
	}

}