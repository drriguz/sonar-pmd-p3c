package com.riguz.sonar.p3c;

import com.alibaba.p3c.pmd.I18nResources;
import org.jdom2.*;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.sonar.api.rule.Severity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleConvertor {
    private final String rulePath;

    public RuleConvertor(String rulePath) {
        this.rulePath = rulePath;
    }

    private static final Map<String, String> SEVERITY_MAPPING
            = Map.of(
            "1", Severity.BLOCKER,
            "2", Severity.CRITICAL,
            "3", Severity.MAJOR,
            "4", Severity.MINOR,
            "5", Severity.INFO);

    public List<Element> convert() throws IOException, JDOMException {
        Document document = new SAXBuilder().build(RuleConvertor.class.getResourceAsStream(rulePath));

        String configKeyPrefix = rulePath.substring(1);

        return document.getRootElement().getChildren()
                .stream()
                .filter(e -> e.getName().equals("rule"))
                .map(e -> {
                    String name = e.getAttributeValue("name");
                    String configKey = configKeyPrefix + "/" + name;
                    String priority = getContent(e, "priority");
                    String severity = SEVERITY_MAPPING.get(priority);
                    String ruleDescriptionKey = getContent(e, "description");
                    String example = getContent(e, "example");
                    String messageKey = e.getAttributeValue("message");
                    return createRule(
                            name,
                            name,
                            severity,
                            configKey,
                            buildRuleDescription(ruleDescriptionKey, example, messageKey));
                })
                .collect(Collectors.toList());
    }

    private String buildRuleDescription(String ruleDescriptionKey, String example, String messageKey) {
        StringBuffer buffer = new StringBuffer();
        if (ruleDescriptionKey != null) {
            buffer.append(I18nResources.getMessage(ruleDescriptionKey));
            buffer.append("\n\n");
        } else {
            buffer.append(I18nResources.getMessage(messageKey));
            buffer.append("\n\n");
        }
        if (example != null) {
            buffer.append("Example:\n\n```");
            buffer.append(example);
            buffer.append("```\n");
        }

        return buffer.toString();
    }

    private String getContent(Element e, String name) {
        return e.getContent(new ElementFilter(name))
                .stream()
                .findFirst()
                .map(Element::getText)
                .orElse(null);
    }

    private Element createRule(String key, String name, String priority, String configKey, String description) {
        return new Element("rule")
                .addContent(new Element("key").setText(key))
                .addContent(new Element("name").setText(name))
                .addContent(new Element("priority").setText(priority))
                .addContent(new Element("configKey").setText(configKey))
                .addContent(new Element("description").setContent(new CDATA(description)))
                .addContent(new Element("descriptionFormat").setText("MARKDOWN"))
                .addContent(new Element("tag").setText("p3c"));
    }
}
