package org.tagbrowser.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Test;

public class CoreTest {

	@Test
	public void testGithub() throws ClientProtocolException, IOException,
			ElementNotFoundException {
		TagBrowser browser = new TagBrowser();
		browser.open("https://github.com/joergviola/play-cms");
		assertTrue(browser.contains("A simple CMS"));
		assertFalse(browser.contains("blablabla"));
		assertTrue(browser.getLoadTime() > 100L);
		assertTrue(browser.getLoadTime() < 10000L);
		browser.clickLinkByName("About");
		assertTrue(browser.location().equals("https://github.com/about"));
	}

	@Test
	public void testIS24() throws ClientProtocolException, IOException,
			ElementNotFoundException, URISyntaxException {
		TagBrowser browser = new TagBrowser();
		browser.open("http://www.is24.de");
		TagForm form = browser.getForm("is24-hp-onestepsearch");
		form.setField("location", "Lünen");
		form.submitValue("Suchen");
		browser.clickLinkByName("Citynah und ruhig in Lünen-Brambauer");
		assertTrue(browser.contains("Frau  Elisabeth Braun"));
	}
}
