/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.maven.enforcer.rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Implementation started off of
 * https://github.com/apache/maven-enforcer/blob/master/enforcer-rules/src/main/java/org/apache/maven/plugins/enforcer/RequireSameVersions.java
 */
public class RequireCorrelatedVersions implements EnforcerRule {

  private boolean requireOneMatch = true;

  private Set<String> correlations = new HashSet<String>();

  public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
//    Log log = helper.getLog();

    MavenProject project;
    try {
      project = (MavenProject) helper.evaluate("${project}");
    } catch (ExpressionEvaluationException ex) {
      throw new EnforcerRuleException("Unable to retrieve the MavenProject: ", ex);
    }

    Set<Artifact> artifacts = project.getArtifacts();
    Map<String, Artifact> artifactsById = new HashMap<String, Artifact>();
    for (Artifact artifact : artifacts) {
      if (artifactsById.containsKey(artifact.getDependencyConflictId())) {
        throw new EnforcerRuleException(
            artifact.getDependencyConflictId() + " has more than one version - resolve that first");
      }
      artifactsById.put(artifact.getDependencyConflictId(), artifact);
    }

    boolean foundMatch = false;
    for (String correlation : correlations) {
      String[] items = correlation.split(",");
      Artifact firstArtifact = artifactsById.get(getDependencyConfictId(items[0]));
      if (firstArtifact != null && firstArtifact.getBaseVersion().equals(getVersion(items[0]))) {
        if (foundMatch) {
          throw new EnforcerRuleException("Found more than one rule for " + items[0]);
        }
        foundMatch = true;
        for (int i = 1; i < items.length; i++) {
          String fullId = items[i];
          String dependencyConflictId = getDependencyConfictId(fullId);
          String thisVersion = getVersion(fullId);
          Artifact thisArtifact = artifactsById.get(dependencyConflictId);
          if (thisArtifact == null) {
            throw new EnforcerRuleException("Could not find dependency: " + dependencyConflictId);
          }
          if (!thisArtifact.getBaseVersion().equals(thisVersion)) {
            throw new EnforcerRuleException("For correlation: " + correlation + "\n"
                + "Found mismatching versions of " + dependencyConflictId + ": expected = "
                + thisVersion
                + ", actual = " + thisArtifact.getBaseVersion());
          }
        }
      }
    }

    if (requireOneMatch && !foundMatch) {
      throw new EnforcerRuleException("requireOneMatch true, but found no matching dependencies");
    }
  }

  private static String getDependencyConfictId(String fullId) {
    return fullId.substring(0, fullId.lastIndexOf(':'));
  }

  private static String getVersion(String fullId) {
    return fullId.substring(fullId.lastIndexOf(':') + 1);
  }

  public String getCacheId()
  {
    return "0";
  }

  public boolean isCacheable()
  {
    return false;
  }

  public boolean isResultValid( EnforcerRule cachedRule )
  {
    return false;
  }
}
