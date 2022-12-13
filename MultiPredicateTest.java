import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class MultiPredicateTest {

	private static final String XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<doc>\n"
			+ "<item entityID='http://aaaa'>starts with http</item>\n"
			+ "<item entityID='urn:mace:glark'>starts with urn:mace:</item>\n"
			+ "<item entityID='xxx'>no predicate matches</item>\n"
			+ "</doc>\n";

	private static final String XPATH =
			"//item" +
			"[not(starts-with(@entityID, 'urn:mace:'))]" +
	        "[not(starts-with(@entityID, 'http://'))]" +
	        "[not(starts-with(@entityID, 'https://'))]";

	private static void performXPathTest() throws Exception {
		System.out.println("Performing XPath test.");
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile(XPATH);
		InputSource source = new InputSource(new StringReader(XML));
		Object result = expr.evaluate(source, XPathConstants.NODESET);
		NodeList nodes = (NodeList)result;
		int num = nodes.getLength();
		System.out.println("Nodes found: " + num + " (expected: 1)");
		for (int i = 0; i < num; i++) {
			Node node = (Node)nodes.item(i);
			System.out.println("Node " + i + ": " + node.getTextContent() +
					" " + node.getAttributes().getNamedItem("entityID"));
		}
		System.out.println();
	}

	private static final String XSLT =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xsl:stylesheet version=\"1.0\"\n"
			+ "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
			+ "    xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n"
			+ "    xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\">\n"
			+ "    <xsl:template match=\"//item"
			+ "        [not(starts-with(@entityID, 'urn:mace:'))]\n"
			+ "        [not(starts-with(@entityID, 'http://'))]\n"
			+ "        [not(starts-with(@entityID, 'https://'))]\">\n"
			+ "        <problem><xsl:value-of select='@entityID'/>: <xsl:value-of select='.'/></problem>\n"
			+ "    </xsl:template>\n"
			+ "</xsl:stylesheet>\n"
			+ "";

	private static void performXSLTTest() throws Exception {
		System.out.println("Performing XSLT test.");

		// Make a transformer based on the XSLT.
		TransformerFactory factory = TransformerFactory.newInstance();
		Templates templates = factory.newTemplates(new StreamSource(new StringReader(XSLT)));
		Transformer transformer = templates.newTransformer();

		// Put the result of the transform into a document fragment.
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();
		DOMResult result = new DOMResult(doc.createDocumentFragment());

		transformer.transform(new StreamSource(new StringReader(XML)), result);
		NodeList children = result.getNode().getChildNodes();
		System.out.println("Nodes: " + children.getLength());

		// Filter out just the elements from the result.
		List<Element> elements = new ArrayList<>();
		for (int i = 0 ; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				elements.add((Element)node);
			}
		}

		// What do we have?
		System.out.println("Elements: " + elements.size() + " (expected: 1)");
		for (Element e : elements) {
			System.out.println("Element: " + e.getLocalName() + ": " + e.getTextContent());
		}
		System.out.println();
	}

	public static void main(String[] args) throws Exception {
		performXPathTest();
		performXSLTTest();
	}

}
