package io.bitcoinsv.bitcoinjsv.merkle;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMerkle {

    @Test
    public void testPlainMerkle() {

        List<String> list = Arrays.asList(new String[] { "1", "2", "3", "4", "5", "6" });

        StringMerkleTree tree = new StringMerkleTree(list);
        System.out.println(tree.toStringTree() + "\n");
        System.out.println(tree.toString() + "\n");

        int branchIndex = 3;

        for (int i = 0; i < list.size(); i++) {
            AbstractMerkleBranch branch = tree.getBranch(i);
            System.out.println(branch.toString());
            System.out.println(branch.validate(tree.getNode(i), tree.getRoot()));
            branch.validate();
        }

        tree.setFirstNode("*");
        tree.addNode("x");
        tree.addNode("y");
        tree.addNode("z");
        tree.addNode("Q");
        System.out.println(tree.toStringTree() + "\n");

        assertTrue("((((*2)(34))((56)(xy)))(((zQ)(zQ))((zQ)(zQ))))".equals(tree.getRoot()));
    }

    @Test
    public void testLayeredMatchesPlain() {
        List<String> list = Arrays.asList(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" });

        StringMerkleTree plain = new StringMerkleTree(list);
        //StringLayeredMerkleTree plain = new StringLayeredMerkleTree(list);
        StringLayeredMerkleTree layered = new StringLayeredMerkleTree(list);


        System.out.println("Plain root:   " + plain.getRecalculations());
        System.out.println("Layered root: " + layered.getRecalculations());
        plain.setRecalculations(0);
        layered.setRecalculations(0);

        plain.addNode("11");
        System.out.println("Plain root:   " + plain.getRoot());
        System.out.println("Plain root:   " + plain.getRecalculations());
        layered.addNode("11");
        System.out.println("Layered root: " + layered.getRoot());
        System.out.println("Layered root: " + layered.getRecalculations());
        assertTrue(plain.getRoot().equals(layered.getRoot()));
        plain.setRecalculations(0);
        layered.setRecalculations(0);

        plain.setFirstNode("-");
        System.out.println("Plain root:   " + plain.getRoot());
        System.out.println("Plain root:   " + plain.getRecalculations());
        layered.setFirstNode("-");
        System.out.println("Layered root: " + layered.getRoot());
        System.out.println("Layered root: " + layered.getRecalculations());
        assertTrue(plain.getRoot().equals(layered.getRoot()));
        plain.setRecalculations(0);
        layered.setRecalculations(0);

        plain.setNode(2, "#");
        plain.setNode(6, "&");
        plain.setNode(7, "*");
        plain.setNode(8, "@");
        plain.setNode(9, "n");
        plain.setNode(10, "x");
        System.out.println("Plain root:   " + plain.getRoot());
        System.out.println("Plain root:   " + plain.getRecalculations());
        layered.setNode(2, "#", false);
        layered.setNode(6, "&", false);
        layered.setNode(7, "*", false);
        layered.setNode(8, "@", false);
        layered.setNode(9, "n", false);
        layered.setNode(10, "x", false);
        layered.recalculate();
        System.out.println("Layered root: " + layered.getRoot());
        System.out.println("Layered root: " + layered.getRecalculations());
        assertTrue(plain.getRoot().equals(layered.getRoot()));
        plain.setRecalculations(0);
        layered.setRecalculations(0);
    }

}
