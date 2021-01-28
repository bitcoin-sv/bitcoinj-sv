/**
 * Copyright (c) 2012 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinj.merkle;

import io.bitcoinj.core.Sha256Hash;

import java.util.List;

/**
 * @author Steve Shadders
 * @since 2012
 */
public class ByteArrayMerkleBranch extends AbstractMerkleBranch<byte[]> {

	public <B extends AbstractMerkleBranch<byte[]>> ByteArrayMerkleBranch(AbstractMerkleTree<byte[], B> tree,
                                                                                                    int nodeIndex, List<byte[]> branches) {
		super(tree, nodeIndex, branches);
	}

	public <B extends AbstractMerkleBranch<byte[]>> ByteArrayMerkleBranch(int nodeIndex, byte[] node, byte[] root,
                                                                                                    List<byte[]> branches) {
		super(nodeIndex, node, root, branches);
	}

	public <B extends AbstractMerkleBranch<byte[]>> ByteArrayMerkleBranch(int nodeIndex, List<byte[]> branches) {
		super(nodeIndex, branches);
	}

	@Override
	protected byte[] makeParent(int level, int levels, byte[] left, byte[] right) {
		return Sha256Hash.hashTwice(left, 0, left.length, right, 0, right.length);
	}

}
