/**
 * Copyright 2019 Syncsort Inc. All Rights Reserved.
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.pem.utilities.sfg2pem.imp.plugins.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.pem.utilities.Configuration;
import com.ibm.pem.utilities.sfg2pem.ApiInvocationException;
import com.ibm.pem.utilities.sfg2pem.Constants;
import com.ibm.pem.utilities.sfg2pem.ImportException;
import com.ibm.pem.utilities.sfg2pem.ValidationException;
import com.ibm.pem.utilities.sfg2pem.imp.ImportHelper;
import com.ibm.pem.utilities.sfg2pem.imp.PartnerInfo;
import com.ibm.pem.utilities.util.ApiResponse;
import com.ibm.pem.utilities.util.DOMUtils;

public class SftpResourceHelper {

	private static final Logger LOG = LoggerFactory.getLogger(SftpResourceHelper.class);

	public static final String SUCCESS_STATUS_CODE_201 = "201";

	public static final String SUCCESS_STATUS_CODE_200 = "200";

	public static final String BAD_REQUEST_STATUS_CODE_400 = "400";

	public static final String CRETAE_PROFILECONFIGURATION_API_URL = "{PROTOCOL}://{PR.HOST_NAME}{:PR.PORT}/mdrws/sponsors/{CONTEXT_URI}/profileconfigurations/";

	public static boolean isInitiater(Document partnerInfo, String initiater) {
		String isInitiatingPartner = DOMUtils.getAttributeValueByTagName(partnerInfo, initiater, "code");
		String doesUseSSH = DOMUtils.getAttributeValueByTagName(partnerInfo, "doesUseSSH", "code");
		if (isInitiatingPartner != null && isInitiatingPartner.equalsIgnoreCase("true")
				&& doesUseSSH.equalsIgnoreCase("true")) {
			return true;
		} else {
			return false;
		}
	}

	public static Boolean isAttributeValueExist(Document doc, String tagName, String attributeName,
			String attributeValue) {
		Boolean isAttributeValueExist = false;
		NodeList elementsByTagName = doc.getElementsByTagName(tagName);
		for (int i = 0; i < elementsByTagName.getLength(); i++) {
			Node node = elementsByTagName.item(i);

			if (node.hasAttributes()) {
				Attr attr = (Attr) node.getAttributes().getNamedItem(attributeName);
				if (attr != null && attr.getValue().equals(attributeValue)) {
					isAttributeValueExist = true;
				}
			}
		}
		return isAttributeValueExist;
	}

	public static boolean doesPartnerProfileHasAUK(PartnerInfo partnerInfo) {
		String prodAuk = DOMUtils.getAttributeValueByTagName(partnerInfo.getProdSfgPartnerDoc(), "TradingPartner",
				"authorizedUserKeyName");

		String testAuk = DOMUtils.getAttributeValueByTagName(partnerInfo.getTestSfgPartnerDoc(), "TradingPartner",
				"authorizedUserKeyName");

		if (prodAuk != null && testAuk != null) {
			return true;
		}
		return false;
	}

	public static ApiResponse markAsComplete(String configResourceKey, Configuration config, String configResourceUri)
			throws ApiInvocationException, ImportException {
		ApiResponse apiOutput = null;
		String url = config.getPrRestURL() + (configResourceUri + configResourceKey + "/actions/markcomplete");
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);
		try {
			if (LOG.isInfoEnabled()) {
				LOG.info("Running API: POST " + url);
			}
			apiOutput = config.getResourceFactory().createHttpClientInstance().doPost(url, headers, null,
					config.getUserName(), config.getPassword(), config, config.getPrHostName());
			if (LOG.isInfoEnabled()) {
				ImportHelper.printApiResponse(apiOutput);
			}
			if (!apiOutput.getStatusCode().equals(SftpResourceHelper.SUCCESS_STATUS_CODE_200)) {
				String message = apiOutput.getResponse();
				boolean isResponsePresent = true;
				if (message == null || message.isEmpty()) {
					isResponsePresent = false;
					message = apiOutput.getStatusLine();
				}
				throw new ImportException(SftpResourceHelper.getErrorDescription(message, isResponsePresent));
			}
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ParserConfigurationException | SAXException
				| ValidationException e) {
			throw new ApiInvocationException(e);
		}
		return apiOutput;
	}

	public static ApiResponse getResourceFromPR(Configuration config, String url)
			throws ApiInvocationException, ImportException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);
		try {
			if (LOG.isInfoEnabled()) {
				LOG.info("Running API: GET " + url);
			}
			ApiResponse apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url, headers,
					config.getUserName(), config.getPassword(), config, config.getPrHostName());
			if (LOG.isInfoEnabled()) {
				LOG.info("Response:\n" + apiOutput.getStatusCode() + apiOutput.getResponse());
			}
			if (!apiOutput.getStatusCode().equals(SftpResourceHelper.SUCCESS_STATUS_CODE_200)) {
				String message = apiOutput.getResponse();
				boolean isResponsePresent = true;
				if (message == null || message.isEmpty()) {
					isResponsePresent = false;
					message = apiOutput.getStatusLine();
				}
				throw new ImportException(getErrorDescription(message, isResponsePresent));
			}
			return apiOutput;
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ParserConfigurationException | SAXException
				| ValidationException e) {
			throw new ApiInvocationException(e);
		}
	}

	public static String callCreateApi(Configuration config, String requestXML, String url,
			String resourceKeyAttributeName) throws ApiInvocationException, ImportException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_CONTENT_TYPE, Constants.MEDIA_TYPE_APPLICATION_XML);
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);
		ApiResponse apiOutput;
		try {
			if (LOG.isInfoEnabled()) {
				LOG.info("Running API: POST " + url);
				LOG.info("Request XML:\n" + requestXML);
			}
			apiOutput = config.getResourceFactory().createHttpClientInstance().doPost(url, headers, requestXML,
					config.getUserName(), config.getPassword(), config, config.getPrHostName());
			if (LOG.isInfoEnabled()) {
				ImportHelper.printApiResponse(apiOutput);
			}
			if (!apiOutput.getStatusCode().equals(SftpResourceHelper.SUCCESS_STATUS_CODE_201)) {
				String message = apiOutput.getResponse();
				boolean isResponsePresent = true;
				if (message == null || message.isEmpty()) {
					isResponsePresent = false;
					message = apiOutput.getStatusLine();
				}
				throw new ImportException(getErrorDescription(message, isResponsePresent));
			}
			return getResourceKeyFromCreateApiResponse(apiOutput.getResponse(), resourceKeyAttributeName);
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| URISyntaxException | HttpException | IOException | ParserConfigurationException | SAXException
				| ValidationException e) {
			throw new ApiInvocationException(e);
		}
	}

	public static String getErrorDescription(String response, boolean isResponsePresent)
			throws ParserConfigurationException, SAXException, IOException {
		if (isResponsePresent) {
			return DOMUtils.getAttributeValueByTagName(response, "error", "errorDescription");
		} else {
			return response;
		}
	}

	private static String getResourceKeyFromCreateApiResponse(String apiResponse, String attributeName)
			throws ParserConfigurationException, SAXException, IOException {
		return DOMUtils.getAttributeValueByTagName(apiResponse, "success", attributeName);
	}

	public static ApiResponse getResourceFromSFG(Configuration config, boolean getFromProductionSfg, String resourceUri,
			String resourceKey) throws ApiInvocationException, ImportException {
		return ImportHelper.getResourceFromSFG(getFromProductionSfg, config, resourceUri, resourceKey);
	}

	public static boolean isServerTypeSGorDG(String profileConfigKey, Configuration config) throws ImportException {
		String serverType = null;
		String url = config.buildPRUrl(CRETAE_PROFILECONFIGURATION_API_URL) + String.format(profileConfigKey);
		try {
			String apiResponse = SftpResourceHelper.getResourceFromPR(config, url).getResponse();
			serverType = DOMUtils.getAttributeValueByTagName(apiResponse, "serverType", "code");
		} catch (ParserConfigurationException | SAXException | IOException | ApiInvocationException e) {
			throw new ImportException(e);
		}

		if (serverType.equals("SG") || serverType.equals("DG")) {
			return true;
		}
		return false;
	}
}
