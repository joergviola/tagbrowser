package org.tagbrowser.api;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
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
	private long parse;
	private String location;
	private Document document;
	private String method;
	private boolean showCookies;
	boolean showParams;
	PrintStream statStream;
	private List<Counter> counter;
	private int requestIndex;

	public TagBrowser() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));
		ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(
				registry);
		httpClient = new DefaultHttpClient(connManager);
		ctx = new BasicHttpContext();
		counter = new ArrayList<Counter>();
		httpClient.setRedirectStrategy(new DefaultRedirectStrategy() {
			public boolean isRedirected(HttpRequest request,
					HttpResponse response, HttpContext context) {
				int responseCode = response.getStatusLine().getStatusCode();
				if (responseCode == 301 || responseCode == 302) {
					location = response.getHeaders("Location")[0].getValue();
					return true;
				} else {
					location = request.getRequestLine().getUri();
					return false;
				}
			}
		});
	}

	public void open(String uri) throws ClientProtocolException, IOException {
		if (!uri.startsWith("http")) {
			if (!uri.startsWith("/")) {
				if (uri.startsWith("?")) {
					int qmark = location.indexOf('?');
					uri = location.substring(0, qmark) + uri;
				} else {
					int lastSlash = location.lastIndexOf('/');
					uri = location.substring(0, lastSlash + 1) + uri;
				}
			}
			uri = ctx.getAttribute(ExecutionContext.HTTP_TARGET_HOST) + uri;
		}
		HttpGet httpGet = new HttpGet(uri);
		request(httpGet);
	}

	void post(HttpPost httpPost) throws ClientProtocolException, IOException,
			URISyntaxException {
		String uri = httpPost.getURI().toString();
		if (!uri.startsWith("http")) {
			uri = ctx.getAttribute(ExecutionContext.HTTP_TARGET_HOST) + uri;
			httpPost.setURI(new URI(uri));
		}
		request(httpPost);
	}

	private void request(HttpRequestBase request) throws IOException,
			ClientProtocolException {
		request.addHeader("Accept-Language", "de");
		if (showCookies && statStream != null)
			statStream.println(httpClient.getCookieStore().getCookies());
		long start = System.currentTimeMillis();
		response = httpClient.execute(request, ctx);
		ms = System.currentTimeMillis() - start;
		method = request.getMethod();
		stats();
		int statusCode = getStatusCode();
		if (statusCode != 200)
			throw new IllegalStateException("Response code: " + statusCode);
		content = null;
		parse = 0L;
	}

	public void setShowCookies(boolean showCookies) {
		this.showCookies = showCookies;
	}

	public void setShowParams(boolean showParams) {
		this.showParams = showParams;
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
		long start = System.currentTimeMillis();
		document = Jsoup.parse(content);
		parse += System.currentTimeMillis() - start;
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
		if (content == null) {
			long start = System.currentTimeMillis();
			content = EntityUtils.toString(response.getEntity());
			parse += System.currentTimeMillis() - start;
		}
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

	public void stats() {
		if (statStream != null)
			statStream.println(method + " " + location + " " + parse + "parse"
					+ " " + ms + "load");
		for (Counter counter : this.counter) {
			counter.count(requestIndex++, parse, ms);
		}
	}

	public void submit(int index, String... params)
			throws ElementNotFoundException, IOException, URISyntaxException {
		TagForm form = getForm(index);
		for (int i = 0; i < params.length - 1; i++)
			form.setField(i, params[i]);
		form.submitValue(params[params.length - 1]);
	}

	public void setStatStream(PrintStream statStream) {
		this.statStream = statStream;
	}

	public void addCounter(Counter counter) {
		this.counter.add(counter);
	}
}
