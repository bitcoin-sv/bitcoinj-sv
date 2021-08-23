/*
 * Copyright (c) 2012 Steve Shadders
 * Open BSV Licence, see the accompanying file LICENSE
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

import java.util.List;

/**
 * @author Steve Shadders
 * @since 2012
 */
public class ByteArrayLayeredMerkleTree extends AbstractLayeredMerkleTree<byte[], ByteArrayMerkleBranch> {

	public ByteArrayLayeredMerkleTree() {
		super();
	}

	public ByteArrayLayeredMerkleTree(boolean ignore, List<List<byte[]>> levels) {
		super(ignore, levels);
	}

	public ByteArrayLayeredMerkleTree(List<byte[]> elements, boolean build) {
		super(elements, build);
	}

	public ByteArrayLayeredMerkleTree(List<byte[]> elements) {
		super(elements);
	}

	@Override
	public byte[] makeParent(byte[] left, byte[] right) {
		return Sha256Hash.hashTwice(left, 0, left.length, right, 0, right.length);
	}

	@Override
	protected AbstractMerkleBranch<byte[]> newBranch(int index, byte[] node, byte[] root, List<byte[]> branch) {
		return new ByteArrayMerkleBranch(index, node, root, branch);
	}


}
