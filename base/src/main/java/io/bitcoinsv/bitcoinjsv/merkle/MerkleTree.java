/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Steve Shadders
 * @since 2012
 *
 */
public class MerkleTree extends AbstractMerkleTree<Sha256Hash, MerkleBranch> implements Serializable {

	public MerkleTree() {
		super();
	}

	public MerkleTree(List<Sha256Hash> elements) {
		super(elements);
	}

	@Override
	public Sha256Hash makeParent(int level, int levels, Sha256Hash left, Sha256Hash right) {
		byte[] leftBytes = Utils.reverseBytes(left.getBytes());
		byte[] rightBytes = Utils.reverseBytes(right.getBytes());
		return new Sha256Hash(Utils.reverseBytes(Sha256Hash.hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
		
	}

	/* (non-Javadoc)
	 * @see com.google.bitcoin.core.merkle.AbstractMerkleTree#newBranch(int, java.lang.Object, java.lang.Object, java.util.List)
	 */
	@Override
	protected MerkleBranch newBranch(int index, Sha256Hash node, Sha256Hash root, List<Sha256Hash> branch) {
		return new MerkleBranch(index, node, root, branch);
	}

	/* (non-Javadoc)
	 * @see com.google.bitcoin.core.merkle.AbstractMerkleTree#getBranch(int)
	 */
	@Override
	public MerkleBranch getBranch(int index) {
		return (MerkleBranch) super.getBranch(index);
	}
	
	

}
