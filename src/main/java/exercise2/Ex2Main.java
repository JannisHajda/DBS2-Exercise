package exercise2;

import de.hpi.dbs2.exercise2.*;

public class Ex2Main {
    public static void main(String[] args) {
        int order = 4;

        BPlusTreeNode<?> root = BPlusTreeNode.buildTree(order,
                new AbstractBPlusTree.Entry[][] {
                        new AbstractBPlusTree.Entry[] {
                                new AbstractBPlusTree.Entry(2, new ValueReference(0)),
                                new AbstractBPlusTree.Entry(3, new ValueReference(1)),
                                new AbstractBPlusTree.Entry(5, new ValueReference(2))
                        },
                        new AbstractBPlusTree.Entry[] {
                                new AbstractBPlusTree.Entry(7, new ValueReference(3)),
                                new AbstractBPlusTree.Entry(11, new ValueReference(4))
                        }
                },
                new AbstractBPlusTree.Entry[][] {
                        new AbstractBPlusTree.Entry[] {
                                new AbstractBPlusTree.Entry(13, new ValueReference(5)),
                                new AbstractBPlusTree.Entry(17, new ValueReference(6)),
                                new AbstractBPlusTree.Entry(19, new ValueReference(7))
                        },
                        new AbstractBPlusTree.Entry[] {
                                new AbstractBPlusTree.Entry(23, new ValueReference(8)),
                                new AbstractBPlusTree.Entry(29, new ValueReference(9))
                        },
                        new AbstractBPlusTree.Entry[] {
                                new AbstractBPlusTree.Entry(31, new ValueReference(10)),
                                new AbstractBPlusTree.Entry(37, new ValueReference(11)),
                                new AbstractBPlusTree.Entry(41, new ValueReference(12))
                        },
                        new AbstractBPlusTree.Entry[] {
                                new AbstractBPlusTree.Entry(43, new ValueReference(13)),
                                new AbstractBPlusTree.Entry(47, new ValueReference(14))
                        }
                });
        System.out.println(root);

        AbstractBPlusTree tree = new BPlusTreeJava(root);
        System.out.println(tree);

        LeafNode leafNode = new LeafNode(4);
        System.out.println(leafNode);
        InnerNode innerNode = new InnerNode(4);
        System.out.println(innerNode);

        /*
         * playground
         * ~ feel free to experiment with the tree and tree nodes here
         */

        AbstractBPlusTree tree2 = new BPlusTreeJava(4);
        tree2.insert(2, new ValueReference(2));

        tree2.insert(7, new ValueReference(7));

        tree2.insert(4, new ValueReference(4));

        tree2.insert(3, new ValueReference(3));

        tree2.insert(8, new ValueReference(8));

        tree2.insert(1, new ValueReference(1));

        tree2.insert(9, new ValueReference(9));
        tree2.insert(10, new ValueReference(10));
        tree2.insert(11, new ValueReference(11));
        tree2.insert(12, new ValueReference(12));
        tree2.insert(13, new ValueReference(13));
        tree2.insert(14, new ValueReference(14));
        tree2.insert(15, new ValueReference(15));
        tree2.insert(16, new ValueReference(16));
        tree2.insert(17, new ValueReference(17));
        tree2.insert(18, new ValueReference(18));
        tree2.insert(19, new ValueReference(19));
        tree2.insert(0, new ValueReference(0));
        tree2.insert(5, new ValueReference(5));
        tree2.insert(6, new ValueReference(6));
        System.out.println(tree2);
    }
}
