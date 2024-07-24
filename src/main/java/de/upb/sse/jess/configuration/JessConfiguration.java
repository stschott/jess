package de.upb.sse.jess.configuration;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JessConfiguration {
    private boolean exitOnCompilationFail = false;
    private boolean exitOnParsingFail = false;
    private boolean looseSignatureMatching = true;
    private boolean keepAsteriskImports = true;
    private boolean failOnAmbiguity = true;
    private boolean disableStubbing = false;

    private String targetVersion = null;

    public JessConfiguration(boolean exitOnCompilationFail, boolean exitOnParsingFail, boolean looseSignatureMatching, boolean keepAsteriskImports, boolean failOnAmbiguity, boolean disableStubbing) {
        this.exitOnCompilationFail = exitOnCompilationFail;
        this.exitOnParsingFail = exitOnParsingFail;
        this.looseSignatureMatching = looseSignatureMatching;
        this.keepAsteriskImports = keepAsteriskImports;
        this.failOnAmbiguity = failOnAmbiguity;
        this.disableStubbing = disableStubbing;
    }
}
