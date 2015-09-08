package br.com.fences.deicdivecarbatch.controleocorrencia;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.batch.api.AbstractBatchlet;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import br.com.fences.deicdivecarbatch.config.AppConfig;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.chave.OcorrenciaChave;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;

@Named
public class ControleOcorrenciaBatchlet extends AbstractBatchlet {

	@Inject 
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;
	
	@Inject
	private Converter<OcorrenciaChave> converterOcorrenciaChave;

	@Inject
	private Converter<ControleOcorrencia> converterControleOcorrencia;

	private String host;
	private String port;

	
	private Set<OcorrenciaChave> ocorrenciasChaves = new LinkedHashSet<>();

	
	public String process() throws Exception {

		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();

		
		//------------- seleciona ultima data de registro
		logger.info("Recuperar ultima data de registro carregada...");
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
				"controleOcorrencia/pesquisarUltimaDataRegistroNaoComplementar";
		WebTarget webTarget = client
				.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.get();
		String json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg); 
			throw new RuntimeException(msg);
		}
		String ultimaDataDeRegistro = json;  
		//TODO force2...
		if (!Verificador.isValorado(ultimaDataDeRegistro))
		{
			ultimaDataDeRegistro = "20100801000000";
		}
		
		
		logger.info("Ultima data de registro carregada: " + ultimaDataDeRegistro);
		logger.info("Montando periodo de pesquisa...");

		Date dtUltimaDataDeRegistro = FormatarData.getAnoMesDiaHoraMinutoSegundoConcatenados().parse(ultimaDataDeRegistro);
		Calendar calUltimaDataDeRegistro = Calendar.getInstance();
		calUltimaDataDeRegistro.setTime(dtUltimaDataDeRegistro);
		calUltimaDataDeRegistro.add(Calendar.SECOND, 1); //-- para nao selecionar o ultimo registro carregado.
		String dataRegistroInicial = FormatarData.getAnoMesDiaHoraMinutoSegundoConcatenados().format(calUltimaDataDeRegistro.getTime());
		logger.info("Ultima data de registro adicionado 1 segundo a mais: " + dataRegistroInicial);
		
		Calendar calDataCorrente = Calendar.getInstance();
		calDataCorrente.add(Calendar.HOUR_OF_DAY, -1); //-- ajuste de seguranca
		String dataCorrente = FormatarData
				.getAnoMesDiaHoraMinutoSegundoConcatenados().format(
						calDataCorrente.getTime());
		
		String dataRegistroFinal = dataCorrente;

		
		//--------------------- seleciona chaves
		//TODO force2...
		//dataRegistroFinal = "20101111103409";
		
		logger.info("Periodo de pesquisa inicial[" + dataRegistroInicial + "] final[" + dataRegistroFinal + "]");
		
		logger.info("Chamada ao servico de pesquisa de chaves de roubo de carga...");
		client = ClientBuilder.newClient();
		servico = "http://"
				+ appConfig.getOcorrenciaRdoBackendHost()
				+ ":"
				+ appConfig.getOcorrenciaRdoBackendPort()
				+ "/ocorrenciardobackend/rest/"
				+ "rdoextrair/pesquisarRouboDeCarga/{dataRegistroInicial}/{dataRegistroFinal}";
		webTarget = client.target(servico);
		response = webTarget
				.resolveTemplate("dataRegistroInicial", dataRegistroInicial)
				.resolveTemplate("dataRegistroFinal", dataRegistroFinal)
				.request(MediaType.APPLICATION_JSON)
				.get();
		json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg);
			throw new RuntimeException(msg);
		}

		Type collectionType = new TypeToken<Set<OcorrenciaChave>>(){}.getType();
		ocorrenciasChaves = (Set<OcorrenciaChave>) converterOcorrenciaChave.paraObjeto(json, collectionType);
//		ocorrenciasChaves.add(new OcorrenciaChave("20250", "2010", "4325", "20100801030816"));
//		ocorrenciasChaves.add(new OcorrenciaChave("10308", "2010", "1789", "20100920130105"));  
		logger.info("Foram lidas [" + ocorrenciasChaves.size() + "] chaves para carga.");

	
		//-- itera chaves
		int count = 0;
		for (OcorrenciaChave ocorrenciaChave : ocorrenciasChaves)
		{
			count++;
			
			ControleOcorrencia controleOcorrencia = new ControleOcorrencia();
			controleOcorrencia.setIdDelegacia(ocorrenciaChave.getIdDelegacia());
			controleOcorrencia.setNumBo(ocorrenciaChave.getNumBo());
			controleOcorrencia.setAnoBo(ocorrenciaChave.getAnoBo());
			controleOcorrencia.setComplementar(ocorrenciaChave.getComplementar());
			controleOcorrencia.setDatahoraRegistroBo(ocorrenciaChave.getDatahoraRegistroBo());
			
			client = ClientBuilder.newClient();
			servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
					"controleOcorrencia/adicionar";
			webTarget = client.target(servico);
			
			json = converterControleOcorrencia.paraJson(controleOcorrencia);
			response = webTarget
					.request(MediaType.APPLICATION_JSON)
					.put(Entity.json(json));
			json = response.readEntity(String.class);
			if (verificarErro.contemErro(response, json))
			{
				String msg = verificarErro.criarMensagem(response, json, servico);
				logger.error(msg);
				throw new RuntimeException(msg);
			}
			
			if ( (count % 5) == 0 )
			{
				logger.info("Processados " + count + "/" + ocorrenciasChaves.size());
			}
			
		}
			
		logger.info("Termino do processo.");
		return "COMPLETED";
	}
	
}