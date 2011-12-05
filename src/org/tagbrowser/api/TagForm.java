package org.tagbrowser.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TagForm {
	private final TagBrowser browser;
	private final HashMap<String, String> fields;
	private final HashMap<String, String> submits;
	private String method;
	private String action;

	public TagForm(TagBrowser browser, Element element) {
		this.browser = browser;
		fields = new HashMap<String, String>();
		submits = new HashMap<String, String>();
		readFields(element);
		method = element.attr("method");
		action = element.attr("action");
	}

	private void readFields(Element element) {
		Elements inputs = element.getElementsByTag("input");
		for (Element input : inputs) {
			String type = input.attr("type");
			String name = input.attr("name");
			String value = input.attr("value");
			String checked = input.attr("checked");
			// System.out.println(type + " " + name + " " + value + " " +
			// checked);
			if ("radio".equals(type)) {
				if (!checked.isEmpty())
					fields.put(name, value);
			} else if ("submit".equals(type)) {
				submits.put(name, value);
			} else {
				fields.put(name, value);
			}
		}
		Elements buttons = element.getElementsByTag("button");
		for (Element button : buttons) {
			String name = button.attr("id");
			String value = button.text();
			submits.put(name, value);
		}
		// System.out.println(fields);
		// System.out.println(submits);
	}

	public void submitName(String name) throws ClientProtocolException,
			IOException, URISyntaxException {
		if (method.equalsIgnoreCase("GET")) {
			browser.open(getUriForGet(name));
		} else if (method.equalsIgnoreCase("POST")) {
			HttpPost httpPost = getPostRequest(name);
			browser.post(httpPost);
		}
	}

	public void submitValue(String value) throws ElementNotFoundException,
			ClientProtocolException, IOException, URISyntaxException {
		String name = getSubmitForValue(value);
		submitName(name);
	}

	private String getSubmitForValue(String value)
			throws ElementNotFoundException {
		for (Entry entry : submits.entrySet()) {
			if (value.equals(entry.getValue()))
				return (String) entry.getKey();
		}
		throw new ElementNotFoundException("submit by value: " + value);
	}

	public void setField(String name, String value)
			throws ElementNotFoundException {
		if (!fields.containsKey(name))
			throw new ElementNotFoundException("Form field: " + name);
		fields.put(name, value);
	}

	public String getMethod() {
		return method;
	}

	public String getUriForGet(String submit) {
		StringBuilder result = new StringBuilder();
		result.append(action);
		result.append("?");
		result.append(submit);
		result.append("=");
		result.append(submits.get(submit));
		for (Entry<String, String> entry : fields.entrySet()) {
			result.append("&");
			result.append(entry.getKey());
			result.append("=");
			result.append(entry.getValue());
		}
		return result.toString();
	}

	public String getAction() {
		return action;
	}

	public HttpPost getPostRequest(String name)
			throws UnsupportedEncodingException {
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		for (Entry<String, String> entry : fields.entrySet()) {
			formparams.add(new BasicNameValuePair(entry.getKey(), entry
					.getValue()));
		}
		formparams.add(new BasicNameValuePair(name, submits.get(name)));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams,
				"UTF-8");
		HttpPost httppost = new HttpPost(action);
		httppost.setEntity(entity);
		return httppost;
	}

}
