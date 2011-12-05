package org.tagbrowser.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TagBrowser {

	private DefaultHttpClient httpClient;
	HttpContext ctx;
	private HttpResponse response;
	private String content;
	private long ms;
	private String location;
	private Document document;

	public TagBrowser() {
		httpClient = new DefaultHttpClient();
		ctx = new BasicHttpContext();
		httpClient.setRedirectStrategy(new DefaultRedirectStrategy() {
			public boolean isRedirected(HttpRequest request,
					HttpResponse response, HttpContext context) {
				int responseCode = response.getStatusLine().getStatusCode();
				if (responseCode == 301 || responseCode == 302) {
					return true;
				} else
					return false;
			}
		});
	}

	public void open(String uri) throws ClientProtocolException, IOException {
		if (!uri.startsWith("http")) {
			uri = ctx.getAttribute(ExecutionContext.HTTP_TARGET_HOST) + uri;
		}
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader("Accept-Language", "de");
		long start = System.currentTimeMillis();
		response = httpClient.execute(httpGet, ctx);
		ms = System.currentTimeMillis() - start;
		int statusCode = getStatusCode();
		if (statusCode != 200)
			throw new IllegalStateException("Response code: " + statusCode);
		location = uri;
		content = null;
	}

	void post(HttpPost httpPost) throws ClientProtocolException, IOException,
			URISyntaxException {
		String uri = httpPost.getURI().toString();
		if (!uri.startsWith("http")) {
			uri = ctx.getAttribute(ExecutionContext.HTTP_TARGET_HOST) + uri;
			httpPost.setURI(new URI(uri));
		}
		httpPost.addHeader("Accept-Language", "de");
		long start = System.currentTimeMillis();
		response = httpClient.execute(httpPost, ctx);
		ms = System.currentTimeMillis() - start;
		int statusCode = getStatusCode();
		if (statusCode != 200)
			throw new IllegalStateException("Response code: " + statusCode);
		location = httpPost.getURI().toString();
		content = null;
	}

	public int getStatusCode() {
		return response.getStatusLine().getStatusCode();
	}

	public void clickName(String name) throws IOException,
			ElementNotFoundException {
		getDom();
		Element link = findLinkByName(name);
		open(link);
	}

	private void open(Element link) throws ClientProtocolException, IOException {
		open(link.attr("href"));
	}

	private Element findLinkByName(String name) throws ElementNotFoundException {
		Elements links = document.getElementsByTag("a");
		for (Element element : links) {
			if (name.equals(element.text().trim()))
				return element;
		}
		throw new ElementNotFoundException("link by name: " + name);
	}

	private void getDom() throws IOException {
		getContent();
		document = Jsoup.parse(content);
	}

	public void clickId(String id) {

	}

	public TagForm getForm(int no) throws ElementNotFoundException, IOException {
		int i = no;
		getDom();
		Elements forms = document.getElementsByTag("form");
		for (Element element : forms) {
			if (i-- == 0) {
				return new TagForm(this, element);
			}
		}
		throw new ElementNotFoundException("form by no: " + no);
	}

	public TagForm getForm(String id) throws IOException,
			ElementNotFoundException {
		getDom();
		Element element = document.getElementById(id);
		if (element == null)
			throw new ElementNotFoundException("form by id: " + id);
		else
			return new TagForm(this, element);
	}

	public boolean contains(String fragment) throws ParseException, IOException {
		getContent();
		return content.indexOf(fragment) != -1;
	}

	public String getContent() throws IOException {
		if (content == null)
			content = EntityUtils.toString(response.getEntity());
		return content;
	}

	public long getLoadTime() {
		return ms;
	}

	public String location() {
		return location;
	}

	public void dump() throws IOException {
		System.out.println(location);
		System.out.println(getStatusCode());
		getContent();
		System.out.println(content);
	}
}
