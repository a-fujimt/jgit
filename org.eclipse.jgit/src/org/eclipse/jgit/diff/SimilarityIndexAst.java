package org.eclipse.jgit.diff;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.IOException;

/**
 * Score similarity
 */
public class SimilarityIndexAst extends SimilarityIndex {

    private Tree tree;

    SimilarityIndexAst() {
        Run.initGenerators();
    }

    void generateAST(ObjectLoader obj, DiffEntry.Side side, DiffEntry entry) throws IOException, TableFullException {
        super.hash(obj);
        super.sort();

        String path = side == DiffEntry.Side.NEW ? entry.getNewPath() : entry.getOldPath();
        String contents = new String(obj.getCachedBytes());
        TreeGenerator p = TreeGenerators.getInstance().get(path);
        if (p != null) {
            try {
                tree = p.generateFrom().string(contents).getRoot();
            } catch (SyntaxException e) {
                System.err.println("Syntax Error file: " + path);
            }
        }
    }

    @Override
    public int score(SimilarityIndex dst, int maxScore) {
        if (dst instanceof SimilarityIndexAst) {
            if (tree != null && ((SimilarityIndexAst) dst).tree != null) {
                Tree dstTree = ((SimilarityIndexAst) dst).tree;
                Matcher defaultMatcher = Matchers.getInstance().getMatcher();
                MappingStore mappings = defaultMatcher.match(tree, dstTree);
                EditScriptGenerator editScriptGenerator = new ChawatheScriptGenerator();
                EditScript actions = editScriptGenerator.computeActions(mappings);
                return (int) ((1 - ((double)actions.size() / (double)(tree.getMetrics().size + dstTree.getMetrics().size))) * maxScore);
            }
        }
        return super.score(dst, maxScore);
    }
}
