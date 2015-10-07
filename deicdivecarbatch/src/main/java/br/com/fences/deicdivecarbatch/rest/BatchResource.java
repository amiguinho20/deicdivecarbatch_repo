package br.com.fences.deicdivecarbatch.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.com.fences.fencesutils.conversor.converter.ColecaoJsonAdapter;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.fencesutils.rest.tratamentoerro.exception.RestRuntimeException;
import br.com.fences.fencesutils.verificador.Verificador;


@RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BatchResource {
	
	@Inject
	private transient Logger logger;
	
	private Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(Collection.class, new ColecaoJsonAdapter())
			.create();
	
	private enum Opcao {EXECUTAR, CONSULTAR, PARAR};
	

	@POST
	@Path("executar/{nomeJobXml}")
	public String executar(@PathParam("nomeJobXml") String nomeJobXml)
	{
		String json = executor(nomeJobXml, Opcao.EXECUTAR);
		return json;
	}
	
	@GET
	@Path("consultar/{nomeJobXml}")
	public String consultar(@PathParam("nomeJobXml") String nomeJobXml)
	{
		String json = executor(nomeJobXml, Opcao.CONSULTAR);
		return json;
	}
	
	@POST
	@Path("parar/{nomeJobXml}")
	public String parar(@PathParam("nomeJobXml") String nomeJobXml)
	{
		String json = executor(nomeJobXml, Opcao.PARAR);
		return json;
	}
	
	
	private String executor(String nomeJobXml, Opcao opcao)
	{
		if (!Verificador.isValorado(nomeJobXml)){
			throw new RestRuntimeException(1, "O parametro 'nomeJobXml' esta vazio.");
		}

		JobOperator jobOperator = BatchRuntime.getJobOperator();
		
		Map<String, String> execucao = new LinkedHashMap<>();
		
		long executionId = 0;
			
		if (opcao.equals(Opcao.EXECUTAR) || opcao.equals(Opcao.PARAR))
		{
			List<Long> idRunningExecutions = null;
			try
			{
				String jobXmlId = convertePrimeiroCaracterParaMinusculo(nomeJobXml);
				idRunningExecutions = jobOperator.getRunningExecutions(jobXmlId);
				if (Verificador.isValorado(idRunningExecutions))
				{
					Collections.sort(idRunningExecutions);
					Collections.reverse(idRunningExecutions);
					executionId = idRunningExecutions.get(0);
				}
			} 
			catch (NoSuchJobException e)
			{
				//-- nao ha job em execucao
			}
			
			if (opcao.equals(Opcao.EXECUTAR))
			{
				if (executionId == 0)
				{
					executionId = jobOperator.start(nomeJobXml, new Properties());
				}
			}
			if (opcao.equals(Opcao.PARAR))
			{
				if (executionId > 0)
				{
					try
					{
						jobOperator.stop(executionId);
					}
					catch(Exception e){}
					try
					{
						//-- abandonar o job para nao permitir restart
						jobOperator.abandon(executionId);
					}
					catch(Exception e){}
				}
				else
				{
					//-- nao ha job em execucao, executar a consulta do ultimo job
					opcao = Opcao.CONSULTAR;
				}
			}
		}
		if (opcao.equals(Opcao.CONSULTAR))
		{
			List<JobInstance> instances = null;
			try
			{
				String jobXmlId = convertePrimeiroCaracterParaMinusculo(nomeJobXml);
				
				instances = jobOperator.getJobInstances(jobXmlId, 0, 1);
				executionId = instances.get(0).getInstanceId();
			} 
			catch (NoSuchJobException e)
			{
				//-- nao ha job em execucao
			}
		}
		
		if (executionId > 0)
		{
			execucao = recuperarInformacaoJobExecution(executionId, jobOperator);
		}
		else
		{
			throw new RestRuntimeException(2, "Nao ha informacao de execucao para o job [" + nomeJobXml + "]");
		}
		
		String json = gson.toJson(execucao, LinkedHashMap.class);
		return json;
		
	}
	
	private String convertePrimeiroCaracterParaMinusculo(String original)
	{
		String retorno = Character.toLowerCase(original.charAt(0)) + original.substring(1, original.length());
		return retorno;
	}
	
	private Map<String, String> recuperarInformacaoJobExecution(long executionId, JobOperator jobOperator)
	{
		Map<String, String> execucao = new LinkedHashMap<>();
		
		JobExecution jobExecution = jobOperator.getJobExecution(executionId);
		
		execucao.put("jobName", jobExecution.getJobName());
		execucao.put("executionId", Long.toString(jobExecution.getExecutionId()));
		execucao.put("batchStatus", jobExecution.getBatchStatus().name());
		execucao.put("exitStatus", jobExecution.getExitStatus());
		if (jobExecution.getCreateTime() != null)
		{
			execucao.put("createTime", FormatarData.getDiaMesAnoComBarrasEHoraMinutoSegundoComDoisPontos().format(jobExecution.getCreateTime()));
		}
		if (jobExecution.getStartTime() != null)
		{
			execucao.put("startTime", FormatarData.getDiaMesAnoComBarrasEHoraMinutoSegundoComDoisPontos().format(jobExecution.getStartTime()));
		}
		if (jobExecution.getEndTime() != null)
		{
			execucao.put("endTime", FormatarData.getDiaMesAnoComBarrasEHoraMinutoSegundoComDoisPontos().format(jobExecution.getEndTime()));
		}
		
		return execucao;
	}
	    
}
