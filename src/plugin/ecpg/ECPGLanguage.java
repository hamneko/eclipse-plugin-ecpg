package plugin.ecpg;

import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.model.ICLanguageKeywords;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;

import plugin.ecpg.parser.scanner.ECPGScanner;

public class ECPGLanguage extends GCCLanguage {

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
		return new ECPGScanner(content, scanInfo, getParserLanguage(), log, getScannerExtensionConfiguration(scanInfo),
				fcp);
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

}
