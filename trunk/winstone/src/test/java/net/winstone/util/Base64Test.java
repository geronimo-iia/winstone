package net.winstone.util;

import junit.framework.TestCase;
import net.winstone.util.Base64;

public class Base64Test extends TestCase {
    
    public Base64Test(String name) {
        super(name);
    }
    
    // The letters a-y encoded in base 64
    private static String ENCODED_PLUS_ONE = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eQ==";
    private static String ENCODED_PLUS_TWO = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=";
    
    public void testDecode() throws Exception {
        String decoded = Base64.decode(ENCODED_PLUS_TWO);
        String expected = "abcdefghijklmnopqrstuvwxyz";
        assertEquals("Straight decode failed", expected, decoded);
        
        decoded = Base64.decode(ENCODED_PLUS_ONE);
        expected = "abcdefghijklmnopqrstuvwxy";
        assertEquals("Decode failed", expected, decoded);
    }
    
    public static void testVersusPostgres() throws Exception {
        String decoded = Base64.decode("MTIzNDU2Nzg5MA==");
        assertEquals("Straight encode failed", "1234567890", decoded);
    }
    
    public static String hexEncode(byte input[]) {
        
        StringBuffer out = new StringBuffer();
        
        for (int i = 0; i < input.length; i++)
            out.append(Integer.toString((input[i] & 0xf0) >> 4, 16)).append(Integer.toString(input[i] & 0x0f, 16));
        
        return out.toString();
    }
    
    public static byte[] hexDecode(String input) {
        
        if (input == null) {
            return null;
        } else if (input.length() % 2 != 0) {
            throw new RuntimeException("Invalid hex for decoding: " + input);
        } else {
            byte output[] = new byte[input.length() / 2];
            
            for (int i = 0; i < output.length; i++) {
                int twoByte = Integer.parseInt(input.substring(i * 2, i * 2 + 2), 16);
                output[i] = (byte)(twoByte & 0xff);
            }
            return output;
        }
    }
}
