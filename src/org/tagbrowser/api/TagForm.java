package org.tagbrowser.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
	class Field {
		String value;

		public Field(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	private final TagBrowser browser;
	private final HashMap<String, Field> fieldsByName;
	private final ArrayList<Field> fieldsBySeq;
	private final HashMap<String, String> submits;
	private String method;
	private String action;

	public TagForm(TagBrowser browser, Element element) {
		this.browser = browser;
		fieldsByName = new HashMap<String, Field>();
		fieldsBySeq = new ArrayList<TagForm.Field>();
		submits = new HashMap<String, String>();
		readFields(element);
		method = element.attr("method");
		action = "/seam-wicket/"+element.attr("action");
//		action = element.attr("action");
	}

	private void readFields(Element element) {
		readInputs(element);
		readSelects(element);
		readButtons(element);
		// System.out.println(fields);
		// System.out.println(submits);
	}

	private void readButtons(Element element) {
		Elements buttons = element.getElementsByAttributeValue("type","button");
		for (Element button : buttons) {
			String name = button.attr("name");
			//String value = button.text().trim();
			String value = button.attr("value");
			submits.put(name, value);
		}
	}

	private void readInputs(Element element) {
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
					addField(type, name, value);
			} else if ("submit".equals(type)) {
				submits.put(name, value);
			} else if (!"button".equals(type)){
				addField(type, name, value);
			}
		}
	}

	private void readSelects(Element element) {
		Elements selects = element.getElementsByTag("select");
		for (Element select : selects) {
			String name = select.attr("name");
			String value = getSelectedOption(select);
			addField("select", name, value);
		}
	}

	private String getSelectedOption(Element select) {
		Elements options = select.getElementsByTag("option");
		String first = null;
		for (Element option : options) {
			String value = option.attr("value");
			if (first == null)
				first = value;
			String selected = option.attr("selected");
			if (selected != null)
				return value;
		}
		return first;
	}

	private void addField(String type, String name, String value) {
		Field field = new Field(value);
		fieldsByName.put(name, field);
		if (!"hidden".equals(type))
			fieldsBySeq.add(field);
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
		for (Entry<String, String> entry : submits.entrySet()) {
			if (value.equals(entry.getValue()))
				return (String) entry.getKey();
		}
		throw new ElementNotFoundException("submit by value: " + value);
	}

	public void setField(String name, String value)
			throws ElementNotFoundException {
		if (!fieldsByName.containsKey(name))
			throw new ElementNotFoundException("Form field: " + name);
		fieldsByName.get(name).value = value;
	}

	public void setField(int index, String value)
			throws ElementNotFoundException {
		if (index < 0 || index >= fieldsBySeq.size())
			throw new ElementNotFoundException("Form field by index: " + index);
		fieldsBySeq.get(index).value = value;
	}

	public String getMethod() {
		return method;
	}

	class URIBuilder {
		private StringBuilder builder = new StringBuilder();

		public void encode(String s) {
			try {
				builder.append(URLEncoder.encode(s, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		@Override
		public String toString() {
			return builder.toString();
		}

		public void plain(String s) {
			builder.append(s);
		}
	}

	public String getUriForGet(String submit) {
		URIBuilder result = new URIBuilder();
		result.plain(action);
		result.plain("?");
		result.encode(submit);
		result.plain("=");
		result.encode(submits.get(submit));
		for (Entry<String, Field> entry : fieldsByName.entrySet()) {
			result.plain("&");
			result.encode(entry.getKey());
			result.plain("=");
			result.encode(entry.getValue().value);
		}
		return result.toString();
	}

	public String getAction() {
		return action;
	}

	public HttpPost getPostRequest(String name)
			throws UnsupportedEncodingException {
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		for (Entry<String, Field> entry : fieldsByName.entrySet()) {
			formparams.add(new BasicNameValuePair(entry.getKey(), entry
					.getValue().value));
		}
		formparams.add(new BasicNameValuePair(name, submits.get(name)));
		if (browser.showParams && browser.statStream != null)
			browser.statStream.println(formparams);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams,
				"UTF-8");
		HttpPost httppost = new HttpPost(action);
		httppost.setEntity(entity);
		return httppost;
	}

}
