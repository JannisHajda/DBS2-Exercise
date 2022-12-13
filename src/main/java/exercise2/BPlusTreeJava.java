package exercise2;

import com.sun.jdi.Value;
import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.exercise2.AbstractBPlusTree;
import de.hpi.dbs2.exercise2.BPlusTreeNode;
import de.hpi.dbs2.exercise2.LeafNode;
import de.hpi.dbs2.exercise2.InnerNode;
import de.hpi.dbs2.exercise2.ValueReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Stack;

/**
 * This is the B+-Tree implementation you will work on.
 * Your task is to implement the insert-operation.
 *
 */
@ChosenImplementation(true)
public class BPlusTreeJava extends AbstractBPlusTree {
    public BPlusTreeJava(int order) {
        super(order);
    }

    public BPlusTreeJava(BPlusTreeNode<?> rootNode) {
        super(rootNode);
    }

    private Stack tracePath(BPlusTreeNode<?> root, Integer key) {
        Stack<BPlusTreeNode<?>> stack = new Stack<>();

        BPlusTreeNode<?> node = root;

        // root is the only node in the tree
        if (node instanceof LeafNode) {
            stack.push(node);
            return stack;
        }

        while (!(node instanceof LeafNode)) {
            stack.push(node);
            Integer[] keys = node.keys;

            int i = 0;
            while (i < keys.length && keys[i] != null && key >= keys[i]) {
                i++;
            }

            node = ((InnerNode) node).references[i];
        }

        // push leaf node
        stack.push(node);

        return stack;
    }

    private void insertIntoNode(BPlusTreeNode node, Integer key, BPlusTreeNode reference) {
        Integer[] keys = node.keys;

        // find the index to insert the key
        int i = 0;
        while (i < keys.length && keys[i] != null && key >= keys[i]) {
            i++;
        }

        // shift keys to the right
        for (int j = keys.length - 1; j > i; j--) {
            keys[j] = keys[j - 1];
        }

        // insert key
        keys[i] = key;

        // shift all references after the inserted key to the right
        BPlusTreeNode[] references = ((InnerNode) node).references;
        for (int j = references.length - 1; j > i + 1; j--) {
            references[j] = references[j - 1];
        }

        // insert reference
        references[i + 1] = reference;
    }

    private void insertLoop(BPlusTreeNode root, Stack stack, Integer key, BPlusTreeNode reference) {
        InnerNode node = (InnerNode) stack.pop();

        if (node.isFull()) {
            InnerNode left = node;
            InnerNode right = new InnerNode(order);

            Integer[] allKeys = new Integer[node.keys.length + 1];
            BPlusTreeNode[] allReferences = new BPlusTreeNode[node.references.length + 1];

            for (int i = 0; i < left.keys.length; i++) {
                allKeys[i] = left.keys[i];
            }

            for (int i = 0; i < left.references.length; i++) {
                allReferences[i] = left.references[i];
            }


            // find insert position for new key
            int insertPosition = 0;
            while (insertPosition < allKeys.length && allKeys[insertPosition] != null
                    && key >= allKeys[insertPosition]) {
                insertPosition++;
            }

            // shift keys to the right
            for (int i = allKeys.length - 1; i > insertPosition; i--) {
                allKeys[i] = allKeys[i - 1];
            }

            // insert new key
            allKeys[insertPosition] = key;

            // shift references to the right
            for (int i = allReferences.length - 1; i > insertPosition + 1; i--) {
                allReferences[i] = allReferences[i - 1];
            }

            // insert new reference
            allReferences[insertPosition + 1] = reference;

            // determine split index
            int splitIndex = (int) Math.ceil(allKeys.length / 2.0);

            // clear left keys
            for (int i = 0; i < left.keys.length; i++) {
                left.keys[i] = null;
            }

            // clear left references
            for (int i = 0; i < left.references.length; i++) {
                left.references[i] = null;
            }

            // copy keys to left node
            for (int i = 0; i < splitIndex; i++) {
                left.keys[i] = allKeys[i];
            }

            // copy references to left node
            for (int i = 0; i < splitIndex + 1; i++) {
                left.references[i] = allReferences[i];
            }

            // copy keys to right node
            for (int i = splitIndex; i < allKeys.length; i++) {
                right.keys[i - splitIndex] = allKeys[i];
            }

            // copy references to right node
            for (int i = splitIndex; i < allReferences.length; i++) {
                right.references[i - splitIndex] = allReferences[i];
            }

            // find largest key and delete corresponding reference in left node
            int i = 0;
            while (i < left.keys.length && left.keys[i] != null) {
                i++;
            }

            int largestKey = left.keys[i - 1];
            left.keys[i - 1] = null;
            left.references[i] = null;

            if (root == left) {
                InnerNode newRoot = new InnerNode(order);
                newRoot.keys[0] = largestKey;
                newRoot.references[0] = left;
                newRoot.references[1] = right;

                this.rootNode = newRoot;
                return;
            } else {
                this.insertLoop(root, stack, largestKey, right);
            }
        } else {
            insertIntoNode(node, key, reference);
        }
    }

    private ValueReference updateLeaf(LeafNode leaf, Integer key, ValueReference value) {
        Integer[] keys = leaf.keys;
        ValueReference[] references = leaf.references;

        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == key) {
                ValueReference oldValue = references[i];
                references[i] = value;
                return oldValue;
            }
        }

        return null;
    }

    @Nullable
    @Override
    public ValueReference insert(@NotNull Integer key, @NotNull ValueReference value) {

        BPlusTreeNode<?> root = this.getRootNode();
        Stack nodePath = tracePath(root, key);

        LeafNode potentialLeaf = (LeafNode) nodePath.pop();
        Integer[] keys = potentialLeaf.keys;
        ValueReference[] values = potentialLeaf.references;

        if (potentialLeaf.getOrNull(key) != null) {
            // key already exists in tree -> update value and return old value
            return updateLeaf(potentialLeaf, key, value);
        } else {
            // key does not exist in tree -> insert new key-value pair

            if (potentialLeaf.isFull()) {
                // split leafNode
                LeafNode leftNode = new LeafNode(this.order);
                LeafNode rightNode = new LeafNode(this.order);

                Integer[] allKeys = new Integer[keys.length + 1];
                ValueReference[] allValues = new ValueReference[values.length + 1];

                for (int i = 0; i < keys.length; i++) {
                    allKeys[i] = keys[i];
                }

                for (int i = 0; i < values.length; i++) {
                    allValues[i] = values[i];
                }

                allKeys[allKeys.length - 1] = key;
                allValues[allValues.length - 1] = value;

                // sort keys and values
                for (int i = 0; i < allKeys.length; i++) {
                    for (int j = i + 1; j < allKeys.length; j++) {
                        if (allKeys[i] > allKeys[j]) {
                            Integer tempKey = allKeys[i];
                            ValueReference tempValue = allValues[i];

                            allKeys[i] = allKeys[j];
                            allValues[i] = allValues[j];

                            allKeys[j] = tempKey;
                            allValues[j] = tempValue;
                        }
                    }
                }

                // determine split index
                int splitIndex = (int) Math.ceil(allKeys.length / 2.0);

                // clear left node
                for (int i = 0; i < keys.length; i++) {
                    keys[i] = null;
                    values[i] = null;
                }

                // fill left node
                for (int i = 0; i < splitIndex; i++) {
                    leftNode.keys[i] = allKeys[i];
                    leftNode.references[i] = allValues[i];
                }

                // fill right node
                for (int i = splitIndex; i < allKeys.length; i++) {
                    rightNode.keys[i - splitIndex] = allKeys[i];
                    rightNode.references[i - splitIndex] = allValues[i];
                }

                // set next reference
                if (potentialLeaf.nextSibling != null) {
                    rightNode.nextSibling = potentialLeaf.nextSibling;
                }

                leftNode.nextSibling = rightNode;

                // insert smallest key of right node into parent node
                int smallestKey = rightNode.keys[0];

                if (potentialLeaf == root) {
                    // create new root node
                    InnerNode newRoot = new InnerNode(this.order);
                    newRoot.keys[0] = smallestKey;
                    newRoot.references[0] = leftNode;
                    newRoot.references[1] = rightNode;
                    this.rootNode = newRoot;
                    potentialLeaf = leftNode;
                } else {
                    for (int i = 0; i < leftNode.keys.length; i++) {
                        potentialLeaf.keys[i] = leftNode.keys[i];
                    }

                    for (int i = 0; i < leftNode.references.length; i++) {
                        potentialLeaf.references[i] = leftNode.references[i];
                    }

                    potentialLeaf.nextSibling = leftNode.nextSibling;

                    insertLoop(root, nodePath, smallestKey, rightNode);
                }
            } else {
                // potentialLeaf is not full -> insert new key-value pair
                for (int i = 0; i < keys.length; i++) {
                    if (keys[i] == null) {
                        keys[i] = key;
                        potentialLeaf.references[i] = value;
                        break;
                    }

                    int currentKey = keys[i];

                    if (currentKey > key) {
                        // shift keys and references to the right
                        for (int j = keys.length - 1; j > i; j--) {
                            keys[j] = keys[j - 1];
                            potentialLeaf.references[j] = potentialLeaf.references[j - 1];
                        }

                        keys[i] = key;
                        potentialLeaf.references[i] = value;
                        break;
                    }
                }
            }
        }

        return null;

        // Find LeafNode in which the key has to be inserted.
        // It is a good idea to track the "path" to the LeafNode in a Stack or something
        // alike.
        // Does the key already exist? Overwrite!
        // leafNode.references[pos] = value;
        // But remember return the old value!
        // New key - Is there still space?
        // leafNode.keys[pos] = key;
        // leafNode.references[pos] = value;
        // Don't forget to update the parent keys and so on...
        // Otherwise
        // Split the LeafNode in two!
        // Is parent node root?
        // update rootNode = ... // will have only one key
        // Was node instanceof LeafNode?
        // update parentNode.keys[?] = ...
        // Don't forget to update the parent keys and so on...

        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or
        // debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented
        // or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!
    }
}
