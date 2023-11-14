package th.co.locus.utils;

import java.util.List;

import com.microsoft.sqlserver.jdbc.StringUtils;

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

		if (StringUtils.isEmpty(inputString)) {
			return false;
		}

		for (int i = 0; i < list.size(); i++) {
			String string = list.get(i);
			if (ignoreCase && inputString.equalsIgnoreCase(string)) {
				return true;
			}
			if (!ignoreCase && inputString.equals(string)) {
				return true;
			}
		}

		return false;
	}
}
