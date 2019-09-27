package com.commsen.maven.plugin.bomhelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;

public abstract class PomChangingAbstractMojo extends BomHelperAbstaractMojo {

	@Parameter(
			defaultValue = "false",
			property = "bom-helper.inplace")
	protected boolean inplace;

	@Parameter(
			defaultValue = "pom-bom-update.xml",
			property = "bom-helper.outputFile")
	protected String outputFileName;

	@Parameter(
			defaultValue = "pom-original.xml",
			property = "bom-helper.backupFile")
	protected String backupFileName;

	@Parameter(
			defaultValue = "false",
			property = "bom-helper.backupFile.replace")
	protected boolean replaceBackup;

	@Parameter(
			defaultValue = "true",
			property = "bom-helper.backup")
	protected boolean makeBackup;

	/**
	 * @param doc
	 * @throws MojoExecutionException
	 * @throws IOException
	 * @throws TransformerConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	protected void savePom(Document doc) throws MojoExecutionException, IOException,
												TransformerConfigurationException, TransformerFactoryConfigurationError, 
												TransformerException {
		File outputFile;
		
		if (inplace) {
			outputFile = project.getFile();
			if (makeBackup) {
		    	Path backupFile = Paths.get(outputFile.getParent(), backupFileName);
		    	if (!replaceBackup && backupFile.toFile().exists()) {
					throw new MojoExecutionException("File " + backupFile + " already exists and `replaceBackup` is `false`!");
		    	}
		    	Files.copy(outputFile.toPath(), backupFile, StandardCopyOption.REPLACE_EXISTING);
			}
		} else {
			outputFile = new File(project.getBasedir(), outputFileName);
		}
		
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(outputFile);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, result);
	
		transformer.transform(source, result);
	}
}