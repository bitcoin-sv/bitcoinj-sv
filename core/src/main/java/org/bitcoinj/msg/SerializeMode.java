package org.bitcoinj.msg;

public class SerializeMode {

    private final boolean parseLazy;
    private final boolean parseRetain;
    private boolean compactTransactionsInBlock = true;

    public SerializeMode(boolean parseLazy, boolean parseRetain) {
        this.parseLazy = parseLazy;
        this.parseRetain = parseRetain;
    }

    public SerializeMode(boolean parseLazy, boolean parseRetain, boolean compactTransactionsInBlock) {
        this.parseLazy = parseLazy;
        this.parseRetain = parseRetain;
        this.compactTransactionsInBlock = compactTransactionsInBlock;
    }

    public boolean isParseLazyMode() {
        return parseLazy;
    }

    public boolean isParseRetainMode() {
        return parseRetain;
    }

    public boolean isCompactTransactionsInBlock() {
        return compactTransactionsInBlock;
    }

    public void setCompactTransactionsInBlock(boolean compactTransactionsInBlock) {
        this.compactTransactionsInBlock = compactTransactionsInBlock;
    }

    public static final SerializeMode DEFAULT = new SerializeMode(false, false, false);

}
