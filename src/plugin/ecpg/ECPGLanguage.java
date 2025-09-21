package plugin.ecpg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.parser.c.GCCParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICLanguageKeywords;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ParserMode;
import org.eclipse.core.runtime.CoreException;

import plugin.common.parser.scanner.ExecSqlPosition;
import plugin.ecpg.parser.scanner.ECPGScanner;

@SuppressWarnings("restriction")
public class ECPGLanguage extends GCCLanguage {

	private List<ExecSqlPosition> execSqlPositions;

	public ECPGLanguage() {
	}

	@Override
	public String getId() {
		return "plugin.ecpg.editor";
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.isAssignableFrom(ICLanguageKeywords.class)) {
			return (T) this;
		}
		return super.getAdapter(adapter);
	}

	@Override
	protected IScanner createScanner(FileContent content, IScannerInfo scanInfo, IncludeFileContentProvider fcp,
			IParserLogService log) {
		execSqlPositions = new ArrayList<ExecSqlPosition>();
		return new ECPGScanner(content, scanInfo, getParserLanguage(), log, getScannerExtensionConfiguration(scanInfo),
				fcp, execSqlPositions);
	}

	@Override
	public String[] getKeywords() {
		String[] keywords = super.getKeywords();
		String[] additionalKeywords = new String[] { "EXEC", "SQL" };
		String[] result = new String[keywords.length + additionalKeywords.length];
		System.arraycopy(keywords, 0, result, 0, keywords.length);
		System.arraycopy(additionalKeywords, 0, result, keywords.length, additionalKeywords.length);
		return result;
	}

	@Override
	public IASTTranslationUnit getASTTranslationUnit(FileContent reader, IScannerInfo scanInfo,
			IncludeFileContentProvider fileCreator, IIndex index, int options, IParserLogService log)
			throws CoreException {
		return getASTTranslationUnit(reader, scanInfo, fileCreator, index, options, log, false);
	}

	public IASTTranslationUnit getASTTranslationUnit(FileContent reader, IScannerInfo scanInfo,
			IncludeFileContentProvider fileCreator, IIndex index, int options, IParserLogService log,
			boolean calledFromFormatter) throws CoreException {

		IScanner scanner = createScanner(reader, scanInfo, fileCreator, log);
		scanner.setComputeImageLocations((options & OPTION_NO_IMAGE_LOCATIONS) == 0);
		scanner.setProcessInactiveCode((options & OPTION_PARSE_INACTIVE_CODE) != 0);

		ParserMode mode;
		if ((options & OPTION_SKIP_FUNCTION_BODIES) != 0) {
			mode = ParserMode.STRUCTURAL_PARSE;
		} else {
			mode = ParserMode.COMPLETE_PARSE;
		}
		ICParserExtensionConfiguration conf = GCCParserExtensionConfiguration.getInstance();

		if (calledFromFormatter) {
			return new ECPGSourceParser(scanner, mode, log, conf, index, execSqlPositions).parse();
		}
		return new ECPGSourceParser(scanner, mode, log, conf, index).parse();
	}

}
