package th.co.locus.utils;

import java.util.List;

public class CollectionUtils {
	public static boolean isEmpty(List<?> list) {
		if (list == null) {
			return true;
		}
		return list.isEmpty();
	}
}
