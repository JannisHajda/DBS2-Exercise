package exercise2;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.exercise2.AbstractBPlusTree;
import de.hpi.dbs2.exercise2.BPlusTreeNode;
import de.hpi.dbs2.exercise2.LeafNode;
import de.hpi.dbs2.exercise2.ValueReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private void insertIntoLeaf(BPlusTreeNode root, Integer key, ValueReference value) {
        LeafNode leaf = root.findLeaf(key);

        // insert into leaf
        Integer[] keys = leaf.keys;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == null) {
                leaf.keys[i] = key;
                leaf.references[i] = value;
                break;
            }

            int curKey = keys[i];

            if (curKey > key) {
                // insert key at i and shift all keys to the right
                for (int j = keys.length - 1; j > i; j--) {
                    leaf.keys[j] = keys[j - 1];
                    leaf.references[j] = leaf.references[j - 1];
                }

                leaf.keys[i] = key;
                leaf.references[i] = value;
                break;
            }
        }
    }

    @Nullable
    @Override
    public ValueReference insert(@NotNull Integer key, @NotNull ValueReference value) {
        BPlusTreeNode root = this.getRootNode();
        LeafNode potentialLeaf = root.findLeaf(key);

        if (potentialLeaf.getOrNull(key) == null) {
            // key doesn't exist
            if (potentialLeaf.isFull()) {
                // split leaf
                Integer[] keys = potentialLeaf.keys;
                ValueReference[] values = potentialLeaf.references;

                // create new leaf
                LeafNode newLeaf = new LeafNode(this.order);

                // copy half of the keys and values to the new leaf
                int half = keys.length / 2;

                for (int i = half; i < keys.length; i++) {
                    newLeaf.keys[i - half] = keys[i];
                    newLeaf.references[i - half] = values[i];
                    potentialLeaf.keys[i] = null;
                    potentialLeaf.references[i] = null;
                }

                potentialLeaf.nextSibling = newLeaf;
                this.insertIntoLeaf(root, key, value);
            } else {
                this.insertIntoLeaf(root, key, value);
            }
        } else {
            Integer[] keys = potentialLeaf.keys;
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] == key) {
                    ValueReference oldValue = potentialLeaf.references[i];
                    potentialLeaf.references[i] = value;
                    return oldValue;
                }
            }
        }

        return null;


        // Find LeafNode in which the key has to be inserted.
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.
        // Does the key already exist? Overwrite!
        //   leafNode.references[pos] = value;
        //   But remember return the old value!
        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...
        // Otherwise
        //   Split the LeafNode in two!
        //   Is parent node root?
        //     update rootNode = ... // will have only one key
        //   Was node instanceof LeafNode?
        //     update parentNode.keys[?] = ...
        //   Don't forget to update the parent keys and so on...

        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!
    }
}
