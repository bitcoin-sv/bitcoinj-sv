package org.bitcoinj.script;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ScriptStack extends LinkedList<StackItem> {

    private long stackBytes = 0;

    public ScriptStack(ScriptStack stack) {
        super(stack);
        stackBytes = stack.stackBytes;
    }

    public ScriptStack(Collection<? extends StackItem> c) {
        super();
        addAll(c);
    }

    public ScriptStack() {
        super();
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
            set(i, StackItem.wrapDerived(this.get(i).bytes, derived));
        }
    }

    /**
     * Returns the stack memory usage as defined by the genesis spec:
     * https://github.com/bitcoin-sv-specs/protocol/blob/master/updates/genesis-spec.md
     * @return
     */
    public long getStackMemoryUsage() {
        return 32 * size() * stackBytes;
    }

    /**
     * Returns the total bytes on the stack. This is NOT the value used to check maxstackmemoryusage for consensus
     * @return
     */
    public long getStackBytes() {
        return stackBytes;
    }

    public List<StackItem> unmodifiable() {
        return Collections.unmodifiableList(this);
    }

    public boolean add(StackItem item) {
        stackBytes += item.length;
        return super.add(item);
    }

    public boolean add(StackItem from, StackItem ... derivedFrom) {
        return add(StackItem.from(from, derivedFrom));
    }

    public boolean add(byte[] bytes, StackItem ... derivedFrom) {
        return add(StackItem.from(bytes, derivedFrom));
    }

    public void addLast(StackItem from, StackItem ... derivedFrom) {
        addLast(StackItem.from(from, derivedFrom));
    }

    public void addLast(byte[] bytes, StackItem ... derivedFrom) {
        stackBytes += bytes.length;
        addLast(StackItem.from(bytes, derivedFrom));
    }

    @Override
    public StackItem getLast() {
        return super.getLast();
    }

    @Override
    public StackItem pollLast() {
        StackItem item = super.pollLast();
        stackBytes -= item.length;
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
            stackBytes -= old.length;
        stackBytes += element.length;
        return old;
    }

    @Override
    public Iterator<StackItem> descendingIterator() {
        return new StackIterator(super.descendingIterator());
    }

    public Iterator<StackItem> iterator() {
        return new StackIterator(super.iterator());
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

    private class StackIterator implements Iterator<StackItem> {

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
            stackBytes -= last.length;
            delegate.remove();
            last = null;
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends StackItem> c) {
        throw new NotImplementedException();
    }

    @Override
    public StackItem removeFirst() {
        return super.removeFirst();
    }

    @Override
    public StackItem removeLast() {
        return super.removeLast();
    }

    @Override
    public void addFirst(StackItem stackItem) {
        super.addFirst(stackItem);
    }

    @Override
    public void addLast(StackItem stackItem) {
        super.addLast(stackItem);
    }

    @Override
    public boolean remove(Object o) {
        return super.remove(o);
    }

    @Override
    public void add(int index, StackItem element) {
        super.add(index, element);
    }

    @Override
    public StackItem remove(int index) {
        return super.remove(index);
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

    @Override
    public StackItem poll() {
        return super.poll();
    }

    @Override
    public StackItem remove() {
        return super.remove();
    }

    @Override
    public boolean offer(StackItem stackItem) {
        return super.offer(stackItem);
    }

    @Override
    public boolean offerFirst(StackItem stackItem) {
        return super.offerFirst(stackItem);
    }

    @Override
    public boolean offerLast(StackItem stackItem) {
        return super.offerLast(stackItem);
    }

    @Override
    public StackItem pollFirst() {
        return super.pollFirst();
    }

    @Override
    public void push(StackItem stackItem) {
        super.push(stackItem);
    }

    @Override
    public StackItem pop() {
        return super.pop();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return super.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return super.removeLastOccurrence(o);
    }

    @Override
    public ListIterator<StackItem> listIterator(int index) {
        return super.listIterator(index);
    }

    @Override
    public Spliterator<StackItem> spliterator() {
        return super.spliterator();
    }

    @Override
    public ListIterator<StackItem> listIterator() {
        return super.listIterator();
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<StackItem> operator) {

    }

    @Override
    public boolean removeIf(Predicate<? super StackItem> filter) {
        return false;
    }

    @Override
    public Stream<StackItem> stream() {
        return null;
    }

    @Override
    public Stream<StackItem> parallelStream() {
        return null;
    }

    @Override
    public void forEach(Consumer<? super StackItem> action) {

    }
}
