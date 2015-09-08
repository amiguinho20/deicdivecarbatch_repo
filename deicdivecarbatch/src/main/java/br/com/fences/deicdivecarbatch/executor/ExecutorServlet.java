package br.com.fences.deicdivecarbatch.executor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchRuntime;
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
					long jid = jo.start(job, new Properties());
					out.println("Job submetido: " + jid + "<br>");
					out.println("Status: " + jo.getJobExecution(jid).getBatchStatus() + "<br>");
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
