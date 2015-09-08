package br.com.fences.deicdivecarbatch.indiciado;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import br.com.fences.deicdivecarbatch.config.AppConfig;
import br.com.fences.deicdivecarbatch.indiciado.to.IndiciadoCompostoTO;
import br.com.fences.deicdivecarentidade.indiciado.Indiciado;
import br.com.fences.deicdivecarentidade.indiciado.OcorrenciaReferencia;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.rest.tratamentoerro.util.VerificarErro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.pessoa.Pessoa;

@Named
public class IndiciadoItemProcessor implements ItemProcessor{
	
	@Inject
	private transient Logger logger;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private VerificarErro verificarErro;

	@Inject
	private Converter<Ocorrencia> ocorrenciaConverter;
	
	@Inject
	private Converter<Indiciado> indiciadoConverter;
	
	private String host;
	private String port;
	
	@Override
	public IndiciadoCompostoTO processItem(Object item) throws Exception 
	{
		ControleOcorrencia controleOcorrencia = (ControleOcorrencia) item;
		IndiciadoCompostoTO indiciadoCompostoTO = new IndiciadoCompostoTO();
		indiciadoCompostoTO.setControleOcorrencia(controleOcorrencia);
		
		host = appConfig.getServerBackendHost();
		port = appConfig.getServerBackendPort();
		
		logger.info("Extraindo... " + controleOcorrencia);
		Client client = ClientBuilder.newClient();
		String servico = "http://"
				+ host
				+ ":"
				+ port
				+ "/deicdivecarbackend/rest/"
				+ "rouboCarga/consultar/{id}";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.resolveTemplate("id", controleOcorrencia.getIdOcorrencia())
				.request(MediaType.APPLICATION_JSON)
				.get();
		String json = response.readEntity(String.class);
		if (verificarErro.contemErro(response, json))
		{
			String msg = verificarErro.criarMensagem(response, json, servico);
			logger.error(msg);
			throw new RuntimeException(msg);
		}
		Ocorrencia ocorrencia = ocorrenciaConverter.paraObjeto(json, Ocorrencia.class);

		for (Pessoa pessoa : ocorrencia.getPessoas())
		{
			if (pessoa.getIdTipoPessoa().equals("1"))
			{	//-- indiciado
				if (Verificador.isValorado(pessoa.getNomePessoa()) && 
						Verificador.isValorado(pessoa.getNomemaePessoa()) && 
						Verificador.isValorado(pessoa.getRg())) 
				{
					client = ClientBuilder.newClient();
					servico = "http://"
							+ host
							+ ":"
							+ port
							+ "/deicdivecarbackend/rest/"
							+ "indiciado/consultar/{nome}/{nomeDaMae}/{rg}";
					webTarget = client.target(servico);
					response = webTarget
							.resolveTemplate("nome", pessoa.getNomePessoa())
							.resolveTemplate("nomeDaMae", pessoa.getNomemaePessoa())
							.resolveTemplate("rg", pessoa.getRg())
							.request(MediaType.APPLICATION_JSON)
							.get();
					json = response.readEntity(String.class);
					if (verificarErro.contemErro(response, json))
					{
						String msg = verificarErro.criarMensagem(response, json, servico);
						logger.error(msg);
						throw new RuntimeException(msg);
					}
					Indiciado indiciado = indiciadoConverter.paraObjeto(json, Indiciado.class);
					if (indiciado == null || !Verificador.isValorado(indiciado.getId()))
					{	//-- novo
						indiciado = new Indiciado();
						indiciado.setNome(pessoa.getNomePessoa());
						indiciado.setNomeDaMae(pessoa.getNomemaePessoa());
						indiciado.setRg(pessoa.getRg());
					}
					if (Verificador.isValorado(pessoa.getRgUf()))
					{
						indiciado.setRgUf(pessoa.getRgUf());
					}
					if (Verificador.isValorado(pessoa.getRgDataEmissao()))
					{
						indiciado.setRgDataEmissao(pessoa.getRgDataEmissao());
					}
					if (Verificador.isValorado(pessoa.getNaturalidadePessoa()))
					{
						indiciado.setNaturalidade(pessoa.getNaturalidadePessoa());
					}
					if (Verificador.isValorado(pessoa.getNacionalidadePessoa()))
					{
						indiciado.setNacionalidade(pessoa.getNacionalidadePessoa());
					}
					if (Verificador.isValorado(pessoa.getCpf()))
					{
						indiciado.setCpf(pessoa.getCpf());
					}
					OcorrenciaReferencia ocorrenciaReferencia = new OcorrenciaReferencia();
					ocorrenciaReferencia.setId(ocorrencia.getId());
					ocorrenciaReferencia.setNumBo(ocorrencia.getNumBo());
					ocorrenciaReferencia.setAnoBo(ocorrencia.getAnoBo());
					ocorrenciaReferencia.setIdDelegacia(ocorrencia.getIdDelegacia());
					ocorrenciaReferencia.setNomeDelegacia(ocorrencia.getNomeDelegacia());
					ocorrenciaReferencia.setDatahoraRegistroBo(ocorrencia.getDatahoraRegistroBo());
					indiciado.getOcorrencias().add(ocorrenciaReferencia);
					
					indiciadoCompostoTO.getIndiciados().add(indiciado);
				}
			}
		}

		return indiciadoCompostoTO;
	}
}
