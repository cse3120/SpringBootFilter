package com.anz.cis.retail.excpetion;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.core.config.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import com.anz.cis.retail.constant.GateWayConstants;
import com.anz.cis.retail.util.NonValLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Order(1)
public class NonValueLoggerFilter implements Filter {

	private final static Logger LOG = LoggerFactory.getLogger(NonValueLoggerFilter.class);


	@Autowired
	  private ObjectMapper objectMapper;
	
	@Autowired
	private NonValLogger nonValLogger;
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		LOG.info("Logging Request");
				
		if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;

			HttpServletRequest requestToCache = new ContentCachingRequestWrapper(request);
			HttpServletResponse responseToCache = new ContentCachingResponseWrapper(response);
			chain.doFilter(requestToCache, responseToCache);
			String requestData = getRequestData(requestToCache);
			String responseData = getResponseData(responseToCache);
			String requestID=request.getHeader("requestId");
			String clientDate=request.getHeader("clientDate");
					String consumer=request.getHeader("consumer");
			getChLogMap(requestID,clientDate,consumer,requestData,responseData);
			System.out.println("End");
		} else {
			chain.doFilter(servletRequest, servletResponse);
			System.out.println("Else");
		}
	}
	
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

	private void getChLogMap(String requestID, String clientDate, String consumer, String requestData,
			String responseData) throws JsonProcessingException {
		String requestString = this.objectMapper.writeValueAsString(requestData);
		String responseString = this.objectMapper.writeValueAsString(responseData);

		final Map<String, Object> chLogData = new HashMap<>();
		chLogData.put(GateWayConstants.NV_CH_LOG_REQ_ID, Integer.parseInt(requestID));
		chLogData.put(GateWayConstants.NV_CH_LOG_SVC_NAME, GateWayConstants.CHANNEL_SVC_NAME);
		chLogData.put(GateWayConstants.NV_CH_LOG_CREATED_TS, new java.util.Date());
		chLogData.put(GateWayConstants.NV_CH_LOG_GUID, requestID);
		chLogData.put(GateWayConstants.NV_CH_LOG_CLIENT_DATE, new java.util.Date());
		chLogData.put(GateWayConstants.NV_CH_LOG_PARTY_ID, "");
		chLogData.put(GateWayConstants.NV_CH_LOG_REQ_BODY, requestString);
		chLogData.put(GateWayConstants.NV_CH_LOG_STATUS_KEY, GateWayConstants.NV_CH_LOG_STATUS_VALUE);
		chLogData.put(GateWayConstants.NV_CH_LOG_SC_NAME_KEY,
				consumer == null ? GateWayConstants.NV_CH_LOG_SERVICE_PROVIDER_IS_BLANK
						: consumer);
		chLogData.put(GateWayConstants.NV_CH_LOG_UPDATED_TS, new java.util.Date());
		chLogData.put(GateWayConstants.NV_CH_LOG_RESP_BODY, responseString);
		this.nonValLogger.logChMessage(chLogData);
		System.out.println("End of getChLogMap");
	}

	private static String getRequestData(final HttpServletRequest request) throws UnsupportedEncodingException {
		String payload = null;
		ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
			}
		}
		return payload;
	}

	private static String getResponseData(final HttpServletResponse response) throws IOException {
		String payload = null;
		ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response,
				ContentCachingResponseWrapper.class);
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
				wrapper.copyBodyToResponse();
			}
		}
		return payload;
	}

}