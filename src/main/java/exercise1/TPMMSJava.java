package exercise1;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.dbms.*;
import de.hpi.dbs2.dbms.utils.BlockSorter;
import de.hpi.dbs2.exercise1.SortOperation;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

// compares two pairs of (blockIndex, tuple) based on the tuple's sort column
class PairComparator implements Comparator<Pair<Integer, Tuple>> {
    private Comparator<Tuple> comparator;

    public PairComparator(Relation relation, Integer sortColumnIndex) {
        this.comparator = relation.getColumns().getColumnComparator(sortColumnIndex);
    }

    public int compare(Pair<Integer, Tuple> p1, Pair<Integer, Tuple> p2) {
        // compare the tuples
        return this.comparator.compare(p1.getSecond(), p2.getSecond());
    }
}

@ChosenImplementation(true)
public class TPMMSJava extends SortOperation {
    public TPMMSJava(@NotNull BlockManager manager, int sortColumnIndex) {
        super(manager, sortColumnIndex);
    }

    @Override
    public int estimatedIOCost(@NotNull Relation relation) {
        // phase 1: read all blocks from disk, write all blocks in lists to disk
        // phase 2: read all blocks from lists, write all blocks to output
        // => IO = 4 * (number of blocks)
        return relation.getEstimatedSize() * 4;
    }

    @Override
    public void sort(@NotNull Relation relation, @NotNull BlockOutput output) {
        BlockManager manager = this.getBlockManager();
        int memSize = manager.getFreeBlocks();
        LinkedList<Block> memBlocks = new LinkedList<>();

        int dataSize = relation.getEstimatedSize();

        if (dataSize >= Math.pow(memSize, 2) - 1) {
            throw new RelationSizeExceedsCapacityException();
        }

        phase1(relation, manager, memSize, memBlocks);
        phase2(output, relation, dataSize, manager, memSize, memBlocks);
    }

    private void phase1(Relation relation, BlockManager manager, int memSize, LinkedList<Block> memBlocks) {
        Comparator<Tuple> comparator = relation.getColumns().getColumnComparator(this.getSortColumnIndex());
        Iterator<Block> diskBlocks = relation.iterator();

        while (diskBlocks.hasNext()) {
            // load as many blocks as possible into memory
            for (int i = 0; i < memSize; i++) {
                // if there are no more blocks on disk, we are done
                if (!diskBlocks.hasNext()) {
                    break;
                }

                // load the next block from disk into memory
                Block diskBlock = diskBlocks.next();
                Block memBlock = manager.load(diskBlock);
                memBlocks.add(memBlock);
            }

            // sort the blocks in memory
            BlockSorter.INSTANCE.sort(relation, memBlocks, comparator);

            // write sorted blocks back to disk
            for (Block memBlock : memBlocks) {
                manager.release(memBlock, true);
            }

            memBlocks.clear();
        }
    }

    private void phase2(BlockOutput output, Relation relation, int dataSize, BlockManager manager, int memSize, LinkedList<Block> memBlocks) {
        int numberOfLists = (int) Math.ceil(dataSize / (double) memSize);
        Block outputBlock = manager.allocate(true);
        LinkedList<Integer> diskBlockIndices = new LinkedList<>();
        LinkedList<Iterator<Block>> diskBlockIterators = new LinkedList<>();
        LinkedList<Iterator<Tuple>> tupleIterators = new LinkedList<>();
        PairComparator pairComparator = new PairComparator(relation, this.getSortColumnIndex());

        // Integer: index of the loaded block, Tuple: the next tuple from that block
        PriorityQueue<Pair<Integer, Tuple>> queue = new PriorityQueue<>(pairComparator);

        for (int i = 0; i < numberOfLists; i++) {
            Iterator<Block> diskBlockIterator = relation.iterator();
            int offset = i * memSize;

            // skip to the first block of the list
            for (int j = 0; j < offset; j++) {
                diskBlockIterator.next();
            }

            // load the first block of the list into memory
            Block diskBlock = diskBlockIterator.next();
            Block memBlock = manager.load(diskBlock);
            Iterator<Tuple> tupleIterator = memBlock.iterator();

            // add the first tuple of the block to the queue
            queue.add(new Pair<>(i, tupleIterator.next()));
            diskBlockIterators.add(diskBlockIterator);
            diskBlockIndices.add(offset);
            memBlocks.add(memBlock);
            tupleIterators.add(tupleIterator);
        }

        while (true) {
            // get the next tuple from the queue
            Pair<Integer, Tuple> smallestPair = queue.poll();
            Integer tupleIndex = smallestPair.getFirst();
            Tuple smallestTuple = smallestPair.getSecond();

            // write output block to disk if it is full
            if (outputBlock.isFull()) {
                output.output(outputBlock);
                manager.release(outputBlock, false);
                outputBlock = manager.allocate(true);
            }

            // write the tuple to the output block
            outputBlock.append(smallestTuple);

            Iterator<Tuple> tupleIterator = tupleIterators.get(tupleIndex);

            if (!tupleIterator.hasNext()) {
                // if there are no more tuples in the current block, load the next block from disk
                Block memBlock = memBlocks.get(tupleIndex);
                manager.release(memBlock, false);

                Iterator<Block> diskBlockIterator = diskBlockIterators.get(tupleIndex);

                // if there are more blocks in the list, load the next block from disk
                if (diskBlockIterator.hasNext() && (diskBlockIndices.get(tupleIndex) + 1) % memSize != 0) {
                    diskBlockIndices.set(tupleIndex, diskBlockIndices.get(tupleIndex) + 1);
                    Block diskBlock = diskBlockIterator.next();

                    memBlock = manager.load(diskBlock);
                    tupleIterator = memBlock.iterator();
                    tupleIterators.set(tupleIndex, tupleIterator);
                    memBlocks.set(tupleIndex, memBlock);
                    queue.add(new Pair<>(tupleIndex, tupleIterator.next()));
                } else {
                    // if there are no more blocks in the queue, we are done
                    if (queue.isEmpty()) {
                        output.output(outputBlock);
                        break;
                    }

                    // if there are no more blocks in the list, remove the list from the queue
                    diskBlockIterators.set(tupleIndex, null);
                    tupleIterator = null;
                    tupleIterators.set(tupleIndex, null);
                }
            } else {
                // add the next tuple of the block to the queue
                queue.add(new Pair<>(tupleIndex, tupleIterator.next()));
            }
        }

        // clean up
        manager.release(outputBlock, false);
        memBlocks.clear();
        diskBlockIterators.clear();
        diskBlockIndices.clear();
        tupleIterators.clear();
        outputBlock = null;
    }
}