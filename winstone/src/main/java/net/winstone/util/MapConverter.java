package net.winstone.util;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * MapConverter apply map value onto bean according naming of key and member.
 * Waiting use of a mini IOc conainer aka pico ...
 * 
 * @author Jerome Guibert
 */
public class MapConverter {

    public static <T> T apply(final Map<String, String> map, final T target) throws IllegalArgumentException {
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Field field = null;
                try {
                    field = target.getClass().getDeclaredField(entry.getKey());
                } catch (NoSuchFieldException e) {
                }
                if (field != null) {
                    boolean notAccessible = false;
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                        notAccessible = true;
                    }
                    if (field.getType().equals(String.class)) {
                        field.set(target, entry.getValue());
                    } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                        field.setInt(target, Integer.parseInt(entry.getValue()));
                    } else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                        field.setLong(target, Long.parseLong(entry.getValue()));
                    } else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                        field.setBoolean(target, Boolean.parseBoolean(entry.getValue()));
                    }
                    if (notAccessible) {
                        field.setAccessible(false);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
        return target;
    }
}
