package com.commsen.maven.plugin.bomhelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 * Creates dependency management entries (BOM) from jar files stored in directory on local file system. 
 *  
 */

@Mojo(
		name = "fromJars",
		requiresProject = true,
		defaultPhase = LifecyclePhase.VALIDATE)
public class BomFromJarsMojo extends PomChangingAbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(BomFromJarsMojo.class);

	
	/**
	 * 
	 * The folder to search (recursively) for libraries to be converted to dependency management entries
	 * 
	 */
	@Parameter(
			defaultValue = "${project.basedir}/lib",
			property = "bom-helper.jarsFolder")
	private File librariesFolder;


	/**
	 * 
	 * If true the build will fail if there are libraries found that can not be converted to dependency management entries
	 * 
	 */
	@Parameter(
			defaultValue = "false",
			property = "bom-helper.allOrNothing")
	protected boolean allOrNothing;

	/**
	 * 
	 * Should sub-directories be recursively scanned for Jar files
	 * 
	 */
	@Parameter(
			defaultValue = "false",
			property = "bom-helper.recursive")
	protected boolean recursive;

	/**
	 * 
	 * The maximum number of directory levels to visit
	 * 
	 */
	@Parameter(
			defaultValue = "100",
			property = "bom-helper.recursiveDepth")
	protected int folderDepth;
	
	
	public void execute() throws MojoExecutionException {

		List<Properties> mavenCoordinates = new LinkedList<>();
		Set<String> missingMavenData = new TreeSet<>();
		
		
		try {
			
			logger.debug("Scanning `" + librariesFolder.getAbsolutePath() + "` for jars!");
			
			parseFolder(librariesFolder, mavenCoordinates, missingMavenData);
			
			if (!missingMavenData.isEmpty()) {
				
				missingMavenData.forEach(f -> logger.warn("File `" + f + "` does not contain Maven metadata and can not be automatically added to dependency management entries!"));
				if (allOrNothing) {
					throw new MojoExecutionException("Property `allOrNoting` is `true` but some jars could not be converted to dependency management entries!");
				}
			}
			
			if (!mavenCoordinates.isEmpty()) {
		        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(project.getFile());
			    XPath xPath = XPathFactory.newInstance().newXPath();

			    // create dependencyManagement if does not exists;
			    Node dependencyManagementNode = (Node)xPath.evaluate("//dependencyManagement", doc,  XPathConstants.NODE);
			    if (dependencyManagementNode == null) {
			    	logger.debug("Creating dependencyManagement node!");
			    	dependencyManagementNode = doc.createElement("dependencyManagement");
			    	doc.getFirstChild().appendChild(dependencyManagementNode);
			    }

			    // create dependencyManagement/dependencies if does not exists;
			    Node dependenciesNode = (Node)xPath.evaluate("//dependencyManagement/dependencies", doc,  XPathConstants.NODE);
			    if (dependenciesNode == null) {
			    	logger.debug("Creating dependencies node!");
			    	dependenciesNode = doc.createElement("dependencies");
			    	dependencyManagementNode.appendChild(dependenciesNode);
			    }

			    Set<Gav> currentGavs = project.getDependencyManagement().getDependencies()
			    		.stream()
			    		.map(d -> {
			    			return new Gav(d.getGroupId(), d.getArtifactId(), d.getVersion()); 
			    		})
			    		.collect(Collectors.toSet());
			    
			    boolean added = false;
			    
			    for (Properties p : mavenCoordinates) {
			    	Gav gav = new Gav(p.getProperty("groupId"), p.getProperty("artifactId"), p.getProperty("version"));
			    	if (!currentGavs.contains(gav)) {
			    		Node dependency = doc.createElement("dependency");
				    	dependency.appendChild(createNode(doc, "groupId", gav.g));
				    	dependency.appendChild(createNode(doc, "artifactId", gav.a));
				    	dependency.appendChild(createNode(doc, "version", gav.v));
				    	dependenciesNode.appendChild(dependency);
				    	added = true;
			    	} else {
				    	logger.debug("Skipping " + gav + " as it already is in dependency management!");
			    	}
				}
			       
		    	if (added) {
		    		savePom(doc);
		    	} else {
		    		logger.info("Pom will not be changed. All files with Maven matada alredy have dependency management entries");
		    	}
			} else {
	    		logger.info("No jar files with Maven matada were found!");
			}
			
		} catch (Exception e) {
			throw new MojoExecutionException("An error occured while building BOM's dependency management!", e);
		}
	}
	
	private void parseFolder(File folder, List<Properties> mavenCoordinates, Set<String> missingMavenData) throws IOException {
		
		int maxDepth = 1;
		
		if (recursive) {
			maxDepth = folderDepth;
		}
		
		mavenCoordinates.addAll(
			Files
			 .walk(folder.toPath(), maxDepth, FileVisitOption.FOLLOW_LINKS)
			 .map(Path::toFile)
			 .filter(f -> f.isFile())
			 .filter(f -> f.getName().endsWith(".jar"))
			 .map(this::toJarFile)
			 .filter(Objects::nonNull)
			 .map(jar -> toMavenProperties(jar, missingMavenData))
			 .filter(Objects::nonNull)
			 .collect(Collectors.toList())
		);
	}
	
	private Properties toMavenProperties(JarFile jar, Set<String> missingMavenData) {
		Properties properties = null;
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			if (jarEntry.getName().matches("^META-INF/maven/.*/pom\\.properties$")) {
				try {
					properties = new Properties();
					properties.load(jar.getInputStream(jarEntry));
					break;
				} catch (IOException e) {
					 e.printStackTrace();
				}
			};
		} 
		if (properties == null) {
			missingMavenData.add(jar.getName());
		}
		return properties;
	}

	private JarFile toJarFile(File f) {
		try {
			return new JarFile(f);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Node createNode(Document doc, String name, String text) {
		Node n = doc.createElement(name);
		n.setTextContent(text);
		return n;
	}

}
