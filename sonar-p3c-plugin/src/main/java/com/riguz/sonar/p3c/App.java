package com.riguz.sonar.p3c;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) throws IOException, JDOMException {
        System.out.println("Generating sonar rules automatically...");
        String xml = createRulesXml();
        System.out.println(xml);
    }

    private static String createRulesXml() throws IOException, JDOMException {
        Element root = new Element("rules");
        XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());

        List<String> ruleDefs = List.of(
                "/rulesets/java/ali-comment.xml",
                "/rulesets/java/ali-concurrent.xml",
                "/rulesets/java/ali-constant.xml",
                "/rulesets/java/ali-exception.xml",
                "/rulesets/java/ali-flowcontrol.xml",
                "/rulesets/java/ali-naming.xml",
                "/rulesets/java/ali-oop.xml",
                "/rulesets/java/ali-orm.xml",
                "/rulesets/java/ali-other.xml",
                "/rulesets/java/ali-set.xml");
        List<Element> rules = ruleDefs.stream()
                .map(rule -> {
                    RuleConvertor convertor = new RuleConvertor(rule);
                    try {
                        return convertor.convert();
                    } catch (IOException | JDOMException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());

        root.addContent(rules);
        System.out.println(rules.size());
        return xmlOutput.outputString(root);
    }
}
