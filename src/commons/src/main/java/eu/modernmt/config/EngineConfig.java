package eu.modernmt.config;

import eu.modernmt.lang.LanguageIndex2;

/**
 * Created by davide on 04/01/17.
 */
public class EngineConfig {

    private String name = "default";
    private LanguageIndex2 languageIndex = null;
    private DecoderConfig decoderConfig = new DecoderConfig();
    private AlignerConfig alignerConfig = new AlignerConfig();
    private AnalyzerConfig analyzerConfig = new AnalyzerConfig();

    public String getName() {
        return name;
    }

    public EngineConfig setName(String name) {
        this.name = name;
        return this;
    }

    public LanguageIndex2 getLanguageIndex() {
        return languageIndex;
    }

    public void setLanguageIndex(LanguageIndex2 languageIndex) {
        this.languageIndex = languageIndex;
    }

    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    public AlignerConfig getAlignerConfig() {
        return alignerConfig;
    }

    public AnalyzerConfig getAnalyzerConfig() {
        return analyzerConfig;
    }

    @Override
    public String toString() {
        return "[Engine]\n" +
                "  name = " + name + "\n" +
                "  languages = " + languageIndex + "\n" +
                "  " + decoderConfig.toString().replace("\n", "\n  ") +
                "  " + alignerConfig.toString().replace("\n", "\n  ") +
                "  " + analyzerConfig.toString().replace("\n", "\n  ");
    }
}
