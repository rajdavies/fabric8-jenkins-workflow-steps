/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.fabric8.rest;

import hudson.Extension;
import hudson.console.AnnotatedLargeText;
import org.jenkinsci.plugins.fabric8.dto.BuildDTO;
import org.jenkinsci.plugins.fabric8.dto.NodeDTO;
import org.jenkinsci.plugins.fabric8.dto.StageDTO;
import org.jenkinsci.plugins.fabric8.support.Callback;
import org.jenkinsci.plugins.fabric8.support.FlowNodes;
import org.jenkinsci.plugins.fabric8.support.JSONHelper;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Extension
public class WorkflowRunAction extends ActionSupport<WorkflowRun> {
    public static String getUrl(WorkflowRun node) {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        String path = currentRequest.getContextPath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path + "fabric8/";
    }

    @Override
    public Class<WorkflowRun> type() {
        return WorkflowRun.class;
    }

    public Object doIndex() {
        return JSONHelper.jsonResponse(getTarget());
    }

    public Object doStages() {
        WorkflowRun run = getTarget();
        final BuildDTO buildDTO = BuildDTO.createBuildDTO(run.getParent(), run);
        if (run != null) {
            Callback<FlowNode> callback = new Callback<FlowNode>() {

                @Override
                public void invoke(FlowNode node) {
                    StageDTO stage = StageDTO.createStageDTO(node);
                    if (stage != null) {
                        buildDTO.addStage(stage);
                    }
                }
            };
            FlowNodes.forEach(run.getExecution(), callback);
            FlowNodes.sortInStageIdOrder(buildDTO.getStages());
        }
        return JSONHelper.jsonResponse(buildDTO);
    }

    public Object doNodes() {
        WorkflowRun run = getTarget();
        List<NodeDTO> answer = new ArrayList<NodeDTO>();
        if (run != null) {
            List<FlowNode> nodes = FlowNodes.getSortedFlowNodes(run.getExecution());
            for (FlowNode node : nodes) {
                NodeDTO dto = NodeDTO.createNodeDTO(node);
                if (dto != null) {
                    answer.add(dto);
                }
            }
        }
        return JSONHelper.jsonResponse(answer);
    }

    public Object doLog(Integer maxLineParam) throws IOException {
        System.out.println("Invoke log with maxLineParam: " + maxLineParam);
        WorkflowRun run = getTarget();
        // TODO take this from a query argument
        int maxLines = 5000;
        AnnotatedLargeText logText = run.getLogText();
        List<String> lines = run.getLog(maxLines);
        return JSONHelper.jsonResponse(lines);
    }

    @Nonnull
    @Override
    public Collection<WorkflowRunAction> createFor(WorkflowRun target) {
        try {
            WorkflowRunAction action = getClass().newInstance();
            action.setTarget(target);
            List<WorkflowRunAction> answer = new ArrayList<WorkflowRunAction>();
            answer.add(action);
            return answer;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String getUrlName() {
        return "fabric8";
    }


}
