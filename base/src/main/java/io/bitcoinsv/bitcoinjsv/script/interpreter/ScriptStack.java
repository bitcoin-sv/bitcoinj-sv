/*
 * Author: Steve Shadders
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.script.interpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ScriptStack extends LinkedList<StackItem> {

    private static final Logger log = LoggerFactory.getLogger(ScriptStack.class);

    /**
     * A global flag to turn on an extra check when getting stackmemoryusage. The extra
     * check manually iterates the entire stack and calculates the usage manually. This
     * is far slower than the usual method were it is calculated on the fly but is useful
     * for testing.
     */
    public static boolean VERIFY_STACK_MEMEORY_USAGE = false;

    private final boolean verifyStackMemoryUsage;
    private long stackBytes = 0;

    //no opcode removes more than 4 items from the stack except CHECKMULTISIG
    private List<StackItem> poppedItems = new ArrayList(4);

    public ScriptStack(ScriptStack stack) {
        super(stack);
        stackBytes = stack.stackBytes;
        verifyStackMemoryUsage = stack.verifyStackMemoryUsage;
    }

    public ScriptStack(Collection<? extends StackItem> c, boolean verifyStackMemoryUsage) {
        this();
        addAll(c);
    }

    public ScriptStack() {
        super();
        this.verifyStackMemoryUsage = VERIFY_STACK_MEMEORY_USAGE;
    }

    /**
     * Set the stack state of all contained items to derived.
     * TODO inefficient implementation since we are index accessing LinkedList, probably doesn't matter until we have very large scripts.
     *
     * @param derived whether the stack state should be considered known to the execution context.  If in doubt set to true
     * @return
     */
    public void setDerivations(boolean derived) {
        for (int i = 0; i < this.size(); i++) {
            StackItem old = this.get(i);
            set(i, StackItem.wrapDerived(old.bytes(), old.getType(), derived));
        }
    }

    /**
     * Used for testing to manually calculate stackmemoryusageconsensus and compare it
     * against the faster 'on-the-fly' calculated value.
     * @return
     */
    public boolean isVerifyStackMemoryUsage() {
        return verifyStackMemoryUsage;
    }

    /**
     * Returns the stack memory usage as defined by the genesis spec:
     * https://github.com/bitcoin-sv-specs/protocol/blob/master/updates/genesis-spec.md
     * @return
     */
    public long getStackMemoryUsage() {
        long memoryUsage = 32 * size() + stackBytes;
        if (verifyStackMemoryUsage) {
            long calculated = calculateStackMemoryUsage();
            if (calculated != memoryUsage) {
                throw new RuntimeException(String.format("Stack memory usage calculation error. Calculated value: %s, verified value: %s", memoryUsage, calculated));
            }
            log.debug("Stack memory usage calculation correct. Calculated value: {}, verified value: {}", memoryUsage, calculated);
        }
        return memoryUsage;
    }

    /**
     * Manually calculates the maxstackmemoryusage according to the genesis spec.
     * This iterates the whole stack and isn't performant but is useful to test.
     * @return
     */
    public long calculateStackMemoryUsage() {
        long memoryUsage = 0;
        for (StackItem item: this) {
            memoryUsage += 32 + item.length();
        }
        return memoryUsage;
    }

    /**
     * Returns the total bytes on the stack. This is NOT the value used to check maxstackmemoryusage for consensus
     * @return
     */
    public long getStackBytes() {
        return stackBytes;
    }

    public void clearPoppedItems() {
        poppedItems.clear();
    }

    /**
     * all items popped from the stack since the last time clearPoppedItems() was called
     * @return
     */
    public List<StackItem> getPoppedItems() {
        return Collections.unmodifiableList(poppedItems);
    }

    public List<StackItem> unmodifiable() {
        return Collections.unmodifiableList(this);
    }

    public boolean add(StackItem item) {
        stackBytes += item.length();
        return super.add(item);
    }

    public boolean add(StackItem from, StackItem ... derivedFrom) {
        return add(StackItem.from(from, derivedFrom));
    }

    public boolean add(StackItem.Type type, byte[] bytes, StackItem ... derivedFrom) {
        return add(StackItem.forBytes(bytes, type, derivedFrom));
    }

    @Override
    public void addLast(StackItem stackItem) {
        throw new UnsupportedOperationException();
//        stackBytes += stackItem.length();
//        super.addLast(stackItem);
    }

    @Override
    public StackItem getLast() {
        return super.getLast();
    }

    @Override
    public StackItem pollLast() {
        StackItem item = super.pollLast();
        stackBytes -= item.length();
        poppedItems.add(item);
        return item;
    }

    @Override
    public boolean addAll(Collection<? extends StackItem> c) {
        int size = size();
        for (StackItem item: c)
            add(item);
        return size != size();
    }

    @Override
    public void clear() {
        super.clear();
        stackBytes = 0;
    }

    @Override
    public StackItem set(int index, StackItem element) {
        StackItem old = super.set(index, element);
        if (old != null)
            stackBytes -= old.length();
        stackBytes += element.length();
        return old;
    }

    @Override
    public Iterator<StackItem> descendingIterator() {
        return new StackIterator(super.descendingIterator());
    }

    public Iterator<StackItem> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<StackItem> listIterator() {
        return new StackIterator(super.listIterator(0));
    }

    @Override
    public ListIterator<StackItem> listIterator(int index) {
        return new StackIterator(super.listIterator(index));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        Iterator<StackItem> i = descendingIterator();
        while (i.hasNext()) {
            if (sb.length() > 1)
                sb.append(',');
            sb.append(i.next());
        }
        sb.append(']');
        return sb.toString();
    }

    public String toLongString(boolean topAtLeft) {
        return toLongString(topAtLeft, null);
    }

    public String toLongString(boolean topAtLeft, StackItem.Type forceType) {
        StringBuilder sb = new StringBuilder();
        sb.append("stack[").append(size()).append("]");
        if (topAtLeft)
            sb.append(" top");
        sb.append(" [");
        Iterator<StackItem> it = topAtLeft ? descendingIterator() : iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first)
                first = false;
            else
                sb.append(", ");
            sb.append(it.next().toString(false, forceType));
        }
        sb.append("]");
        if (!topAtLeft)
            sb.append(" top");
        return sb.toString();
    }

    private class StackIterator implements ListIterator<StackItem> {

        private final Iterator<StackItem> delegate;
        private StackItem last;

        public StackIterator(Iterator<StackItem> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public StackItem next() {
            last = delegate.next();
            return last;
        }

        @Override
        public void remove() {
            stackBytes -= last.length();
            poppedItems.add(last);
            delegate.remove();
            last = null;
        }

        @Override
        public boolean hasPrevious() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StackItem previous() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(StackItem stackItem) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(StackItem stackItem) {
            throw new UnsupportedOperationException();
        }


    }


    @Override
    public int indexOf(Object o) {
        return super.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return super.lastIndexOf(o);
    }

    @Override
    public StackItem element() {
        return super.element();
    }



    // NOT IMPLEMENTED

    @Override
    public boolean addAll(int index, Collection<? extends StackItem> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem removeFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem removeLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(StackItem stackItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, StackItem element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem poll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(StackItem stackItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offerFirst(StackItem stackItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offerLast(StackItem stackItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void push(StackItem stackItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StackItem pop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<StackItem> spliterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(UnaryOperator<StackItem> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super StackItem> filter) {
        return false;
    }

    @Override
    public Stream<StackItem> stream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<StackItem> parallelStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(Consumer<? super StackItem> action) {
    }

}
