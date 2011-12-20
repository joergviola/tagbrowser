package org.tagsobe.script;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.tagbrowser.api.ElementNotFoundException;
import org.tagbrowser.api.TagBrowser;
import org.tagsobe.script.HotelBookingCounter.Value;

public class HotelBookingTest {
	private static final String CLIENTS = "1,2,3,4,5,7,10,12,15,17,20,25,30,40,50,100,200,300,500";
	private static final String LOOPS = "200";

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java -jar tagsobe.jar <url>");
			System.out.println("       -Dclients List of client counts");
			System.out
					.println("       -Dloops List of sequential loops per client");
			System.out.println("       -Dstats shows call stats");
			System.out.println("       -Dparams shows call params");
			return;
		}
		;
		;
		String[] clients = System.getProperty("clients", CLIENTS).split(",");
		int loops = Integer.parseInt(System.getProperty("loops", LOOPS));
		String url = args[0];
		HotelBookingTest test = new HotelBookingTest();
		try {
			for (String client : clients) {
				test.perform(url, Integer.parseInt(client), loops);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean stats;
	private boolean params;

	public HotelBookingTest() {
		stats = System.getProperty("stats") != null;
		params = System.getProperty("params") != null;
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
		int index = 1;
		for (Value value : counter.getValues()) {
			System.out.println(index + ".\t" + clients + "\t" + value.getMS()
					+ "\t" + value.getMSDev() + "\t" + value.getParse());
			index++;
		}
		System.out.println("all\t" + clients + "\t" + counter.getAll().getMS()
				+ "\t" + counter.getAll().getMSDev() + "\t"
				+ counter.getAll().getParse());
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
			if (stats)
				browser.setStatStream(System.out);
			if (params)
				browser.setShowParams(true);
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
