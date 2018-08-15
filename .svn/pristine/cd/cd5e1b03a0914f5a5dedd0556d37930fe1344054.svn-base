package com.mitrallc.camp;

import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectInspector {

	private static final int HEADER_SIZE = 8;
	private static final int POINTER_SIZE = 4;
	private static final int ARRAY_LENGTH_FIELD_SIZE = 4;

	/**
	 * estimated size of given object
	 * 
	 * @param object
	 * @return estimated object size in bytes
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static long deepMemoryUsage(Object object)
			throws IllegalArgumentException, IllegalAccessException {
		LinkedList<Object> stack = new LinkedList<Object>();
		long size = 0;
		Map<Class<?>, List<Field>> cache = new HashMap<Class<?>, List<Field>>();
		Set<Object> seen = new HashSet<Object>();
		seen.add(object);
		stack.push(object);
		while (!stack.isEmpty()) {
			Object top = stack.pop();
			Class<?> currentClass = top.getClass();
			if (ignore(top)) {
				continue;
			}
			long csize = HEADER_SIZE;
			if (currentClass.isArray()) {
				csize += ARRAY_LENGTH_FIELD_SIZE;
				Class<?> type = top.getClass();
				if (boolean[].class.isInstance(type)) {
					csize += boolean[].class.cast(top).length;
				} else if (byte[].class.isInstance(top)) {
					csize += byte[].class.cast(top).length;
				} else if (short[].class.isInstance(top)) {
					csize += short[].class.cast(top).length * 2;
				} else if (int[].class.isInstance(top)) {
					csize += int[].class.cast(top).length * 4;
				} else if (long[].class.isInstance(top)) {
					csize += long[].class.cast(top).length * 8;
				} else if (float[].class.isInstance(top)) {
					csize += float[].class.cast(top).length * 4;
				} else if (double[].class.isInstance(top)) {
					csize += double[].class.cast(top).length * 8;
				} else if (char[].class.isInstance(top)) {
					csize += char[].class.cast(top).length * 2;
				} else {
					Object[] castedObjectArray = Object[].class.cast(top);
					csize += castedObjectArray.length * POINTER_SIZE;
					for (Object em : castedObjectArray) {
						if (em != null && !seen.contains(em)) {
							stack.push(em);
							seen.add(em);
						}
					}
				}
			} else {
				List<Field> fields = cache.get(currentClass);
				if (fields == null) {
					fields = getObjectFields(currentClass);
					cache.put(currentClass, fields);
				}

				for (Field field : fields) {
					if (field == null) {
						csize += POINTER_SIZE;
					} else {
						Class<?> type = field.getType();
						if (boolean.class.equals(type)
								|| byte.class.equals(type)) {
							csize += 1;
						} else if (short.class.equals(type)
								|| char.class.equals(type)) {
							csize += 2;
						} else if (int.class.equals(type)
								|| float.class.equals(type)) {
							csize += 4;
						} else if (long.class.equals(type)
								|| double.class.equals(type)) {
							csize += 8;
						} else {
							csize += POINTER_SIZE;
							Object value = field.get(top);
							if (value != null && !seen.contains(value)) {
								stack.push(value);
								seen.add(value);
							}
						}
					}
				}
			}
			size += byte2Word(csize);
		}
		seen.clear();
		cache.clear();
		return size * 8;
	}

	private static long byte2Word(long size) {
		return (size + 7) >> 3;
	}

	/**
	 * Ignore these since it is referencing the whole JVM
	 * 
	 * @param obj
	 * @return
	 */
	private static boolean ignore(Object obj) {
		return ThreadGroup.class.isInstance(obj)
				|| ClassLoader.class.isInstance(obj)
				|| AccessControlContext.class.isInstance(obj)
				|| PlatformManagedObject.class.isInstance(obj);
	}

	private static List<Field> getObjectFields(Class<?> type) {
		boolean skipParent = type.isMemberClass();
		List<Field> result = new ArrayList<Field>();
		while (!Object.class.equals(type)) {
			for (Field field : type.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					// doesn't include thread since the field is not thread-safe.  
					if (Thread.class.equals(type)) {
						result.add(null);
					} else if (skipParent
							&& field.getName().startsWith("this$")) {
						result.add(null);
					} else if (Thread.class.equals(field.getType())) {
						result.add(null);
					} else {
						field.setAccessible(true);
						result.add(field);
					}
				}
			}
			type = type.getSuperclass();
		}
		return result;
	}
}