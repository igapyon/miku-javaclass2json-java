package jp.igapyon.mikujavaclass2json.internal;

import java.util.ArrayList;
import java.util.List;

public final class DescriptorTypes {
    private DescriptorTypes() {
    }

    public static List<String> extract(String descriptor) {
        List<String> values = new ArrayList<String>();
        if (descriptor == null) {
            return values;
        }
        int index = 0;
        while (index < descriptor.length()) {
            int start = descriptor.indexOf('L', index);
            if (start < 0) {
                break;
            }
            int end = descriptor.indexOf(';', start);
            if (end < 0) {
                break;
            }
            values.add(descriptor.substring(start + 1, end).replace('/', '.'));
            index = end + 1;
        }
        return values;
    }
}
