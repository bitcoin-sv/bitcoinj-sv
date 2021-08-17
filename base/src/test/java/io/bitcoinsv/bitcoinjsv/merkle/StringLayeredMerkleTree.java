/**
 * Copyright (c) 2012 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Shadders
 * @since 2012
 */
public class StringLayeredMerkleTree extends AbstractLayeredMerkleTree<String, StringMerkleBranch> {
	
	public StringLayeredMerkleTree() {
	}

	public StringLayeredMerkleTree(List<String> elements) {
		super(elements);
	}

	public StringLayeredMerkleTree(List<String> elements, boolean build) {
		super(elements, build);
	}

	@Override
	public String makeParent(String left, String right) {
		return "(" + left + right + ")";
		//return left + right;
		// return "(.)(.)";
	}
	
	/* (non-Javadoc)
	 * @see com.google.bitcoin.core.merkle.AbstractMerkleTree#newBranch(int, java.lang.Object, java.lang.Object, java.util.List)
	 */
	@Override
	protected StringMerkleBranch newBranch(int index, String node, String root, List<String> branch) {
		return new StringMerkleBranch(index, node, root, branch);
	}


}

