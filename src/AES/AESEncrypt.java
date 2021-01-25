package AES;

import Utility.Copy;

public class AESEncrypt {

    private final int Nb = 4; // words in block, always 4 for now
    private final int Nk; // key lengths in words
    private final int Nr; // number of rounds, = Nk + 6;
    private int wCount; // position in w for RoundKey (=0 for each encrypt)
    private final AESTables tab; // all the tables needed for AES;
    private final byte[] w; //the expanded key

    // AESencrypt: Constructor for class. Mainly expands key
    public AESEncrypt(byte key[], int NkIn) {
        Nk = NkIn;
        Nr = Nk + 6;
        tab = new AESTables();
        w = new byte[4 * Nb * (Nr + 1)];
        keyExpansion(key, w);
    }

    // Cipher: actual AES encryption
    public void cipher(byte[] in, byte[] out) {

        wCount = 0; //count bytes in expanded key throughout encryption
        byte[][] state = new byte[4][Nb];
        Copy.copy(state, in); // actual component-wise copy
        addRoundKey(state); //xor with expanded key
        for (int round = 1; round < Nr; round++) {
            subBytes(state); //S-box substitution
            shiftRows(state); // mix-up rows
            mixColumns(state);
            addRoundKey(state); //xor with expanded key
            Copy.copy(out, state);
        }

        subBytes(state); //S-box substitution
        shiftRows(state); // mix-up rows
        addRoundKey(state); //xor with expanded key
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

    //subBytes: apply Sbox substitution to each byte of state
    private void subBytes(byte[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < Nb; col++) {
                state[row][col] = tab.SBox(state[row][col]);
            }
        }
    }

    //shiftRows: simple circular shift of rows 1, 2, 3 by 1, 2, 3
    private void shiftRows(byte[][] state) {
        byte[] t = new byte[4];
        for (int r = 1; r < 4; r++) {
            for (int c = 0; c < Nb; c++) {
                t[c] = state[r][(c + r) % Nb];
            }
            System.arraycopy(t, 0, state[r], 0, Nb);
        }
    }

    //mixColumns: complex and sophisticated mixing of columns
    private void mixColumns(byte[][] state) {
        int[] sp = new int[4];
        byte b02 = (byte) 0x02, b03 = (byte) 0x03;
        for (int c = 0; c < 4; c++) {
            sp[0] = tab.FFMul(b02, state[0][c]) ^ tab.FFMul(b03, state[1][c]) ^ state[2][c] ^ state[3][c];
            sp[1] = state[0][c] ^ tab.FFMul(b02, state[1][c]) ^ tab.FFMul(b03, state[2][c]) ^ state[3][c];
            sp[2] = state[0][c] ^ state[1][c] ^ tab.FFMul(b02, state[2][c]) ^ tab.FFMul(b03, state[3][c]);
            sp[3] = tab.FFMul(b03, state[0][c]) ^ state[1][c] ^ state[2][c] ^ tab.FFMul(b02, state[3][c]);
            for (int i = 0; i < 4; i++) {
                state[i][c] = (byte) (sp[i]);
            }
        }
    }

    private void addRoundKey(byte[][] state) {
        for (int c = 0; c < Nb; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] = (byte) (state[r][c] ^ w[wCount++]);
            }
        }
    }
}
