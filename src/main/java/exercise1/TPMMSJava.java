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

@ChosenImplementation(true)
public class TPMMSJava extends SortOperation {
    public TPMMSJava(@NotNull BlockManager manager, int sortColumnIndex) {
        super(manager, sortColumnIndex);
    }

    @Override
    public int estimatedIOCost(@NotNull Relation relation) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void sort(@NotNull Relation relation, @NotNull BlockOutput output) {
        BlockManager manager = this.getBlockManager();
        int memSize = manager.getFreeBlocks();
        LinkedList<Block> memBlocks = new LinkedList<>();
        Comparator<Tuple> comparator = relation.getColumns().getColumnComparator(this.getSortColumnIndex());

        int dataSize = relation.getEstimatedSize();

        if (dataSize >= Math.pow(memSize, 2) - 1) {
            throw new RelationSizeExceedsCapacityException();
        }

        phase1(relation, manager, memSize, memBlocks, comparator);

        int numberOfLists = (int) Math.ceil(dataSize / (double) memSize);
        Block outputBlock = manager.allocate(true);
        LinkedList<Integer> diskBlockIndices = new LinkedList<>();
        LinkedList<Iterator<Block>> diskBlockIterators = new LinkedList<>();
        LinkedList<Iterator<Tuple>> tupleIterators = new LinkedList<>();
        PairComparator pairComparator = new PairComparator(relation, this.getSortColumnIndex());
        PriorityQueue<Pair<Integer, Tuple>> queue = new PriorityQueue<>(pairComparator);

        for (int i = 0; i < numberOfLists; i++) {
            Iterator<Block> diskBlockIterator = relation.iterator();
            int offset = i * memSize;

            for (int j = 0; j < offset; j++) {
                diskBlockIterator.next();
            }

            Block diskBlock = diskBlockIterator.next();
            Block memBlock = manager.load(diskBlock);
            Iterator<Tuple> tupleIterator = memBlock.iterator();

            queue.add(new Pair<>(i, tupleIterator.next()));
            diskBlockIterators.add(diskBlockIterator);
            diskBlockIndices.add(offset);
            memBlocks.add(memBlock);
            tupleIterators.add(tupleIterator);
        }

        while (true) {
            Pair<Integer, Tuple> smallestPair = queue.poll();
            Integer tupleIndex = smallestPair.getFirst();
            Tuple smallestTuple = smallestPair.getSecond();

            if (outputBlock.isFull()) {
                output.output(outputBlock);
                manager.release(outputBlock, false);
                outputBlock = manager.allocate(true);
            }

            outputBlock.append(smallestTuple);

            Iterator<Tuple> tupleIterator = tupleIterators.get(tupleIndex);

            if (!tupleIterator.hasNext()) {
                Block memBlock = memBlocks.get(tupleIndex);
                manager.release(memBlock, false);

                Iterator<Block> diskBlockIterator = diskBlockIterators.get(tupleIndex);
                if (diskBlockIterator.hasNext() && (diskBlockIndices.get(tupleIndex) + 1) % memSize != 0) {
                    diskBlockIndices.set(tupleIndex, diskBlockIndices.get(tupleIndex) + 1);
                    Block diskBlock = diskBlockIterator.next();
                    memBlock = manager.load(diskBlock);
                    tupleIterator = memBlock.iterator();
                    tupleIterators.set(tupleIndex, tupleIterator);
                    memBlocks.set(tupleIndex, memBlock);
                    queue.add(new Pair<>(tupleIndex, tupleIterator.next()));
                } else {
                    if (queue.isEmpty()) {
                        output.output(outputBlock);
                        break;
                    }
                    diskBlockIterators.set(tupleIndex, null);
                    tupleIterator = null;
                    tupleIterators.set(tupleIndex, null);
                }
            } else {
                queue.add(new Pair<>(tupleIndex, tupleIterator.next()));
            }
        }

        manager.release(outputBlock, false);
        diskBlockIterators.clear();
        diskBlockIndices.clear();
        tupleIterators.clear();
        outputBlock = null;
        System.out.println("Number of disk blocks: " + relation.getEstimatedSize());
    }

    private Boolean phase1(Relation relation, BlockManager manager, int memSize, LinkedList<Block> memBlocks, Comparator<Tuple> comparator) {
        Iterator<Block> diskBlocks = relation.iterator();

        while (diskBlocks.hasNext()) {
            for (int i = 0; i < memSize; i++) {
                if (!diskBlocks.hasNext()) {
                    break;
                }

                Block diskBlock = diskBlocks.next();
                Block memBlock = manager.load(diskBlock);
                memBlocks.add(memBlock);
            }

            BlockSorter.INSTANCE.sort(relation, memBlocks, comparator);

            for (Block memBlock : memBlocks) {
                manager.release(memBlock, true);
            }

            memBlocks.clear();
        }

        return true;
    }
}

class PairComparator implements Comparator<Pair<Integer, Tuple>> {
    private Comparator<Tuple> comparator;

    public PairComparator(Relation relation, Integer sortColumnIndex) {
        this.comparator = relation.getColumns().getColumnComparator(sortColumnIndex);
    }

    public int compare(Pair<Integer, Tuple> p1, Pair<Integer, Tuple> p2) {
        return this.comparator.compare(p1.getSecond(), p2.getSecond());
    }
}