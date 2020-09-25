package com.converter.service.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.stereotype.Service;

import com.converter.factories.Converter;
import com.converter.factories.ConverterFactories;
import com.converter.service.IFLVService;
import com.converter.util.Des;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * FLV流转换
 * 
 * @author lizhiyong
 * @date 下午5:06:46
 */
@Slf4j
@Service
public class FLVService implements IFLVService {

	private Map<String, Converter> converters = new HashMap<>();
	/**
	 * 密钥
	 */
	private static final String DES_KEY = "W1ses0ft";
	/**
	 * 编码
	 */
	private static Charset charset = Charset.forName("utf-8");

	@Override
	public void open(String url, HttpServletResponse response, HttpServletRequest request) {
		String key = MD5Encoder.encode(url.getBytes());
		AsyncContext async = request.startAsync();
		async.setTimeout(0);
		if (converters.containsKey(key)) {
			Converter c = converters.get(key);
			try {
				c.addOutputStreamEntity(key, async);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				throw new IllegalArgumentException(e.getMessage());
			}
		} else {
			List<AsyncContext> outs = Lists.newArrayList();
			outs.add(async);
			ConverterFactories c = new ConverterFactories(url, key, converters, outs);
			c.start();
			converters.put(key, c);
		}
		response.setContentType("video/x-flv");
		response.setHeader("Connection", "keep-alive");
		response.setStatus(HttpServletResponse.SC_OK);
		try {
			response.flushBuffer();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public String encode(String url) {
		return Des.encryptString(url, charset, DES_KEY);
	}

	@Override
	public String decode(String url) {
		return Des.decryptString(url, charset, DES_KEY);
	}

}
