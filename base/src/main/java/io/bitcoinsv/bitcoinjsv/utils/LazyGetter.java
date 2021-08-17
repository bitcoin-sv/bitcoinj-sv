/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.utils;

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
