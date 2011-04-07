package net.winstone;

import net.winstone.boot.BootStrap;

/**
 *
 *  @author Jerome Guibert
 */
public class Winstone {

    /**
     * Main methods.
     * @param args
     * @throws IOException if something is wrong when reading properties files.
     */
    public static void main(final String[] args) {
        BootStrap bootStrap = new BootStrap(args);
        bootStrap.boot();
    }
}
