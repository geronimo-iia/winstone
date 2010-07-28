package net.winstone.accesslog;

/**
 * PatternType enumerate kind of pattern supported : COMMON, COMBINED, RESIN.
 * @author Jerome Guibert
 */
public enum PatternType {

    COMMON("###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size###"), COMBINED("###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size### \"###referer###\" \"###userAgent###\""), RESIN(
    "###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size### \"###userAgent###\"");
    private String pattern;

    private PatternType(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
