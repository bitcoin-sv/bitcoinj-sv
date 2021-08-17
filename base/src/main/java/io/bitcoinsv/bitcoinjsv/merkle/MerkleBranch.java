/*
 * © 2020 Bitcoin Association
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Steve Shadders
 * @since 2012
 *
 */
public class MerkleBranch extends AbstractMerkleBranch<Sha256Hash> implements Serializable {

	private static MerkleTree instance = new MerkleTree(); 
	
	public MerkleBranch(MerkleTree tree, int nodeIndex, List<Sha256Hash> branch) {
		super(tree, nodeIndex, branch);
	}

	public MerkleBranch(int nodeIndex, List<Sha256Hash> branch) {
		super(nodeIndex, branch);
	}

	public MerkleBranch(int nodeIndex, Sha256Hash node, Sha256Hash root, List<Sha256Hash> branch) {
		super(nodeIndex, node, root, branch);
	}

	@Override
	protected Sha256Hash makeParent(int level, int levels, Sha256Hash left, Sha256Hash right) {
		return instance.makeParent(level, levels, left, right);
	}
	
	/**
	 * 
	 * @param stream
	 */
	public void bitcoinSerialize(OutputStream stream) {
		throw new RuntimeException("not implemented");
	}

}
