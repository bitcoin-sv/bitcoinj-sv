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
public class StringMerkleBranch extends AbstractMerkleBranch<String> {

	private static final StringMerkleTree treeInstance = new StringMerkleTree();
	
	public StringMerkleBranch(StringMerkleTree tree, int nodeIndex, List<String> branch) {
		super(tree, nodeIndex, branch);
	}

	public StringMerkleBranch(int nodeIndex, List<String> branch) {
		super(nodeIndex, branch);
	}

	public StringMerkleBranch(int nodeIndex, String node, String root,
			List<String> branch) {
		super(nodeIndex, node, root, branch);
	}

	@Override
	protected String makeParent(int level, int levels, String left, String right) {
		return treeInstance.makeParent(level, levels, left, right);
	}

}
