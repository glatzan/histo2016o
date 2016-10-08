package org.histo.config.enums;

public enum Month {
	JANUARY(0), FEBRUARY(1), MARCH(2), APRIL(3), MAY(4), JUNE(5), JULY(6), AUGUST(7), SEPTEMBER(8), OCTOBER(
			9), NOVEMBER(10), DECEMBER(11);

	private final int number;

	Month(final int number) {
		this.number = number;
	}

	public int getNumber() {
		return number;
	}

	public final static Month getMonthByNumber(int number){
		Month[] arr = Month.values();
		for (int i = 0; i < arr.length; i++) {
			if(arr[i].getNumber() == number)
				return arr[i];
		}
		
		return JANUARY;
	}
}
