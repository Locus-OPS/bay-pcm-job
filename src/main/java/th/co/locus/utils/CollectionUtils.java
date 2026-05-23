package th.co.locus.utils;

import java.util.List;

public class CollectionUtils {
	public static boolean isEmpty(List<?> list) {
		if (list == null) {
			return true;
		}
		return list.isEmpty();
	}

	/**
	 * Verify that the input string is existed in the input list or not.
	 *
	 * @param list        the input list
	 * @param inputString the input string
	 * @param ignoreCase  the flag to tell that compare string by ignore sensitive
	 *                    case or not
	 *
	 * @return existing result
	 */
	public static boolean isExistStringInList(List<String> list, String inputString, boolean ignoreCase) {
		if (isEmpty(list)) {
			return false;
		}

		if (inputString == null || inputString.isBlank()) {
			return false;
		}

		return list.stream()
				.anyMatch(s -> ignoreCase ? inputString.equalsIgnoreCase(s) : inputString.equals(s));
	}
}
