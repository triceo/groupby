package org.optaplanner.examples.cloudbalancing;

import java.util.Objects;

public final class GroupKey {

    private final Object key;

    public GroupKey(final Object key) {
        this.key = key;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final GroupKey groupKey = (GroupKey) o;
        return Objects.equals(key, groupKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
