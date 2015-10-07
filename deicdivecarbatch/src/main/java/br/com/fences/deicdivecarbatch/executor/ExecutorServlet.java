package br.com.fences.deicdivecarbatch.executor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.com.fences.fencesutils.verificador.Verificador;

/**
 * Servlet implementation class Teste
 */
@WebServlet("/ExecutorServlet")
public class ExecutorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("text/html;charset=UTF-8");
		
		String abortar = request.getParameter("abortar");
		String job = request.getParameter("job");
		
		try (PrintWriter out = response.getWriter()) {
			JobOperator jo = BatchRuntime.getJobOperator();
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Invocação de Job - Servlet</title>");
			out.println("</head>");
			out.println("<body>");

			if (Verificador.isValorado(abortar))
			{
				try 
				{
					long jid = Long.parseLong(abortar);    
					out.println("Status antes: " + jo.getJobExecution(jid).getBatchStatus() + "<br>");
					//jo.abandon(jid);
					jo.stop(jid);
					out.println("Status depois: " + jo.getJobExecution(jid).getBatchStatus() + "<br>");
				}catch(Exception e)
				{
					out.println("Erro: " + e.getMessage() + "<br>");
				}
						
			}
			else
			{
				if (Verificador.isValorado(job)){
					
					//jo.getJobInstances(jobName, start, count);
					
					String jobXmlId = Character.toLowerCase(job.charAt(0)) + job.substring(1, job.length());
					
					List<Long> idRunningExecutions = null;
					try{
						idRunningExecutions = jo.getRunningExecutions(jobXmlId);
					} catch (NoSuchJobException e )
					{
						out.println("NoSuchJobException [" + jobXmlId + "]: " + e.getMessage()  + "<br>");
					}
					out.println("execucoes para o [" + jobXmlId + "]: " + idRunningExecutions  + "<br>");

					List<JobInstance> instances = null;
					try{
						instances = jo.getJobInstances(jobXmlId, 0, 10);
					}
					catch (NoSuchJobException e )
					{
						out.println("NoSuchJobException [" + jobXmlId + "]: " + e.getMessage()  + "<br>");
					}
					
					if (instances != null)
					{
						for (JobInstance instance : instances)
						{
							JobExecution execution = jo.getJobExecution(instance.getInstanceId());
							out.println("jobName: " + execution.getJobName() + "<br>");
							out.println("execId: " + execution.getExecutionId() + "<br>");
							out.println("status: " + execution.getBatchStatus() + "<br>");
							out.println("createTime: " + execution.getCreateTime() + "<br>");
							out.println("startTime: " + execution.getStartTime() + "<br>");
							out.println("endTime: " + execution.getEndTime() + "<br>");
							out.println("exitStatus: " + execution.getExitStatus() + "<br>");
							out.println("..." + "<br>");
						}
					}	
					
				
					if (idRunningExecutions == null)
					{
						long jid = jo.start(job, new Properties());
						out.println("Job submetido: " + jid + "<br>");
						out.println("Status: " + jo.getJobExecution(jid).getBatchStatus() + "<br>");
					}
					else
					{
						out.println("Job nao executado por ter um jah em execucao com o id: " + idRunningExecutions + "<br>");
					}
										
					
				}
				else
				{
					out.println("O parametro job esta nulo.<br>");
				}
			}
			
			
			
			out.println("</body>");
			out.println("</html>");
		} catch (JobStartException | JobSecurityException ex) {
			Logger.getLogger(ExecutorServlet.class.getName()).log(Level.SEVERE,
					null, ex);
		}
		
		
	}

}
