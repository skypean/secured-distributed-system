package OriginalNode;

import AES.CBCMode;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Split {

    private final String fileName;

    public Split(String fileName) {
        this.fileName = fileName;
    }

    public CBCMode[] splitFileByParts(final int nbPart, final int[] activePorts, final ArrayList<String> outputFileNames) {
        if (nbPart <= 0) {
            return null;
        }

        RandomAccessFile sourceFile = null;

        CBCMode[] splitParts = new CBCMode[nbPart];
        try {

            final long sourceSize = Files.size(Paths.get(fileName));
            final long minBytesPerSplit = sourceSize / nbPart;
            long remainingBytes = sourceSize;

            sourceFile = new RandomAccessFile(fileName, "r");

            long startSplitPos = 0;
            int nbPartSplitted = 0;
            while (nbPartSplitted < nbPart - 1) {
                long endSplitPos = startSplitPos + (remainingBytes < minBytesPerSplit ? remainingBytes : minBytesPerSplit);
                long realByteSplit = remainingBytes < minBytesPerSplit ? remainingBytes : minBytesPerSplit;
                boolean validDelim = isValidDelimiter(endSplitPos, sourceFile) || isValidDelimiter(endSplitPos - 1, sourceFile);
                while (!validDelim) {
                    endSplitPos++;
                    realByteSplit++;
                    validDelim = isValidDelimiter(endSplitPos, sourceFile) || isValidDelimiter(endSplitPos - 1, sourceFile);
                }
                remainingBytes -= realByteSplit;

                splitParts[nbPartSplitted] = new CBCMode(fileName, startSplitPos, realByteSplit, CBCMode.ENCRYPT, outputFileNames.get(nbPartSplitted));
                startSplitPos += realByteSplit;
                nbPartSplitted++;
            }
            splitParts[nbPartSplitted] = new CBCMode(fileName, startSplitPos, remainingBytes, CBCMode.ENCRYPT, outputFileNames.get(nbPartSplitted));
        } catch (IOException ex) {
            Logger.getLogger(Split.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            sourceFile.close();
        } catch (IOException ex) {
            Logger.getLogger(Split.class.getName()).log(Level.SEVERE, null, ex);
        }

        return splitParts;
    }

    private boolean isValidDelimiter(long position, RandomAccessFile sourceFile) {
        if (position == 0 || position == -1) {
            return true;
        }
        char delim;
        try {
            sourceFile.seek(position);
            delim = (char) sourceFile.read();
            if (delim == '\n' || delim == '\t' || delim == '\r' || delim == ' ') {
                return true;
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return false;
    }
}
