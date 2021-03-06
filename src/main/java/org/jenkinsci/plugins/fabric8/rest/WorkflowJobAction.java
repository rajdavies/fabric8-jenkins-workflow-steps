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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import hudson.Extension;
import org.jenkinsci.plugins.fabric8.dto.BuildDTO;
import org.jenkinsci.plugins.fabric8.dto.JobDTO;
import org.jenkinsci.plugins.fabric8.dto.JobMetricDTO;
import org.jenkinsci.plugins.fabric8.dto.StageDTO;
import org.jenkinsci.plugins.fabric8.support.Callback;
import org.jenkinsci.plugins.fabric8.support.FlowNodes;
import org.jenkinsci.plugins.fabric8.support.JSONHelper;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 */
@Extension
public class WorkflowJobAction extends ActionSupport<WorkflowJob> {
    public static String getUrl(WorkflowJob node) {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        String path = currentRequest.getContextPath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path + "fabric8";
    }

    @Override
    public Class<WorkflowJob> type() {
        return WorkflowJob.class;
    }

    public Object doIndex() {
        return JSONHelper.jsonResponse(getTarget());
    }

    public Object doStages() {
        Predicate<WorkflowRun> predicate = Predicates.alwaysTrue();
        List<WorkflowRun> runs = filterRuns(predicate);
        return filterStages(runs);
    }

    public Object doPendingStages() {
        Predicate<WorkflowRun> predicate = new Predicate<WorkflowRun>() {
            @Override
            public boolean apply(WorkflowRun run) {
                return run != null && run.isBuilding();
            }
        };
        // if there are no pending lets just return the last run
        List<WorkflowRun> runs = filterRuns(predicate);
        if (runs.isEmpty()) {
            WorkflowJob job = getTarget();
            if (job != null) {
                WorkflowRun build = job.getLastBuild();
                if (build != null) {
                    runs = Collections.singletonList(build);
                }
            }
        }
        return filterStages(runs);
    }

    public Object doMetrics() {
        JobMetricDTO dto = JobMetricDTO.createJobMetricsDTO(getTarget());
        return JSONHelper.jsonResponse(dto);
    }

    protected List<WorkflowRun> filterRuns(Predicate<WorkflowRun> predicate) {
        final List<WorkflowRun> answer = new ArrayList<WorkflowRun>();
        WorkflowJob job = getTarget();
        if (job != null) {
            WorkflowRun build = job.getLastBuild();
            while (build != null) {
                if (predicate.apply(build)) {
                    answer.add(build);
                }
                build = build.getPreviousBuild();
            }
        }
        return answer;
    }

    protected Object filterStages(Iterable<WorkflowRun> runs) {
        WorkflowJob job = getTarget();
        final JobDTO jobDTO = JobDTO.createJobDTO(job);
        if (job != null && jobDTO != null) {
            for (WorkflowRun build : runs) {
                final BuildDTO buildDTO = BuildDTO.createBuildDTO(job, build);
                jobDTO.addBuild(buildDTO);

                Callback<FlowNode> callback = new Callback<FlowNode>() {

                    @Override
                    public void invoke(FlowNode node) {
                        StageDTO stage = StageDTO.createStageDTO(node);
                        if (stage != null) {
                            buildDTO.addStage(stage);
                        }
                    }
                };
                FlowNodes.forEach(build.getExecution(), callback);
                FlowNodes.sortInStageIdOrder(buildDTO.getStages());
            }
        }
        return JSONHelper.jsonResponse(jobDTO);
    }


    @Nonnull
    @Override
    public Collection<WorkflowJobAction> createFor(WorkflowJob target) {
        try {
            WorkflowJobAction action = getClass().newInstance();
            action.setTarget(target);
            List<WorkflowJobAction> answer = new ArrayList<WorkflowJobAction>();
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
