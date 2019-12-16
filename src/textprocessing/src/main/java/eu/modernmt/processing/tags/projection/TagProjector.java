package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.*;
import eu.modernmt.xml.XMLUtils;

import java.util.Arrays;


public class TagProjector {

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();

        if (source.hasTags()) {
            TagCollection sourceTags = new TagCollection(source.getTags());

            if (source.hasWords()) {
                if (translation.hasAlignment()) {
                    sourceTags.fixXmlCompliance();

                    Word[] sourceWords = source.getWords();
                    Word[] translationWords = translation.getWords();
                    TagCollection translationTags = new TagCollection();
                    SpanCollection sourceSpans = new SpanCollection(sourceTags.getTags(), sourceWords.length);

                    SpanTree sourceTree = new SpanTree(sourceSpans);
                    sourceTree.create();

                    Alignment alignment = new Alignment(translation.getWordAlignment(), sourceWords.length, translationWords.length);

                    SpanCollection translationSpans = new SpanCollection();
                    translationSpans.project(sourceSpans, alignment, translationWords.length);

                    SpanTree translationTree = new SpanTree(translationSpans);
                    translationTree.project(sourceTree, alignment, translationWords.length);

                    translationTags.populate(translationTree);

                    translation.setTags(translationTags.getTags());
                    simpleSpaceAnalysis(translation);
                }
            } else { //there are no source words; just copy the source tags in the target tags
                Tag[] copy = Arrays.copyOf(sourceTags.getTags(), sourceTags.size());
                translation.setTags(copy);
            }
        }

        return translation;
    }

    public static void simpleSpaceAnalysis(Sentence sentence) {

        String spaceAfterPreviousWord = null;
        Token previousToken = null;
        for (Token currentToken : sentence) {

            if (previousToken == null) {
                if (currentToken instanceof Tag) {
                    //Remove first whitespace of the tag in the first position, only if it is a Tag
                    currentToken.setLeftSpace(null);
                }
            } else {
                String space = Sentence.getSpaceBetweenTokens(previousToken, currentToken);
                previousToken.setRightSpace(space);
                currentToken.setLeftSpace(space);

                if (currentToken instanceof Tag) { // X-Tag
                    spaceAfterPreviousWord = Sentence.combineSpace(spaceAfterPreviousWord, currentToken);
                } else { // X-Word
                    if (previousToken instanceof Tag) {// Tag-Word
                        //This Word requires a space on the left,
                        //but no Token between the last Word and this Word has any space (ex. "previousWord<tag1><tag2>thisWord");
                        //hence force a space before this Word
                        if (spaceAfterPreviousWord == null && ((Word) currentToken).isLeftSpaceRequired() && !currentToken.isVirtualLeftSpace()) {
                            previousToken.setRightSpace(" ");
                            currentToken.setLeftSpace(" ");
                        }
                    }
                    spaceAfterPreviousWord = null;

                }

            }
            previousToken = currentToken;
        }
        //Remove the last whitespace whatever token is
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }

    }

}
