package com.commsen.maven.plugin.bomhelper;

import java.util.LinkedList;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Sorts alphabetically the dependencies declared in dependency management (BOM).
 * 
 */

@Mojo(
		name = "sort",
		requiresProject = true,
		defaultPhase = LifecyclePhase.VALIDATE)
public class BomSortMojo extends PomChangingAbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(BomSortMojo.class);

	
	public void execute() throws MojoExecutionException {

		try {
			
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(project.getFile());

		    XPath xPath = XPathFactory.newInstance().newXPath();
		    Node mgmDependenciesNode = (Node)xPath.evaluate("//dependencyManagement/dependencies", doc,  XPathConstants.NODE);
		    NodeList mgmDependenciesNodes = (NodeList) xPath.evaluate("//dependencyManagement/dependencies/dependency", doc, XPathConstants.NODESET);

		    if (mgmDependenciesNodes.getLength() == 0) {
		    	logger.info("No BOM dependencies found! Nothing to sort!");
		    	return;
		    }

		    
		    TreeMap<Gav, PreFormattedNode> deps = new TreeMap<>();
		    
		    Gav lastGav = null;
		    boolean sorted = true;
		    
		    for (int mdIndex = 0; mdIndex < mgmDependenciesNodes.getLength(); ++mdIndex) {
		        Node dependencyNode = mgmDependenciesNodes.item(mdIndex);
		        PreFormattedNode fmtDependencyNode = new PreFormattedNode();
		        fmtDependencyNode.node = dependencyNode;
		        Node tmpNode = dependencyNode.getPreviousSibling();
		        while (	tmpNode != null && Node.ELEMENT_NODE != tmpNode.getNodeType()) {
		        	fmtDependencyNode.prefix.addFirst(tmpNode);
		        	tmpNode = tmpNode.getPreviousSibling();
		        }

		        mgmDependenciesNode.removeChild(fmtDependencyNode.node);
		        fmtDependencyNode.prefix.forEach(mgmDependenciesNode::removeChild);
		        
		        NodeList gavNodes = dependencyNode.getChildNodes();
		        String g = "", a = "", v = "";
		        for (int gavNodesIndex = 0; gavNodesIndex < gavNodes.getLength(); gavNodesIndex++) {
			        Node gavNode = gavNodes.item(gavNodesIndex);
		        	if ("groupId".equals(gavNode.getNodeName())) g = gavNode.getTextContent();
		        	if ("artifactId".equals(gavNode.getNodeName())) a = gavNode.getTextContent();
		        	if ("version".equals(gavNode.getNodeName())) v = gavNode.getTextContent();
		        }
		        
		        Gav thisGav = new Gav(g, a, v);
		        if (lastGav != null && thisGav.compareTo(lastGav) < 0) {
		        	sorted = false;
		        }
		        deps.put(thisGav, fmtDependencyNode);
		        lastGav = thisGav;
		    }
		    
		    if (sorted) {
		    	logger.info("The BOM dependencies are already sorted!");
		    	return;
		    }

		    deps.values().stream().forEach(n -> {
		    	n.prefix.forEach(mgmDependenciesNode::appendChild);
		    	mgmDependenciesNode.appendChild(n.node);
		    });
		    
		    savePom(doc);

		} catch (Exception e) {
			throw new MojoExecutionException("An error occured while sorting BOM's artifacts", e);
		}
	}

	private static class PreFormattedNode {
		public Node node;
		public LinkedList<Node> prefix = new LinkedList<>();
	}

	private static class Gav implements Comparable<Gav>{
		public String g;
		public String a;
		public String v;

		
		public Gav(String g, String a, String v) {
			this.g = g;
			this.a = a;
			this.v = v;
		}


		@Override
		public int compareTo(Gav other) {
		    int cmp = g.compareTo(other.g);
		    if (cmp == 0)
		        cmp = a.compareTo(other.a);
		    if (cmp == 0)
		        cmp = v.compareTo(other.v);
		    return cmp;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((g == null) ? 0 : g.hashCode());
			result = prime * result + ((v == null) ? 0 : v.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Gav other = (Gav) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (g == null) {
				if (other.g != null)
					return false;
			} else if (!g.equals(other.g))
				return false;
			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.equals(other.v))
				return false;
			return true;
		}


		@Override
		public String toString() {
			return "Gav [g=" + g + ", a=" + a + ", v=" + v + "]";
		}
	}

}
