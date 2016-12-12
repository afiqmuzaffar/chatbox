package pl.grzeslowski.chatbox.rnn.trainer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

class DialogsDataSetIterator implements DataSetIterator {
    private final List<TrainerImpl.VecDialog> dialogs;
    private final int batchSize;
    private final int maxWordsInDialog;
    private final int layerSize;
    private Iterator<TrainerImpl.VecDialog> iterator;
    private int cursor;

    DialogsDataSetIterator(List<TrainerImpl.VecDialog> dialogs, int batchSize, int maxWordsInDialog, int layerSize) {
        this.dialogs = dialogs;
        iterator = dialogs.iterator();

        this.batchSize = batchSize;
        this.maxWordsInDialog = maxWordsInDialog;
        this.layerSize = layerSize;
    }

    @Override
    public DataSet next(int howMuchToTake) {
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }

        List<TrainerImpl.VecDialog> toProcess = new ArrayList<>(howMuchToTake);
        for (int i = 0; i < howMuchToTake && iterator.hasNext(); i++) {
            toProcess.add(iterator.next());
            cursor++;
        }

        int maxLength = 0;
        for (TrainerImpl.VecDialog dialog : toProcess) {
            maxLength = Math.max(maxLength, dialog.getQuestionSize());
            maxLength = Math.max(maxLength, dialog.getAnswerSize());
        }

        checkArgument(maxLength <= maxWordsInDialog, format("maxLength = %s > maxWordsInDialog = %s", maxLength, maxWordsInDialog));

        final INDArray features = Nd4j.zeros(toProcess.size(), layerSize, maxLength);
        final INDArray labels = Nd4j.zeros(toProcess.size(), layerSize, maxLength);

        INDArray featuresMask = Nd4j.zeros(toProcess.size(), maxLength);
        INDArray labelsMask = Nd4j.zeros(toProcess.size(), maxLength);

        for (int i = 0; i < toProcess.size(); i++) {
            final TrainerImpl.VecDialog dialog = toProcess.get(i);

            putIntoArray(features, featuresMask, i, dialog.getQuestion());
            putIntoArray(labels, labelsMask, i, dialog.getAnswer());
        }

        return new DataSet(features, labels, featuresMask, labelsMask);
    }

    private void putIntoArray(INDArray array, INDArray mask, int idx, List<INDArray> putFromHere) {
        int[] temp = new int[2];
        temp[0] = idx;

        for (int k = 0; k < putFromHere.size(); k++) {
            INDArray vector = putFromHere.get(k);
            array.put(new INDArrayIndex[]{NDArrayIndex.point(idx), NDArrayIndex.all(), NDArrayIndex.point(k)}, vector);

            temp[1] = k;
            mask.putScalar(temp, 1.0);
        }
    }

    @Override
    public int totalExamples() {
        return dialogs.size();
    }

    @Override
    public int inputColumns() {
        return maxWordsInDialog * layerSize;
    }

    @Override
    public int totalOutcomes() {
        return maxWordsInDialog * layerSize;
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {
        iterator = dialogs.iterator();
        cursor = 0;
    }

    @Override
    public int batch() {
        return batchSize;
    }

    @Override
    public int cursor() {
        return cursor;
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        throw new UnsupportedOperationException("getPreProcessor not suported");
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor preProcessor) {
        throw new UnsupportedOperationException("setPreProcessor not suported");

    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("getLabels not suported");
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public DataSet next() {
        return next(batch());
    }
}