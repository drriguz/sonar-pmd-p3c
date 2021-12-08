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
package com.sonar.it.java.suite.orchestrator;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.MavenLocation;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import java.io.File;
import java.util.List;

import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static com.sonar.orchestrator.locator.FileLocation.byWildcardMavenFilename;
import static com.sonar.orchestrator.locator.FileLocation.ofClasspath;

/**
 * Wraps the {@link Orchestrator} and replaces deprecated methods with a different implementation.
 */
public class PmdTestOrchestrator {

    private static final String SONAR_JAVA_PLUGIN_VERSION_KEY = "test.sonar.plugin.version.java";
    private static final String SONAR_VERSION_KEY = "test.sonar.version";
    private static final String LANGUAGE_KEY = "java";

    private final Orchestrator delegate;

    private PmdTestOrchestrator(Orchestrator delegate) {
        this.delegate = delegate;
    }

    public void resetData(String project) {
        SonarClient
                .builder()
                .url(delegate.getServer().getUrl())
                .login(ADMIN_LOGIN)
                .password(ADMIN_PASSWORD)
                .connectTimeoutMilliseconds(300_000)
                .readTimeoutMilliseconds(600_000)
                .build()
                .post("/api/projects/delete?project=" + deriveProjectKey(project));
    }

    public void start() {
        delegate.start();
    }

    public BuildResult executeBuild(MavenBuild build) {
        return delegate.executeBuild(build);
    }

    public List<Issue> retrieveIssues(IssueQuery query) {
        return SonarClient.create(delegate.getServer().getUrl())
                .issueClient()
                .find(query)
                .list();
    }

    public void associateProjectToQualityProfile(String profile, String project) {
        final String projectKey = deriveProjectKey(project);
        delegate.getServer().provisionProject(projectKey, project);
        delegate.getServer().associateProjectToQualityProfile(projectKey, LANGUAGE_KEY, profile);
    }

    public static PmdTestOrchestrator init() {
        final Orchestrator orchestrator = Orchestrator
                .builderEnv()
                .setSonarVersion(determineSonarqubeVersion())
                .addPlugin(MavenLocation.create(
                        "org.sonarsource.java",
                        "sonar-java-plugin",
                        determineJavaPluginVersion()
                ))
                .addPlugin(MavenLocation.create(
                        "org.sonarsource.pmd",
                        "sonar-pmd-plugin",
                        "3.3.1"
                ))
                .addPlugin(byWildcardMavenFilename(new File("../sonar-p3c-plugin/target"), "sonar-p3c-plugin-*.jar"))
                .restoreProfileAtStartup(ofClasspath("/com/sonar/it/java/PmdTest/pmd-junit-rules.xml"))
                .restoreProfileAtStartup(ofClasspath("/com/sonar/it/java/PmdTest/pmd-backup.xml"))
                .restoreProfileAtStartup(ofClasspath("/com/sonar/it/java/PmdTest/pmd-all-rules.xml"))
                .restoreProfileAtStartup(ofClasspath("/com/sonar/it/java/PmdTest/pmd-p3c.xml"))
                .build();

        return new PmdTestOrchestrator(orchestrator);
    }

    private static String deriveProjectKey(String projectName) {
        return String.format("com.sonarsource.it.projects:%s", projectName);
    }

    private static String determineJavaPluginVersion() {
        return System.getProperty(SONAR_JAVA_PLUGIN_VERSION_KEY, "DEV");
    }

    private static String determineSonarqubeVersion() {
        return System.getProperty(SONAR_VERSION_KEY, "LATEST_RELEASE[7.9]");
    }
}