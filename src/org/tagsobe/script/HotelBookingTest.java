package org.tagsobe.script;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.tagbrowser.api.ElementNotFoundException;
import org.tagbrowser.api.TagBrowser;
import org.tagsobe.script.HotelBookingCounter.Value;

public class HotelBookingTest {
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out
					.println("Usage: java -jar tagsobe.jar <client1,client2,...> <loops> <url>");
			return;
		}
		String[] clients = args[0].split(",");
		int loops = Integer.parseInt(args[1]);
		String url = args[2];
		HotelBookingTest test = new HotelBookingTest();
		try {
			for (String client : clients) {
				test.perform(url, Integer.parseInt(client), loops);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HotelBookingCounter counter;

	private void perform(String url, int clients, int loop) throws Exception {
		counter = new HotelBookingCounter();
		new Scan(url, 1).run();
		counter = new HotelBookingCounter();
		Thread[] pool = new Thread[clients];
		for (int i = 0; i < pool.length; i++) {
			pool[i] = new Thread(new Scan(url, loop));
			pool[i].start();
		}
		for (int i = 0; i < pool.length; i++) {
			pool[i].join();
		}
		for (Value value : counter.getValues()) {
			System.out.println(clients + "\t" + value.getMS() + "\t"
					+ value.getMSDev() + "\t" + value.getParse());
		}
		// System.out.println(counter.getAll());
	}

	class Scan implements Runnable {

		private final String url;
		private final int loop;

		public Scan(String url, int loop) {
			this.url = url;
			this.loop = loop;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < loop; i++) {
					scan();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void scan() throws ClientProtocolException, IOException,
				ElementNotFoundException, URISyntaxException {
			TagBrowser browser = new TagBrowser();
			browser.addCounter(counter);
			// browser.setStatStream(System.out);
			// browser.setShowParams(true);
			browser.open(url);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			browser.submit(0, "a", "Find Hotels");
			browser.clickName("View Hotel");
			browser.submit(0, "Book Hotel");
			browser.submit(0, "keith", "melbourne", "Login");
			browser.submit(0, "12-01-2041", "12-02-2041", "false",
					"OCEAN_VIEWdfg", "OCEAN_VIEWdgf", "OCEAN_VIEW",
					"1111222233334444", "KEITH MELBOURNE", "1", "1", "2010",
					"Proceed");
			browser.submit(0, "Confirm");
			browser.contains("Current Hotel Bookings");
		}
	}
}
