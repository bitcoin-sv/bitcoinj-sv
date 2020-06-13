/**
 * Copyright (c) 2020 Steve Shadders.
 * All rights reserved.
 */
package org.bitcoinj.utils;

import org.bitcoinj.utils.ObjectGetter;

/**
 * uses a fetcher getter the first time it's called then becomes direct. This can be used to implement lazy field.
 * @param <T>
 */
public class LazyGetter<T> implements ObjectGetter<T> {

    private ObjectGetter<T> fetcher;

    public LazyGetter(ObjectGetter<T> fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public T get() {
        if (fetcher instanceof DirectGetter)
            return fetcher.get();
        T obj = fetcher.get();
        fetcher = ObjectGetter.direct(obj);
        return obj;
    }


}
