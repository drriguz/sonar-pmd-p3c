/*
 * SonarQube PMD Plugin Integration Test
 * Copyright (C) 2013-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.riguz.sonar.p3c;

import org.apache.commons.io.IOUtils;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class PmdExtensionRepository implements RulesDefinition {

    private static final Logger LOGGER = Loggers.get(PmdExtensionRepository.class);

    // Must be the same than the PMD plugin
    private static final String REPOSITORY_KEY = "pmd";
    private static final String LANGUAGE_KEY = "java";

    private static final List<String> ruleDefs = List.of(
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

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY);

        String rulesXml;


        try {
            rulesXml = createRulesXml();
        } catch (IOException | JDOMException e) {
            LOGGER.error("Failed to read p3c ruleset", e);
            throw new RuntimeException(e);
        }

        try (InputStream inputStream = IOUtils.toInputStream(rulesXml, StandardCharsets.UTF_8)) {
            new RulesDefinitionXmlLoader()
                    .load(
                            repository,
                            inputStream,
                            StandardCharsets.UTF_8
                    );
        } catch (IOException e) {
            LOGGER.error("Failed to load PMD RuleSet.", e);
        }

        repository.done();
    }

    private String createRulesXml() throws IOException, JDOMException {
        Element root = new Element("rules");
        XMLOutputter xmlOutput = new XMLOutputter();

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
        return xmlOutput.outputString(root);
    }
}
