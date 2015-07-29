package br.com.fences.deicdivecarbatch.roubocarga;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.batch.api.chunk.AbstractItemReader;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import br.com.fences.deicdivecarbatch.config.AppConfig;
import br.com.fences.deicdivecarbatch.roubocarga.tratamentoerro.VerificarErro;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.ocorrenciaentidade.chave.OcorrenciaChave;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


@Named
public class ExtratorItemReader extends AbstractItemReader {

	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;
	
	private Gson gson = new GsonBuilder().create();
	
	private List<OcorrenciaChave> ocorrenciasChaves = new ArrayList<>();
	private Iterator<OcorrenciaChave> iteratorOcorrenciaChave;
	
	private String host;
	private String port;
	
	@Override
	public void open(Serializable checkpoint) throws Exception {
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();
		
		logger.info("Recuperar ultima data de registro carregada...");
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + "/deicdivecarbackend/rest/" + 
				"rouboCarga/pesquisarUltimaDataRegistroNaoComplementar";
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

		Type collectionType = new TypeToken<List<OcorrenciaChave>>(){}.getType();
		ocorrenciasChaves = gson.fromJson(json, collectionType);
//		ocorrenciasChaves.add(new OcorrenciaChave("10374", "2015", "5552", "20150617000757"));
//		ocorrenciasChaves.add(new OcorrenciaChave("10374", "2015", "5553", "20150617002747"));
		iteratorOcorrenciaChave = ocorrenciasChaves.iterator();
		logger.info("Foram lidas [" + ocorrenciasChaves.size() + "] chaves para carga.");
		
	}
	
	/**
	 * O container ira parar de chamar esse metodo quando retornar nulo.
	 */
	@Override
	public OcorrenciaChave readItem() throws Exception 
	{
		OcorrenciaChave ocorrenciaChave = null;
		if (iteratorOcorrenciaChave.hasNext())
		{
			ocorrenciaChave = iteratorOcorrenciaChave.next();
		}
		if (ocorrenciaChave == null)
		{
			logger.info("Nao existe mais registro para leitura. Termino do Job.");
		}
		
		return ocorrenciaChave;
	}

}
