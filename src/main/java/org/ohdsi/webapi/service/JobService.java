package org.ohdsi.webapi.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.job.JobInstanceResource;
import org.ohdsi.webapi.job.JobUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
@Path("/job/")
public class JobService extends AbstractDaoService {
    
    @Autowired
    private String batchTablePrefix;
    
    @Autowired
    private JobExplorer jobExplorer;
    
    @GET
    @Path("{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobInstanceResource findJob(@PathParam("jobId") final Long jobId) {
        final JobInstance job = this.jobExplorer.getJobInstance(jobId);
        if (job == null) {
            return null;//TODO #8 conventions under review
        }
        return JobUtils.toJobInstanceResource(job);
    }
    
    @GET
    @Path("{jobId}/execution/{executionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobExecutionResource findJobExecution(@PathParam("jobId") final Long jobId,
                                                 @PathParam("executionId") final Long executionId) {
        return service(jobId, executionId);
    }
    
    /**
     * Overloaded findJobExecution method.
     * 
     * @param executionId
     * @return
     */
    @GET
    @Path("/execution/{executionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobExecutionResource findJobExecution(@PathParam("executionId") final Long executionId) {
        return service(null, executionId);
    }
    
    private JobExecutionResource service(Long jobId, Long executionId) {
        final JobExecution exec = this.jobExplorer.getJobExecution(executionId);
        if (exec == null || (jobId != null && !jobId.equals(exec.getJobId()))) {
            return null;//TODO #8 conventions under review
        }
        return JobUtils.toJobExecutionResource(exec);
    }
}
