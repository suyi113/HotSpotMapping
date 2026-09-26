package net.hotspot.cplus;



import net.hotspot.cplus.Utils.UnsafeUtils;

import java.util.ArrayList;
import java.util.List;

public class ciEnv {

    private static final long TASK_OFF =
            VMStructsHelper.findOffsetOrDefault("ciEnv::_task", -1L);
    private static final long FACTORY_OFF =
            VMStructsHelper.findOffsetOrDefault("ciEnv::_factory", -1L);

    private static final long CI_METADATA_OFF =
            VMStructsHelper.findOffsetOrDefault("ciObjectFactory::_ci_metadata", -1L);

    private static final long METADATA_OFF = 16L;

    private static final int  GA_LEN_OFF  = 0;
    private static final int  GA_DATA_OFF = 8;

    private final long address;

    public ciEnv(long address) { this.address = address; }

    public long getAddress() { return address; }

    public CompileTask getTask() {
        if (TASK_OFF < 0) return null;
        long t = UnsafeUtils.unsafe.getLong(address + TASK_OFF);
        return t == 0 ? null : new CompileTask(t);
    }

    public long findCiMethodFor(Method target) {
        if (FACTORY_OFF < 0 || CI_METADATA_OFF < 0) return 0;

        long factory = UnsafeUtils.unsafe.getLong(address + FACTORY_OFF);
        if (factory == 0) return 0;

        long arrBase = factory + CI_METADATA_OFF;
        int  len     = UnsafeUtils.unsafe.getInt(arrBase + GA_LEN_OFF);
        long data    = UnsafeUtils.unsafe.getLong(arrBase + GA_DATA_OFF);
        if (len <= 0 || data == 0) return 0;

        long targetAddr = target.getAddress();

        int lo = 0, hi = len - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long ci = UnsafeUtils.unsafe.getLong(data + (long) mid * 8L);
            if (ci == 0 || (ci & 7L) != 0) return 0;

            long meta = UnsafeUtils.unsafe.getLong(ci + METADATA_OFF);

            int cmp = Long.compareUnsigned(meta, targetAddr);
            if (cmp == 0)      return ci;
            else if (cmp < 0)  lo = mid + 1;
            else               hi = mid - 1;
        }
        return 0;
    }

    public CiMethod getCiMethod(Method target) {
        long p = findCiMethodFor(target);
        return p == 0 ? null : new CiMethod(p);
    }

    public List<Long> listAllCiMetadata() {
        List<Long> out = new ArrayList<>();
        if (FACTORY_OFF < 0 || CI_METADATA_OFF < 0) return out;

        long factory = UnsafeUtils.unsafe.getLong(address + FACTORY_OFF);
        if (factory == 0) return out;

        long arrBase = factory + CI_METADATA_OFF;
        int  len     = UnsafeUtils.unsafe.getInt(arrBase + GA_LEN_OFF);
        long data    = UnsafeUtils.unsafe.getLong(arrBase + GA_DATA_OFF);
        if (len <= 0 || data == 0) return out;

        for (int i = 0; i < len; i++) {
            long ci = UnsafeUtils.unsafe.getLong(data + (long) i * 8L);
            if (ci == 0 || (ci & 7L) != 0 || ci < 0x10000L) continue;
            out.add(ci);
        }
        return out;
    }

    public List<CiMethod> listCiMethods() {
        List<CiMethod> out = new ArrayList<>();
        for (long ci : listAllCiMetadata()) {
            long meta = UnsafeUtils.unsafe.getLong(ci + METADATA_OFF);
            if (meta == 0) continue;

            long cmOrLh = UnsafeUtils.unsafe.getLong(meta + 8L);

            if ((cmOrLh & 7L) != 0) continue;
            if (cmOrLh < 0x100000000L) continue;

            out.add(new CiMethod(ci));
        }
        return out;
    }
}