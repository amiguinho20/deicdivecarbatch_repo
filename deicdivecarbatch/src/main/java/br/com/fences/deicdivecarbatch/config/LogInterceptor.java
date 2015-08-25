package br.com.fences.deicdivecarbatch.config;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Log
@Interceptor
public class LogInterceptor {

	DateFormat df = new SimpleDateFormat("HH:mm:ss");
	Logger logger;

	@AroundInvoke
	public Object log(InvocationContext ctx) throws Exception {

		logger = LogManager.getLogger(ctx.getTarget().getClass().getSuperclass().getName());

		long tempoInicial = Calendar.getInstance().getTimeInMillis();
		Object obj = null;

		try {
			obj = ctx.proceed();
		} catch (Exception e) {
			logar(ctx, tempoInicial, e);
			throw e;
		}
		logar(ctx, tempoInicial, null);

		return obj;
	}

	private void logar(InvocationContext ctx, long tempoInicial, Exception e) {
		String duracao = calcularTempo(tempoInicial);
		String msg = null;
		if (e == null) {
			msg = "Metodo " + ctx.getMethod().getName()
					+ " parametros "
					+ Arrays.deepToString(ctx.getParameters()) + ", duracao ["
					+ duracao + "].";
			logger.info(msg);
		} else {
			msg = "Erro [" + e.getMessage() + "] metodo "
					+ ctx.getMethod().getName() + " parametros "
					+ Arrays.deepToString(ctx.getParameters()) + ", duracao ["
					+ duracao + "].";
			logger.error(msg, e);
		}
	}

	private String calcularTempo(long tempoInicial) {
		long tempoFinal = System.currentTimeMillis();
		long duracaoLong = tempoFinal - tempoInicial;
		
		long segundos = duracaoLong / 1000;
		long minutos = segundos / 60;
		long horas = minutos / 60;
		
		String duracao = String.format("%02d:%02d:%02d", horas, minutos, segundos);
		
		return duracao;
	}

}