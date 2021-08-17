/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Steve Shadders
 * @since 2012
 * @param <D>
 */
public abstract class AbstractMerkleBranch<D> implements Serializable {

	//AbstractMerkleTree<D, ?> tree;

	/**
	 * index of the node in the original tree
	 */
	int nodeIndex;
	
	D node;
	
	D root;
	
	List<D> branchNodes;

	public <B extends AbstractMerkleBranch<D>> AbstractMerkleBranch(AbstractMerkleTree<D, B> tree, int nodeIndex,
                                                                                              List<D> branches) {
		super();
		this.node = tree.getNode(nodeIndex);
		this.root = tree.getRoot();
		this.nodeIndex = nodeIndex;
		this.branchNodes = branches;
	}
	
	public <B extends AbstractMerkleBranch<D>> AbstractMerkleBranch(int nodeIndex, List<D> branches) {
		super();
		this.nodeIndex = nodeIndex;
		this.branchNodes = branches;
	}
	
	public <B extends AbstractMerkleBranch<D>> AbstractMerkleBranch(int nodeIndex, D node, D root, List<D> branches) {
		super();
		this.node = node;
		this.nodeIndex = nodeIndex;
		this.root = root;
		this.branchNodes = branches;
	}

	/**
	 * Validate that this branch solves for the given merkle root
	 * 
	 * @param node
	 *            the node to check, this is required as the Satoshi brand of
	 *            merkle branch does not include the actual node.
	 * @param root
	 *            The merkleroot to validate against
	 */
	public boolean validate(D node, D root) {
		// for each level the branchNode only provides the counterpart nodes. We must
		// determine the order of the nodes
		// and calculate the other using the children from the previous
		// iteration.

		D parent = node; //the node or calculated node
		int indexInLevel = nodeIndex;
		D branchNode; //the counterpart that must be provided
		D left;
		D right;
		for (int level = 0; level < branchNodes.size(); level++) {
			branchNode = branchNodes.get(level);
			if (indexInLevel % 2 == 0) {
				left = parent;
				right = branchNode;
			} else {
				right = parent;
				left = branchNode;
			}
			parent = makeParent(level, branchNodes.size(), left, right);
			indexInLevel /= 2;
		}
		return parent.equals(root);
	}
	
	/**
	 * Validate that this branch solves for the given merkle root
	 * @return true if solved
	 * @throws IllegalStateException if the branch was constructed without providing a non-null node and merkle root.
	 */
	public boolean validate() throws IllegalStateException {
		try {
			return validate(node, root);
		} catch (NullPointerException e) {
			throw new IllegalStateException("No merkle root or node has been provided.  Cannot validate a merkle branch with both.");
		}
	}

	protected abstract D makeParent(int level, int levels, D left, D right);

	/**
	 * @return index of the node in the original merkle tree.
	 */
	public int getNodeIndex() {
		return nodeIndex;
	}

	/**
	 * @return the node for whom this branch validates.
	 */
	public D getNode() {
		return node;
	}

	/**
	 * @return the root of the parent merkle tree.
	 */
	public D getRoot() {
		return root;
	}

	/**
	 * @return the branches
	 */
	public List<D> getBranchNodes() {
		return branchNodes;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("index: ").append(nodeIndex);
		sb.append(" node: ").append(node);
		sb.append(" branch: ").append(branchNodes);
		sb.append(" root: ").append(root);
		return sb.toString();
	}

	/**
     * Document this properly (its int math and has some caveats)
	 * and move it to BitcoinJ Utils class.
	 * @param num
     * @return
     */
	public static int log2(int num) {
		if (num == 0)
			return 0;
		return 31 - Integer.numberOfLeadingZeros(num);
	}

}
