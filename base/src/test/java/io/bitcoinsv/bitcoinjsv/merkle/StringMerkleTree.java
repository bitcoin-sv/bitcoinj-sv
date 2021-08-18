/**
 * Copyright (c) 2012 Steve Shadders.
 * All rights reserved.
 */
package io.bitcoinsv.bitcoinjsv.merkle;

import java.util.List;

/**
 *
 * @author Steve Shadders
 * @since 2012
 *
 */
public class StringMerkleTree extends AbstractMerkleTree<String, StringMerkleBranch> {

	public StringMerkleTree(List<String> elements) {
		super(elements);
	}

	StringMerkleTree() {
		super();
	}

	@Override
	public String makeParent(int level, int levels, String left, String right) {
		return "(" + left + right + ")";
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
