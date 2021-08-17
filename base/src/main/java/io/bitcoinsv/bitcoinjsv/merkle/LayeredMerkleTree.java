/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.util.List;

/**
 * @author Steve Shadders
 * @since 2012
 */
public class LayeredMerkleTree extends AbstractLayeredMerkleTree<Sha256Hash, MerkleBranch> {

	public LayeredMerkleTree() {
		super();
	}

	public LayeredMerkleTree(List elements) {
		super(elements);
	}

	public LayeredMerkleTree(List elements, boolean build) {
		super(elements, build);
	}

	@Override
	public Sha256Hash makeParent(Sha256Hash left, Sha256Hash right) {
		byte[] leftBytes = Utils.reverseBytes(left.getBytes());
		byte[] rightBytes = Utils.reverseBytes(right.getBytes());
		byte[] allbytes = new byte[64];
		System.arraycopy(leftBytes, 0, allbytes, 0, 32);
		System.arraycopy(rightBytes, 0, allbytes, 32, 32);
		//return new Sha256Hash(Utils.reverseBytes(Utils.doubleDigestTwoBuffers(leftBytes, 0, 32, rightBytes, 0, 32)));
		return Sha256Hash.create(Sha256Hash.hash(allbytes, 0, 64));
		
	}

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
//
//	@Override
//	protected AbstractMerkleBranch<Sha256Hash> newBranch(int index, List<Sha256Hash> branch) {
//		return new MerkleBranch(index, node, root, branch);
//	}

}
