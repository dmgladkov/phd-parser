package ru.parse.dump.vind;

import ru.parse.dump.objects.DumpClass;

import java.util.HashMap;
import java.util.Map;

public class DumpClassCacheRegion {
    private static final int CAPACITY = 100000;
    private Map<Long, DumpClass> cached = new HashMap<>(CAPACITY);

    public void put(DumpClass aClass) {
        cached.put(aClass.getAddress(), aClass);
    }

    public DumpClass find(long address) {
        return cached.get(address);
    }

    public int size() {
        return cached.size();
    }
}
