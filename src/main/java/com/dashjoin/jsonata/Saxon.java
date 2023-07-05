package com.dashjoin.jsonata;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.xpath.XPathFactoryImpl;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class Saxon {

    public static void main(String[] args) {
        String s = "<date>2005-12-31</date>";
        //String expression = "string(format-date(//date, '[D01].[M01].[Y0001]'))";
        //String expression = "string(format-date(//date, '[Y0001]-W[W01]-[F1]'))";
        String expression = "string(format-date(//date, '[Y]-[M]-[D]'))";
        //String expression = "format-number(0.1, 'AeAAA')"; //, {'zero-digit': 'A'})";

        
        SaxonXPath xp = new SaxonXPath(s, expression);
        String res = xp.getxPathResult();
        System.out.println(res);
    }

    public static void main2(String[] args) {

        String xml = "";
        String xPathStatement = "";
        String xPathResult = "";
        SaxonXPath xPathEvaluation = null;
        Boolean xPathResultMatch = false;

        xml="<root version = '1.0' encoding = 'UTF-8' xmlns:bar='http://www.smth.org/'><bar:a>#BBB#</bar:a><a>#CCC#</a><b><a>#DDD#</a></b></root>";

        //I'm using the following XPath Tester for test scenarios
        //https://www.freeformatter.com/xpath-tester.html#ad-output
        // Test #1
        xPathStatement="/root/a";

        xPathEvaluation = new SaxonXPath(xml, xPathStatement);

        xPathResult = xPathEvaluation.getxPathResult();
            System.out.println("Test #1 xPathResult - " + xPathResult);
            //xPathResult == "<a version = '1.0' encoding = 'UTF-8'>#BBB#</a><a>#CCC#</a>";
        xPathResultMatch = xPathEvaluation.getxPathResultMatch();
            System.out.println("Test #1 xPathResultMatch - " + xPathResultMatch);
            //xPathResultMatch == true;

        // Test #2
        xPathStatement="//a";
        xPathEvaluation.Reset(xml, xPathStatement);
        xPathResult = xPathEvaluation.getxPathResult();
            System.out.println("Test #2 xPathResult - " + xPathResult);
            //xPathResult == "<a version = '1.0' encoding = 'UTF-8'>#BBB#</a><a>#CCC#</a><a>#DDD#</a>";
        xPathResultMatch = xPathEvaluation.getxPathResultMatch();
            System.out.println("Test #2 xPathResultMatch - " + xPathResultMatch);
            //xPathResultMatch == true;

        // Test #3
        xPathStatement="/root/a[1]/text()";
        xPathEvaluation.Reset(xml, xPathStatement);
        xPathResult = xPathEvaluation.getxPathResult();
            System.out.println("Test #3 xPathResult - " + xPathResult);
            //xPathResult == "#BBB#";
        xPathResultMatch = xPathEvaluation.getxPathResultMatch();
            System.out.println("Test #3 xPathResultMatch - " + xPathResultMatch);
            //xPathResultMatch == true;

        // Test #4
        xPathStatement="/a/root/a/text()";
        xPathEvaluation.Reset(xml, xPathStatement);
        xPathResult = xPathEvaluation.getxPathResult();
            System.out.println("Test #4 xPathResult - " + xPathResult);
            //xPathResult == "";
        xPathResultMatch = xPathEvaluation.getxPathResultMatch();
            System.out.println("Test #4 xPathResultMatch - " + xPathResultMatch);
            //xPathResultMatch == false;

        // Test #5
        xPathStatement="/root";
        xPathEvaluation.Reset(xml, xPathStatement);
        xPathResult = xPathEvaluation.getxPathResult();
            System.out.println("Test #5 xPathResult - " + xPathResult);
            //xPathResult == "<root><a version = '1.0' encoding = 'UTF-8'>#BBB#</a><a>#CCC#</a><b><a>#DDD#</a></b></root>";
        xPathResultMatch = xPathEvaluation.getxPathResultMatch();
            System.out.println("Test #5 xPathResultMatch - " + xPathResultMatch);
            //xPathResultMatch == true;         
    }
    static class SaxonXPath{
        private String xml;
        private String xPathStatement;
        private String xPathResult;
        private Boolean xPathResultMatch;
        private XPathFactory xPathFactory;
        private XPath xPath;
        public SaxonXPath(String xml, String xPathStatement){
            System.setProperty("javax.xml.xpath.XPathFactory:" + NamespaceConstant.OBJECT_MODEL_SAXON, "net.sf.saxon.xpath.XPathFactoryImpl");
            try {
                this.xPathFactory = XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON);
            } catch (XPathFactoryConfigurationException e) {
                e.printStackTrace();
            }
            this.xPath = this.xPathFactory.newXPath();
            this.Reset(xml, xPathStatement);
        }
        public void Reset(String xml, String xPathStatement){
            this.xml = xml;
            this.xPathStatement = xPathStatement;
            this.xPathResult = "";
            this.xPathResultMatch = null;
            try{                
                InputSource inputSource = new InputSource(new StringReader(this.xml));
                SAXSource saxSource = new SAXSource(inputSource);
                Configuration config = ((XPathFactoryImpl) this.xPathFactory).getConfiguration();
                TreeInfo document = config.buildDocumentTree(saxSource);
                XPathExpression xPathExpression = this.xPath.compile(this.xPathStatement);
                List<NodeInfo> matches = (List<NodeInfo>) xPathExpression.evaluate(document, XPathConstants.NODESET);
                if (matches != null && matches.size()>0) {
                    this.xPathResultMatch = true;   
                    for (Iterator<NodeInfo> iter = matches.iterator(); iter.hasNext();) {
                        Object no = iter.next();
                        if (no instanceof String)
                            xPathResult += no;
                        else if (no instanceof NodeInfo) {
                            NodeInfo node = (NodeInfo) no;

                            xPathResult += net.sf.saxon.query.QueryResult.serialize(node);
                        } else {
                            System.err.println("ERROR - unexpected XPath type "+no);
                        }
                    }
                } else {
                    this.xPathResultMatch = false;
                }
            } catch(Exception e){
                e.printStackTrace();
            }           
        }
        public String getxPathResult(){
            return this.xPathResult;
        }
        public Boolean getxPathResultMatch(){
            return this.xPathResultMatch;
        }
    }
}