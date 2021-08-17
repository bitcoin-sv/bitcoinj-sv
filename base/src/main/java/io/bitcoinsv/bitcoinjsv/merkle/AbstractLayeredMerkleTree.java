/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import java.util.*;

/**
 * 
 * @author Steve Shadders
 * @since 2012
 * @param <D>
 * @param <B>
 */
public abstract class AbstractLayeredMerkleTree<D, B extends AbstractMerkleBranch<D>> {

	private static final int VALID = 0;
	private static final int LEFT_INVALID = 1;
	private static final int RIGHT_INVALID = 2;
	/**
	 * If a centre node is modified/inserted/removed we have to rebuild the
	 * whole tree. Rebuilding from the edges is easier to do incrementally.
	 */
	private static final int CENTRE_INVALID = 4;

	private int leftMostInvalid = -1;

	private int validState = 0;

	List<List<D>> levels = new ArrayList();

	private Set<Integer> invalidCentreNodes;
	
	private int recalculations = 0;

	// private List<MerkleNode<D>> elements;
	// private D root;

	public abstract D makeParent(D left, D right);
	
	private D _makeParent(D left, D right) {
		recalculations++;
		return makeParent(left, right);
	}

	public AbstractLayeredMerkleTree() {
		validState = CENTRE_INVALID;
		build(newLevel(0, 1, 10));
	}

	public AbstractLayeredMerkleTree(List<D> elements) {
		this(elements, true);
	}

	/**
	 * Creates a MerkleTree that has been prebuilt. Assumes
	 * 
	 * @param ignore
	 *            not used, just a marker to differentiate constructors
	 * @param levels
	 *            A list of tree levels beginning with leaf nodes at index 0.
	 */
	public AbstractLayeredMerkleTree(boolean ignore, List<List<D>> levels) {
		this.levels = levels;
		validState = VALID;
	}

	public AbstractLayeredMerkleTree(List<D> elements, boolean build) {
		if (build) {
			List<D> list = newLevel(0, MerkleTree.levels(elements.size()), 10);
			list.addAll(elements);
			build(list);
		} else {
			levels.add(elements);
		}
		validState = build ? VALID : CENTRE_INVALID;
	}

	/**
	 * Adds a new leaf to the tree. Any MerkleBranches previously derived from
	 * this tree will no longer be valid.
	 * 
	 * @param node
	 * @param recalculate
	 *            recalculate immediately. If false then the tree's internal
	 *            state will not be valid until a call to recalculate() or
	 *            recalculateRight()
	 * @return the index of the newly added node.
	 */
	public int addNode(D node, boolean recalculate) {
		List<D> elements = levels.get(0);
		if (leftMostInvalid < 0) {
			leftMostInvalid = elements.size();
		}
		validState = validState | RIGHT_INVALID;
		elements.add(node);
		if (recalculate) {
			recalculateRight();
		}
		return elements.size() - 1;
	}

	/**
	 * Adds a new leaf to the tree and recalculates internally
	 * 
	 * @param node
	 * @return the index of the newly added node.
	 */
	public int addNode(D node) {
		return addNode(node, true);
	}

	/**
	 * Sets the first node in tree (typically the coinbase transaction) to a new
	 * value and recalculates the left most branch of the tree.
	 * 
	 * @param first
	 */
	public void setFirstNode(D first) {
		setFirstNode(first, true);
	}

	/**
	 * Changes (possibly) a centre node and recalculates the branch.
	 * 
	 * @param index
	 * @param node
	 */
	public void setNode(int index, D node) {
		setNode(index, node, true);
	}

	/**
	 * Changes (possibly) a centre node and marks it invalid.
	 * 
	 * @param index
	 * @param node
	 * @param recalculate
	 */
	public void setNode(int index, D node, boolean recalculate) {
		List<D> elements = levels.get(0);
		validState = validState | CENTRE_INVALID;
		elements.set(index, node);
		if (invalidCentreNodes == null)
			invalidCentreNodes = new HashSet<Integer>();
		/*
		 * Round node index down to even number. If it's counterpart is also
		 * replaced it will be recalculated as part of the same branch.
		 */
		invalidCentreNodes.add(index - index % 2);
		if (recalculate) {
			recalculate();
		}
	}

	/**
	 * Sets the first node in tree (typically the coinbase transaction) to a new
	 * value. Any MerkleBranches previously derived from this tree will no
	 * longer be valid.
	 * 
	 * @param first
	 * @param recalculate
	 */
	public void setFirstNode(D first, boolean recalculate) {
		List<D> elements = levels.get(0);
		validState = validState | LEFT_INVALID;
		elements.set(0, first);
		if (recalculate) {
			recalculateLeft();
		}
	}

	/**
	 * Recalculates the right hand part of the tree starting at the element n+1
	 * where n was the last element in the tree the last time it was in a valid
	 * state.
	 * 
	 * No checks on the rest of the tree state are performed. It is assumed if
	 * you are calling this method you are tracking the tree's valid state
	 * externally.
	 * 
	 */
	private void recalculateRight() {
		recalculateRight(leftMostInvalid);
	}

	/**
	 * Recalculates the right hand part of the tree starting at the element n+1
	 * where n was the last element in the tree the last time it was in a valid
	 * state.
	 * 
	 * No checks on the rest of the tree state are performed. It is assumed if
	 * you are calling this method you are tracking the tree's valid state
	 * externally.
	 * 
	 * @param leftMostInvalid
	 *            the leftmost node index that should be calculated from
	 */
	private void recalculateRight(int leftMostInvalid) {
		if (leftMostInvalid < 0) {
			return;
		}
		List<D> level;
		List<D> parentLevel;
		int offset = leftMostInvalid;
		int totalLevels = MerkleTree.levels(levels.get(0).size());
		for (int levelIndex = 0; levelIndex < levels.size(); levelIndex++) {
			level = levels.get(levelIndex);
			if (levels.size() - 1 <= levelIndex) {
				levels.add(newLevel(levels.size(), totalLevels, level.size() / 2 + 2));
				// hitRoot = true;
			}
			parentLevel = levels.get(levelIndex + 1);
			int inc = 2;
			for (int i = offset; i < level.size(); i += inc) {
				D left;
				D right;
				if (i % 2 == 0) {
					left = level.get(i);
					right = level.size() - 1 > i ? level.get(i + 1) : left;
					inc = 2;
				} else {
					left = level.get(i - 1);
					right = level.get(i);
					inc = 1;
				}
				int targetIndex = i / 2;
				if (parentLevel.size() > targetIndex) {
					parentLevel.set(i / 2, _makeParent(left, right));
				} else {
					parentLevel.add(_makeParent(left, right));
				}
			}
			if (parentLevel.size() == 1) {
				break;
			}
			offset = offset / 2;
		}
		// unset the invalid flag.
		validState = validState & ~RIGHT_INVALID;
		this.leftMostInvalid = -1;
	}

	/**
	 * Recalculates the leftmost branch of the tree. This would typically be
	 * used when altering the state of the coinbase transaction e.g. to
	 * increment extranonce.
	 * 
	 * No checks on the rest of the tree state are performed. It is assumed if
	 * you are calling this method you are tracking the tree's valid state
	 * externally.
	 */
	
	private void recalculateLeft() {
		recalculateLeft(true);
	}
	
	private void recalculateLeft(boolean clearRecalcs) {
		if (clearRecalcs) {
			recalculations = 0;
		}
		recalculateBranch(0);
		// List<D> level;
		// for (int levelIndex = 0; levelIndex < levels.size() -1; levelIndex++)
		// {
		// level = levels.get(levelIndex);
		// D left = level.get(0);
		// levels.get(levelIndex + 1).set(0, makeParent(left, level.size() > 1 ?
		// level.get(1) : left));
		// }
		// unset the invalid flag.
		validState = validState & ~LEFT_INVALID;
	}

	private void recalculateBranch(int index) {

		// round down to even number
		index -= index % 2;
		int parentIndex;
		List<D> level;
		List<D> parentLevel;
		D left;
		D right;

		for (int levelIndex = 0; levelIndex < levels.size() - 1; levelIndex++) {
			level = levels.get(levelIndex);
			parentLevel = levels.get(levelIndex + 1);
			// invalidCentreNodes only contains even indexes so we don't
			// have to worry about whether it's a left or right node.
			left = level.get(index);
			right = level.size() > index + 1 ? level.get(index + 1) : left;
			parentIndex = index / 2;
			parentLevel.set(parentIndex, _makeParent(left, right));
			index = parentIndex - parentIndex % 2;
		}

	}

	/**
	 * recalculates all nodes marked as invalid.
	 *
	 * FIXME this is broken, it should recalculate layer by layer
	 * otherwise a pair higher up in the tree may be derived from
	 * an un-recalculated node.
	 * @deprecated
	 */
	private void recalculateCentreOld() {
		if (invalidCentreNodes == null || invalidCentreNodes.isEmpty())
			return;
		for (Integer index : invalidCentreNodes) {
			recalculateBranch(index);
		}
		invalidCentreNodes.clear();
		// unset the invalid flag.
		validState = validState & ~CENTRE_INVALID;
	}

	private void recalculateCentre() {
		if (invalidCentreNodes == null || invalidCentreNodes.isEmpty())
			return;
		Set<Integer> nextLayerIndexes = new HashSet();
		Set<Integer> thisLayer = invalidCentreNodes;

		int parentIndex;
		List<D> level;
		List<D> parentLevel;
		D left;
		D right;

		for (int i = 0; i < levels.size() - 1; i++) {

			level = levels.get(i);
			parentLevel = levels.get(i + 1);

			for (Integer index : thisLayer) {
				// invalidCentreNodes only contains even indexes so we don't
				// have to worry about whether it's a left or right node.
				left = level.get(index);
				right = level.size() > index + 1 ? level.get(index + 1) : left;
				parentIndex = index / 2;
				parentLevel.set(parentIndex, _makeParent(left, right));

				nextLayerIndexes.add(parentIndex - parentIndex % 2);
			}
			thisLayer = nextLayerIndexes;
			nextLayerIndexes = new HashSet<>();
		}
		invalidCentreNodes.clear();
		// unset the invalid flag.
		validState = validState & ~CENTRE_INVALID;
	}

	/**
	 * Rebuilds the entire tree.
	 */
	public void rebuild() {
		build(levels.get(0));
	}

	/**
	 * Recalculates those parts of the tree that are invalid. If a centre node
	 * has been modified, inserted or removed the entire tree will be rebuilt.
	 * If nodes are added or the first node has been modified then an
	 * incremental calculation will be performed.
	 */
	public void recalculate() {
		recalculations = 0;
		if ((validState & CENTRE_INVALID) == CENTRE_INVALID) {
			recalculateCentre();
		}
		if ((validState & RIGHT_INVALID) == RIGHT_INVALID) {
			// if the right hand check starts from nodes 0 or 1 the left most
			// branch is recalculated
			// as a part of the operation.
			boolean skipLeft = leftMostInvalid > 1;
			recalculateRight(leftMostInvalid);
			if (skipLeft) {
				return;
			}
		}
		if ((validState & LEFT_INVALID) == LEFT_INVALID) {
			recalculateLeft();
		}
		validState = VALID;
	}

	private void build(List<D> level) {
		recalculations = 0;
		levels.clear();
		// List<MerkleNode<D>> base = new ArrayList(levelData.size() + 5);
		// base.addAll(levelData);
		levels.add(level);

		// elements.addAll(levelData);
		int totalLevels = MerkleTree.levels(levels.get(0).size());
		while (level.size() > 1) {
			List<D> parentLevel = newLevel(levels.size(), totalLevels, levels.size() / 2 + 5);
			levels.add(parentLevel);
			for (int i = 0; i < level.size(); i++) {
				D left = level.get(i);
				D right;
				if (level.size() >= ++i + 1) {
					right = level.get(i);
				} else {
					right = left;
				}
				// D parentData = makeParent(left.data, right.data);
				// MerkleNode<D> parent = new MerkleNode<D>(parentData, this);
				D parent = _makeParent(left, right);
				// parent.index = parentLevel.size();
				// parent.level = levels.size() - 1;
				parentLevel.add(parent);
			}
			// elements.addAll(parents);
			// swap them over but reused the datas list
			level = parentLevel;
		}
		validState = 0;
		leftMostInvalid = -1;
	}

	/**
	 * Override to provide a custom list implementation.
	 * 
	 * @param currentLevels
	 * @param size
	 * @return
	 */
	protected List<D> newLevel(int currentLevels, int totalLevels, int size) {
		return new ArrayList<D>(size);
	}

	/**
	 * Override to provide the matching type of branch
	 * 
	 * @param index
	 * @param branch
	 * @return
	 */
	protected abstract AbstractMerkleBranch<D> newBranch(int index, D node, D root, List<D> branch);

	protected AbstractMerkleBranch<D> getBranch(int index) {
		ArrayList<D> branch = new ArrayList<D>(levels.size() - 1);
		AbstractMerkleBranch<D> merkleBranch = newBranch(index, getNode(index), getRoot(), branch);
		for (int levelIndex = 0; levelIndex < levels.size() - 1; levelIndex++) {
			List<D> level = levels.get(levelIndex);

			// index^1 flips the last bit. So if index is odd it becomes index
			// -1
			// if index is even it becomes index + 1. This gives the index of
			// the node's matching pair.
			// Equivalent code would be:
			// index % 2 == 0 ? index + 1 : index -1
			int i = Math.min(index ^ 1, level.size() - 1);
			branch.add(level.get(i));

			// right bitshift is equivilent to (index) / 2 (integer op so
			// remainder is dropped)
			index >>= 1;
		}
		return merkleBranch;
	}

	public int size() {
		return levels.get(0).size();
	}
	
	public int sizeAll() {
		int size = 0;
		for (List level: levels)
			size += level.size();
		return size;
	}
	
	public D getNode(int index) {
		return levels.get(0).get(index);
	}

	public int indexOf(D node) {
		return levels.get(0).indexOf(node);
	}

	/**
	 * @return the merkle root of the tree
	 */
	public D getRoot() {
		if (size() == 0)
			return null;
		return levels.get(levels.size() - 1).get(0);
	}

	/**
	 * @return The leaf nodes of the tree
	 */
	public List<D> getElements() {
		return Collections.unmodifiableList(levels.get(0));
	}

	/**
	 * Returns the entire tree as a single list. This is way a tree is
	 * internally represented in the Satoshi client.
	 * 
	 * The list begins with leaf nodes of tree and adds each level in sequence.
	 * The merkle root is the last element of the list
	 * 
	 * @return
	 */
	public List<D> getAsList() {
		ArrayList<D> list = new ArrayList<D>(levels.get(0).size() * 2 - 1);
		for (List<D> level : levels) {
			list.addAll(level);
		}
		return list;
	}

	public boolean isRoot(int level) {
		return level == levels.size() - 1;
	}

	public boolean isLeaf(int level) {
		return level == 0;
	}

	/**
	 * @param level
	 *            The level
	 * @param index
	 *            The index within the level
	 * @return true if this node is duplicated in it's level. i.e. it is the
	 *         last node in the level and the level size is not even.
	 */
	private boolean isOdd(int level, int index) {
		List<D> levelList = levels.get(level);
		return levelList.size() - 1 == index && index % 2 != 0;
	}

	/**
	 * @param level
	 * @param index
	 * @return Parent node in the tree or null if this is the root node
	 */
	private D getParent(int level, int index) {
		return isRoot(level) ? null : levels.get(level + 1).get(index / 2);
	}

	/**
	 * sibling node in the tree or null if this an end node with no sibling
	 * 
	 * @param level
	 * @param index
	 * @return
	 */
	private D getSibling(int level, int index) {
		List<D> levelList = levels.get(level);
		if (index == levelList.size() - 1) {
			return null;
		}
		return levelList.get(index ^ 1);
		// index^1 is the equivalent of index % 2 == 0 ? index + 1 : index - 1
		// return index % 2 == 0 ? levelList.get(index + 1) :
		// levelList.get(index - 1);
	}

	/**
	 * @return true if the internal state of the tree is valid. Adding or
	 *         changing nodes will put the tree into an invalid state until
	 *         calculate() is called
	 */
	public boolean isValid() {
		return validState != 0;
	}

	public String toString() {
		recalculate();
		List<D> tree = new ArrayList(levels.get(0).size() * 2);
		for (List<D> level : levels)
			for (D d : level)
				tree.add(d);
		return String.valueOf(tree);
	}

	public String toStringTree() {
		StringBuilder sbAll = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		StringBuilder sbInt = new StringBuilder();
		// List<Integer> centres = new ArrayList();
		// List<Integer> parentCentres = new ArrayList();

		Map<D, Integer> centrePoints = new HashMap<D, Integer>();

		boolean reachedRoot = false;
		D first;
		int level = 0;
		while (level < levels.size()) {
			List<D> nodes = levels.get(level);
			for (int i = 0; i < nodes.size(); i++) {
				D left = nodes.get(i);
				D right = ++i < nodes.size() ? nodes.get(i) : left;
				Integer centre = centrePoints.get(left);
				if (centre != null) {
					sb.append(spaces(centre - sb.length() - (left.toString().length() / 2)));
				}
				sb.append(left);
				sb.append("   ");
				// centrePoints.put(left.getParent(), sb.length() - 2);
				centrePoints.put(getParent(level, i), sb.length() - 2);
				sbInt.append(spaces(sb.length() - 3 - sbInt.length())).append("/");
				if (level < levels.size() - 1) {
					sb.append(right);
					sb.append("   ");
					sbInt.append(spaces(sb.length() - 3 - right.toString().length() - sbInt.length())).append("\\");
				}
			}
			sbAll.insert(0, "\n").insert(0, sb);
			level++;
			if (level < levels.size()) {
				sbAll.insert(0, "\n").insert(0, sbInt);
			}
			sb.setLength(0);
			sbInt.setLength(0);

		}
		return sbAll.toString();
	}

	private String spaces(int num) {
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
