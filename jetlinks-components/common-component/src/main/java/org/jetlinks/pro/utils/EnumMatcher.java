package org.jetlinks.pro.utils;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnumMatcher<E extends Enum<?>> {

    @Getter
    private Set<String> excludes;

    @Getter
    private Set<String> includes = new HashSet<>(Collections.singleton("*"));

    /**
     * 设置为true时, 优选判断excludes
     */
    @Getter
    private boolean excludeFirst = true;

    private final E[] enums;

    @Getter
    private final long allMask;

    @Getter
    private long matchMask;

    private final Function<String, Optional<E>> converter;

    public EnumMatcher(E[] enums, Function<String, Optional<E>> converter) {
        this.enums = enums;
        this.converter = converter;
        this.allMask = createMask(enums);
        init();
    }

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes;
        init();
    }

    public void setIncludes(Set<String> includes) {
        this.includes = includes;
        init();
    }

    public void setExcludeFirst(boolean excludeFirst) {
        this.excludeFirst = excludeFirst;
        init();
    }


    protected long createMask(E[] messageTypes) {
        long mask = 0;

        for (E messageType : messageTypes) {
            mask |= 1L << messageType.ordinal();
        }
        return mask;
    }

    protected long createMask(Collection<E> messageTypes) {
        long mask = 0;

        for (E messageType : messageTypes) {
            mask |= 1L << messageType.ordinal();
        }
        return mask;
    }

    protected void init() {
        long excludesMask = 0;
        if (!CollectionUtils.isEmpty(excludes)) {
            matchMask = allMask;
            if (excludes.contains("*")) {
                excludesMask = createMask(Arrays.asList(enums));
            } else {
                excludesMask = createMask(excludes.stream()
                                                  .map(converter)
                                                  .filter(Optional::isPresent)
                                                  .map(Optional::get)
                                                  .collect(Collectors.toList()));
            }
            matchMask ^= excludesMask;
        }
        if (!CollectionUtils.isEmpty(includes)) {
            long includesMask;
            if (includes.contains("*")) {
                includesMask = createMask(Arrays.asList(enums));
            } else {
                includesMask = createMask(includes.stream()
                                                  .map(converter)
                                                  .filter(Optional::isPresent)
                                                  .map(Optional::get)
                                                  .collect(Collectors.toList()));
            }
            matchMask = includesMask;
            if (excludeFirst) {
                matchMask ^= excludesMask;
            }
        }
    }

    @SafeVarargs
    public final boolean match(E... type) {
        long mask = createMask(type);
        return (matchMask & mask) != 0;
    }

    public final boolean match(E type) {
        long mask = 1L << type.ordinal();

        return (matchMask & mask) != 0;
    }
}
