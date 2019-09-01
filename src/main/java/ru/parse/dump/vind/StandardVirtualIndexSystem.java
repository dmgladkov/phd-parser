package ru.parse.dump.vind;

import ru.parse.dump.objects.DumpClass;

public class StandardVirtualIndexSystem implements VirtualIndexSystem {
    private DumpClassCacheRegion classCacheRegion = new DumpClassCacheRegion();

    @Override
    public <T> T find(long address, Class<T> objectType) {
        if (objectType == DumpClass.class) {
            return objectType.cast(classCacheRegion.find(address));
        }
        return null;
    }

    @Override
    public void save(long address, Object object) {

    }

    @Override
    public void save(DumpClass aClass) {
        classCacheRegion.put(aClass);
    }

    public int getClassesCount() {
        return classCacheRegion.size();
    }
}
