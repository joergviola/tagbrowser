package org.tagsobe.script;

import java.util.ArrayList;
import java.util.List;

import org.tagbrowser.api.Counter;

public class HotelBookingCounter implements Counter {

	class Value {

		int n;
		long parse;
		long ms;
		private long msQ;
		private long parseQ;

		public synchronized void count(long parse, long ms) {
			n++;
			this.parse += parse;
			this.parseQ += parse * parse;
			this.ms += ms;
			this.msQ += ms * ms;
		}

		// http://de.wikipedia.org/wiki/Standardabweichung
		private long dev(long avg, long avgQ) {
			if (n <= 1)
				return 0;
			long rad = avgQ - avg * avg / n;
			if (rad < 0)
				return -1;
			else
				return (long) Math.sqrt(rad / (n - 1));
		}

		public long getMS() {
			return ms / n;
		}

		public long getMSDev() {
			return dev(ms, msQ);
		}

		public long getParse() {
			return parse / n;
		}

		public long getParseDev() {
			return dev(parse, parseQ);
		}

		@Override
		public String toString() {
			return "Load: " + getMS() + "+/-" + getMSDev() + ", parse: "
					+ getParse() + "+/-" + getParseDev() + ", probes: " + n;
		}

		public void out(String page, int clients, String fmt) {
			if ("java".equals(fmt)) {
				System.out.print("new Stat(\"");
				System.out.print(page);
				System.out.print("\",");
				System.out.print(clients);
				System.out.print(",");
				System.out.print(getMS());
				System.out.print(",");
				System.out.print(getMSDev());
				System.out.print(",");
				System.out.print(getParse());
				System.out.println("),");
			} else
				System.out.println(page + "\t" + clients + "\t" + getMS()
						+ "\t" + getMSDev() + "\t" + getParse());

		}
	}

	Value all = new Value();
	List<Value> values = new ArrayList<Value>();

	@Override
	public void count(int index, long parse, long ms) {
		Value value = getValue(index);
		value.count(parse, ms);
		all.count(parse, ms);
	}

	public Value getValue(int index) {
		if (index == values.size()) {
			Value value = new Value();
			values.add(value);
			return value;
		} else {
			return values.get(index);
		}
	}

	public Value getAll() {
		return all;
	}

	public List<Value> getValues() {
		return values;
	}
}
