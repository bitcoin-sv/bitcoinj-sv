/**
 * Copyright (c) 2012 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.merkle;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Steve Shadders
 * @since 2012
 * @param <D>
 * @param <B>
 */
public abstract class AbstractMerkleTree<D, B extends AbstractMerkleBranch<D>> implements Serializable {

	private static final int VALID = 0;
	private static final int LEFT_INVALID = 1;
	/**
	 * If any node other than left or left + 1 node is modified/inserted/removed
	 * we have to rebuild the whole tree. Rebuilding from the edges is easier to
	 * do incrementally.
	 */
	private static final int RIGHT_INVALID = 2;
	private static final int CENTRE_INVALID = 4;

	private int validState = 0;

	List<D> tree = new ArrayList();

	int nodeCount;
	
	private int recalculations = 0;

	/**
	 * Only needed by MerkleBranch to get access to the makeParent(left, right) method
	 */
	AbstractMerkleTree() {
		validState = RIGHT_INVALID;
		nodeCount = 0;
	}

	public AbstractMerkleTree(List<D> elements) {
		this(elements, true);
	}

	public AbstractMerkleTree(List<D> elements, boolean build) {
		tree.addAll(elements);
		nodeCount = elements.size();
		validState = RIGHT_INVALID;
		if (build) {
			build();
		}
		
	}

	/**
	 * Adds a new leaf to the tree. Any MerkleBranches previously derived from
	 * this tree will no longer be valid.
	 * 
	 * @param node
	 */
	public void addNode(D node) {
		 validState = validState | RIGHT_INVALID;
		 if (nodeCount == 0) {
			 tree.add(node);
		 } else if (tree.size() > nodeCount) {
			 tree.set(nodeCount, node);
		 } else {
			 tree.add(node);
		 }
		 nodeCount++;
	}

	/**
	 * Sets the first node in tree (typically the coinbase transaction) to a new
	 * value. Any MerkleBranches previously derived from this tree will no
	 * longer be valid.
	 * 
	 * @param first
	 */
	public void setFirstNode(D first) {
		validState = validState | LEFT_INVALID;
		if (tree.size() > 0) {
			tree.set(0, first);
		} else {
			nodeCount = 1;
			tree.add(first);
			//no need to recalculate for a single node tree as the node is also the root.
		}

	}
	
	public void setNode(int index, D node) {
		tree.set(index, node);
		validState = validState | CENTRE_INVALID;
	}

	/**
	 * Recalculates the leftmost branch of the tree. This would typically be
	 * used when altering the state of the coinbase transaction e.g. to
	 * increment extranonce.
	 * 
	 * No checks on the rest of the tree state are performed
	 */
	private void recalculateLeft() {
		int levelOffset = 0;
		int levelSize = nodeCount;
		int level = 0;
		int levels = levels(nodeCount);
		while (levelSize > 1) {
			D left = tree.get(levelOffset);
			D right = levelSize > 1 ? tree.get(levelOffset + 1) : left;
			levelOffset += levelSize;
			tree.set(levelOffset, _makeParent(level, levels, left, right));
			levelSize = (levelSize + 1) / 2;
			level++;
		}
		 //unset the invalid flag.
		 validState = validState & ~LEFT_INVALID;
	}

	/**
	 * Makes a parent node.  The additional parameters allow customizing hash function
	 * based on level.  For a standard merkle tree these are not used.
	 * 
	 * @param level current level (of the left/right child nodes)
	 * @param levels total levels in the tree
	 * @param left node
	 * @param right node
	 * @return
	 */
	protected abstract D makeParent(int level, int levels, D left, D right);
	
	private D _makeParent(int level, int levels, D left, D right) {
		recalculations++;
		return makeParent(level, levels, left, right);
	}

	/**
	 * Recalculates those parts of the tree that are invalid. If a centre node
	 * has been modified, inserted or removed the entire tree will be rebuilt.
	 * If the first or 2nd node has been modified or added then an
	 * incremental calculation will be performed.
	 */
	private void makeValid() {
		if (validState == VALID)
			return;
		 if ((validState & RIGHT_INVALID) == RIGHT_INVALID
				 || (validState & CENTRE_INVALID) == CENTRE_INVALID) {
			 build();
			 return;
		 }
		 if ((validState & LEFT_INVALID) == LEFT_INVALID) {
			 recalculateLeft();
		 }
		 validState = VALID;
	}

	private void build() {
		recalculations = 0;
		// remove any excess elements past the base level of the tree;
		if (tree.size() > nodeCount) {
			// clearing is faster than making a new list and adding the elements
			tree.subList(nodeCount, tree.size()).clear();
		}

		int levelOffset = 0; // Offset in the list where the currently processed
								// level starts.
		// Step through each level, stopping when we reach the root (levelSize
		// == 1).
		int level = 0;
		int levels = levels(nodeCount);
		for (int levelSize = tree.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
			// For each pair of nodes on that level:
			for (int left = 0; left < levelSize; left += 2) {
				// The right hand node can be the same as the left hand, in the
				// case where we don't have enough
				// transactions.
				int rightIndex = Math.min(left + 1, levelSize - 1);
				D leftNode = tree.get(levelOffset + left);
				D rightNode = tree.get(levelOffset + rightIndex);
				tree.add(_makeParent(level, levels, leftNode, rightNode));
			}
			// Move to the next level.
			levelOffset += levelSize;
		}
		validState = 0;
	}

	
	
	/**
	 * Override to provide the matching type of branch
	 * 
	 * @param index
	 * @param branch
	 * @return
	 */
	protected abstract AbstractMerkleBranch<D> newBranch(int index, D node, D root, List<D> branch);

	public AbstractMerkleBranch<D> getBranch(int index) {
		ArrayList<D> branch = new ArrayList<D>();
		AbstractMerkleBranch<D> merkleBranch = newBranch(index, getNode(index), getRoot(), branch);
		int j = 0;
		for (int nSize = nodeCount; nSize > 1; nSize = (nSize + 1) / 2) {
			// index^1 flips the last bit. So if index is odd it becomes
			// index - 1,
			// if index is even it becomes index + 1. 
			// This gives the index of the
			// node's matching pair.
			// Equivalent code would be:
			// index % 2 == 0 ? index + 1 : index -1
			int i = Math.min(index ^ 1, nSize - 1);
			branch.add(tree.get(j + i));

			// right bit shift is equivalent to (index) / 2 (integer op so
			// remainder is dropped)
			index >>= 1;
			j += nSize;
		}
		return merkleBranch;
	}

	public D getNode(int index) {
		makeValid();
		return tree.get(index);
	}

	public int indexOf(D node) {
		makeValid();
		return tree.indexOf(node);
	}

	/**
	 * @return the merkle root of the tree
	 */
	public D getRoot() {
		makeValid();
		return tree.get(tree.size() - 1);
	}
	
	/**
	 * @return the number of leaf nodes in the tree.
	 */
	public int nodeCount() {
		return nodeCount;
	}
	
	/**
	 * Returns the size of the entire tree.  For the number of leaf nodes use nodeCount()  
	 */
	public int size() {
		makeValid();
		return tree.size();
	}

	/**
	 * @return a read only list containing the leaf nodes of the tree.
	 */
	public List<D> getElements() {
		//don't need to call makeValid here as the part of the backing list
		//that contains the elements should always be consistent.
		return Collections.unmodifiableList(tree.subList(0, nodeCount));
	}

	/**
	 * Returns the entire tree as a single read only list. This is the way a
	 * tree is internally represented in the Satoshi client.
	 * 
	 * The list begins with leaf nodes of tree and adds each level in sequence.
	 * The merkle root is the last element of the list
	 * 
	 * @return
	 */
	public List<D> getAsList() {
		makeValid();
		return Collections.unmodifiableList(tree);
	}

	public String toString() {
		makeValid();
		return String.valueOf(tree);
	}

	/**
	 * Return a String representation of the tree.
	 * @return
	 */
	public String toStringTree() {
		makeValid();
		StringBuilder sbAll = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		StringBuilder sbInt = new StringBuilder();

		Map<Integer, Integer> centrePoints = new HashMap<Integer, Integer>();

		boolean reachedRoot = false;
		D first;
		int level = 0;
		int levelOffset = 0;
		int levelSize = nodeCount;
		int levels = levels(nodeCount);
		while (level < levels) {
			// List<D> nodes = levels.get(level);
			int extraOffset = 0;
			for (int i = 0; i < levelSize; i++) {
				D left = tree.get(i + levelOffset);
				D right = ++i < levelSize ? tree.get(i + levelOffset) : left;
				Integer centre = centrePoints.get(i - 1 + levelOffset);
				if (centre != null) {
					int spaces = centre - sb.length() - (left.toString().length() / 2);
					sb.append(spaces(spaces + extraOffset));
					extraOffset = spaces > 0 ? 0 : -spaces;
				}
				sb.append(String.valueOf(left));
				centre = centrePoints.get(i + levelOffset);
				if (centre != null) {
					int spaces = centre - sb.length() - (left.toString().length() / 2);
					sb.append(spaces(spaces));
				} else {
					sb.append("   ");
				}
				centrePoints.put(indexOfParent(i, nodeCount), sb.length() - 2);
				sbInt.append(spaces(sb.length() - 3 - sbInt.length())).append("/");
				if (level < levels - 1 && i < levelSize) {
					sb.append(String.valueOf(right));
					sb.append("   ");
					sbInt.append(spaces(sb.length() - 3 - right.toString().length() - sbInt.length())).append("\\");
				}
			}
			sbAll.insert(0, "\n").insert(0, sb);
			levelOffset += levelSize;
			levelSize = (int) Math.ceil(levelSize / 2.0d);
			level++;
			if (level < levels) {
				sbAll.insert(0, "\n").insert(0, sbInt);
			}
			sb.setLength(0);
			sbInt.setLength(0);

		}
		return sbAll.toString();
	}
	
	/*
	 * The following set of static methods are only used by the toStringTree() method.  They are horribly
	 * inefficient.
	 */
	
	private static final double log2 = Math.log(2d);
	/**
	 * @param nodeCount
	 * @return the depth of a merkle tree with the given leaf node count.
	 */
	public static int levels(int nodeCount) {
		//java has no log2 function so we simulate it with Math.log(nodeCount) / Match.log(2)
		//what we really need the the index of the MSB.  
		return ((int) Math.ceil((Math.log(nodeCount) / log2))) + 1;
	}

	/**
	 * @param level
	 * @param nodeCount
	 * @return the starting index of the given level in a merkle tree with the
	 *         given leaf node count.
	 */
	public static int startIndexOf(int level, int nodeCount) {
		int cursor = 0;
		int size = nodeCount;
		for (int i = 0; i < level; i++) {
			cursor += size;
			//size = (int) Math.ceil(size / 2.0d);
			size = (size + 1) / 2;
		}
		return cursor;
	}

	/**
	 * give the level of the tree represented by the given index. This method is
	 * slow. It only exists for the toString method.
	 * 
	 * @param index
	 * @return level or -1 if index is outside of the tree
	 */
	public static int levelOf(int index, int nodeCount) {
		int cursor = 0;
		for (int level = 1; level <= levels(nodeCount); level++) {
			int newCursor = startIndexOf(level, nodeCount);
			if (index >= cursor && index < newCursor)
				return level - 1;
			cursor = newCursor;
		}
		return -1;
	}

	public static int indexOfParent(int childIndex, int nodeCount) {
		int levelOffset = startIndexOf(levelOf(childIndex, nodeCount), nodeCount);
		return indexOfParent(childIndex, levelOffset, nodeCount);
	}

	/**
	 * returns index of parent given the childIndex and child's level offset
	 * (the point in the underlying list where the child's level begins)
	 * 
	 * @param childIndex
	 * @param levelOffset
	 * @return
	 */
	public static int indexOfParent(int childIndex, int levelOffset, int nodeCount) {
		// offset offset of parent from the start of it's level.
		int parentIndex = (childIndex - levelOffset) >> 1;
		int parentLevelOffset = startIndexOf(levelOf(childIndex, nodeCount) + 1, nodeCount);
		return parentIndex + parentLevelOffset;
	}

	private static String spaces(int num) {
		if (num < 1) {
			return "";
		}
		StringBuilder sb = new StringBuilder(num);
		for (int i = 0; i < num; i++)
			sb.append(" ");
		return sb.toString();
	}

	/**
	 * @return the recalculations
	 */
	public int getRecalculations() {
		return recalculations;
	}

	/**
	 * @param recalculations the recalculations to set
	 */
	public void setRecalculations(int recalculations) {
		this.recalculations = recalculations;
	}
	
	

}
