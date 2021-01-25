package AES;

import Utility.Copy;

public class AESDecrypt {

    private final int Nb = 4; // words in block, always 4 for now
    private final int Nk; // key lengths in words
    private final int Nr; // number of rounds, = Nk + 6;
    private int wCount; // position in w for RoundKey (=0 for each encrypt)
    private final AESTables tab; // all the tables needed for AES;
    private final byte[] w; //the expanded key

    // AESencrypt: Constructor for class. Mainly expands key
    public AESDecrypt(byte key[], int NkIn) {
        Nk = NkIn;
        Nr = Nk + 6;
        tab = new AESTables();
        w = new byte[4 * Nb * (Nr + 1)];
        keyExpansion(key, w);
    }

    public void invCipher(byte[] in, byte[] out) {
        long startTIme = System.currentTimeMillis();

        wCount = 4 * Nb * (Nr + 1);
        byte[][] state = new byte[4][Nb];
        Copy.copy(state, in);
        invAddRoundKey(state);
        for (int round = Nr - 1; round >= 1; round--) {
            invShiftRows(state);
            invSubBytes(state);
            invAddRoundKey(state);
            invMixColumns(state);
        }
        invShiftRows(state);
        invSubBytes(state);
        invAddRoundKey(state);
        Copy.copy(out, state);
    }

    private void keyExpansion(byte[] key, byte[] w) {
        byte[] temp = new byte[4];
        // first just copy key to w;
        int j = 0;
        while (j < 4 * Nk) {
            w[j] = key[j++];
        }

        //here j == 4*Nk;
        int i;
        while (j < 4 * Nb * (Nr + 1)) {
            i = j / 4; //j is always multiple of 4 here

            //hanlde everything word-at-a time, 4 bytes at a time
            for (int iTemp = 0; iTemp < 4; iTemp++) {
                temp[iTemp] = w[j - 4 + iTemp];
            }
            if (i % Nk == 0) {
                byte ttemp, tRcon;
                byte oldTemp0 = temp[0];
                for (int iTemp = 0; iTemp < 4; iTemp++) {
                    if (iTemp == 3) {
                        ttemp = oldTemp0;
                    } else {
                        ttemp = temp[iTemp + 1];
                    }
                    if (iTemp == 0) {
                        tRcon = tab.Rcon(i / Nk);
                    } else {
                        tRcon = 0;
                    }
                    temp[iTemp] = (byte) (tab.SBox(ttemp) ^ tRcon);
                }
            } else if (Nk > 6 && (i % Nk) == 4) {
                for (int iTemp = 0; iTemp < 4; iTemp++) {
                    temp[iTemp] = tab.SBox(temp[iTemp]);
                }
            }
            for (int iTemp = 0; iTemp < 4; iTemp++) {
                w[j + iTemp] = (byte) (w[j - 4 * Nk + iTemp] ^ temp[iTemp]);
            }
            j = j + 4;
        }
    }

    private void invSubBytes(byte[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < Nb; col++) {
                state[row][col] = tab.invSBox(state[row][col]);
            }
        }
    }

    private void invShiftRows(byte[][] state) {
        byte[] t = new byte[4];
        for (int r = 1; r < 4; r++) {
            for (int c = 0; c < Nb; c++) {
                t[(c + r) % Nb] = state[r][c];
            }
            System.arraycopy(t, 0, state[r], 0, Nb);
        }
    }

    private void invMixColumns(byte[][] state) {
        int sp[] = new int[4];
        byte b0b = (byte) 0x0b, b0d = (byte) 0x0d, b09 = (byte) 0x09, b0e = (byte) 0x0e;

        for (int c = 0; c < 4; c++) {
            sp[0] = tab.FFMul(b0e, state[0][c]) ^ tab.FFMul(b0b, state[1][c]) ^ tab.FFMul(b0d, state[2][c]) ^ tab.FFMul(b09, state[3][c]);
            sp[1] = tab.FFMul(b09, state[0][c]) ^ tab.FFMul(b0e, state[1][c]) ^ tab.FFMul(b0b, state[2][c]) ^ tab.FFMul(b0d, state[3][c]);
            sp[2] = tab.FFMul(b0d, state[0][c]) ^ tab.FFMul(b09, state[1][c]) ^ tab.FFMul(b0e, state[2][c]) ^ tab.FFMul(b0b, state[3][c]);
            sp[3] = tab.FFMul(b0b, state[0][c]) ^ tab.FFMul(b0d, state[1][c]) ^ tab.FFMul(b09, state[2][c]) ^ tab.FFMul(b0e, state[3][c]);
            for (int i = 0; i < 4; i++) {
                state[i][c] = (byte) sp[i];
            }
        }
    }

    private void invAddRoundKey(byte[][] state) {
        for (int c = Nb - 1; c >= 0; c--) {
            for (int r = 3; r >= 0; r--) {
                state[r][c] = (byte) (state[r][c] ^ w[--wCount]);
            }
        }
    }

}
