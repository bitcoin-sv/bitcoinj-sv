package org.bitcoinj.script;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ScriptStack extends LinkedList<StackItem> {


    public ScriptStack(ScriptStack stack) {
        super(stack);
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

    public List<StackItem> unmodifiable() {
        return Collections.unmodifiableList(this);
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
        addLast(StackItem.from(bytes, derivedFrom));
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

}
