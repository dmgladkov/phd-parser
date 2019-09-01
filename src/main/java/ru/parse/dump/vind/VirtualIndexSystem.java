package ru.parse.dump.vind;

import ru.parse.dump.objects.DumpClass;

public interface VirtualIndexSystem {
    <T> T find(long address, Class<T> objectType);

    void save(long address, Object object);

    void save(DumpClass aClass);
}
