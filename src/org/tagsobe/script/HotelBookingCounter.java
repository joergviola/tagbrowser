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

		public long getMS() {
			return ms / n;
		}

		public long getMSDev() {
			return (long) Math.sqrt(Math.abs(msQ / n - (ms * ms) / (n * n)));
		}

		public long getParse() {
			return parse / n;
		}

		public long getParseDev() {
			return (long) Math.sqrt(Math.abs(parseQ / n - (parse * parse)
					/ (n * n)));
		}

		@Override
		public String toString() {
			return "Load: " + getMS() + "+/-" + getMSDev() + ", parse: "
					+ getParse() + "+/-" + getParseDev() + ", probes: " + n;
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
