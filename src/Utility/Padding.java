package Utility;

public class Padding {

    public static byte[] padding(byte[] in) {
        if(in.length >= 16)
            return in;
        byte[] out = new byte[16];
        System.arraycopy(in, 0, out, 0, in.length);
        for (int i = in.length; i < out.length; i++) {
            out[i] = (byte) ' ';
        }
        return out;
    }

    public static String rightPad(String str, int length, char car) {
        return (String.format("%" + (-length) + "s", str).replace(' ', car));
    }
}
