package org.tagsobe.script;

import org.tagbrowser.api.TagBrowser;

public class HotelBookingTest {
	public static void main(String[] args) {
		String url = "http://localhost:8080";
		if (args.length > 0)
			url = args[0];
		HotelBookingTest test = new HotelBookingTest();
		try {
			test.perform(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void perform(String url) throws Exception {
		TagBrowser browser = new TagBrowser();
		browser.setStatStream(System.out);
		browser.open(url);
		browser.submit(0, "a", "Find Hotels");
		browser.submit(0, "View Hotel");
		browser.submit(0, "Book Hotel");
		browser.submit(0, "keith", "melbourne", "Login");
		browser.submit(0, "2041-12-01", "2041-12-02", "false",
				"1111222233334444", "KEITH MELBOURNE", "1", "1", "2010",
				"Proceed");
		browser.submit(0, "Confirm");
		browser.contains("Current Hotel Bookings");
	}
}
